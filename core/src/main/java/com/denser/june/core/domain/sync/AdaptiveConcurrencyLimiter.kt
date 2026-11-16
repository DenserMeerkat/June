package com.denser.june.core.domain.sync

import com.denser.june.core.domain.logging.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AdaptiveConcurrencyLimiter(
    val minLimit: Int = 3,
    val maxLimit: Int = 10,
    initialLimit: Int = maxLimit,
    val successThreshold: Int = 8
) {
    private val mutex = Mutex()

    var currentLimit: Int = initialLimit.coerceIn(minLimit, maxLimit)
        private set

    var inFlight: Int = 0
        private set

    private var consecutiveSuccesses: Int = 0
    private val waiters = ArrayDeque<CompletableDeferred<Unit>>()

    suspend fun acquire() {
        val waiter: CompletableDeferred<Unit>? = mutex.withLock {
            if (inFlight < currentLimit) {
                inFlight++
                null
            } else {
                val deferred = CompletableDeferred<Unit>()
                waiters.addLast(deferred)
                deferred
            }
        }

        if (waiter != null) {
            try {
                waiter.await()
            } catch (e: CancellationException) {
                val toResume = mutex.withLock {
                    if (!waiters.remove(waiter)) {
                        inFlight--
                        wakeUpWaitersLocked()
                    } else {
                        emptyList()
                    }
                }
                toResume.forEach { it.complete(Unit) }
                throw e
            }
        }
    }

    suspend fun release(isSuccess: Boolean, isThrottle: Boolean = false) {
        val toResume = mutex.withLock {
            if (isThrottle) {
                consecutiveSuccesses = 0
                val oldLimit = currentLimit
                currentLimit = maxOf(minLimit, currentLimit / 2)
                AppLogger.w(
                    AppLogger.Category.SYNC,
                    "AdaptiveLimiter",
                    "Throttled! Concurrency backed off from $oldLimit to $currentLimit"
                )
            } else if (isSuccess) {
                consecutiveSuccesses++
                if (consecutiveSuccesses >= successThreshold && currentLimit < maxLimit) {
                    consecutiveSuccesses = 0
                    currentLimit++
                    AppLogger.d(
                        AppLogger.Category.SYNC,
                        "AdaptiveLimiter",
                        "Ramped up concurrency to $currentLimit (after $successThreshold successes)"
                    )
                }
            } else {
                consecutiveSuccesses = 0
            }

            inFlight--
            wakeUpWaitersLocked()
        }

        toResume.forEach { it.complete(Unit) }
    }

    private fun wakeUpWaitersLocked(): List<CompletableDeferred<Unit>> {
        val list = mutableListOf<CompletableDeferred<Unit>>()
        while (waiters.isNotEmpty() && inFlight < currentLimit) {
            inFlight++
            list.add(waiters.removeFirst())
        }
        return list
    }

    companion object {
        fun isThrottleError(throwable: Throwable?): Boolean {
            if (throwable == null) return false
            val message = throwable.message.orEmpty()
            val causeMessage = throwable.cause?.message.orEmpty()
            val combined = "$message $causeMessage".lowercase()

            if (combined.contains("429") || combined.contains("503") || combined.contains("504")) return true
            if (combined.contains("too many requests") || combined.contains("rate limit") || combined.contains("ratelimit")) return true
            if (combined.contains("quota exceeded") || combined.contains("userratelimitexceeded")) return true
            if (combined.contains("server busy") || combined.contains("timeout") || combined.contains("timed out")) return true
            if (throwable is java.net.SocketTimeoutException) return true

            return false
        }
    }
}
