package com.denser.june.core.domain.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Concurrency gate that holds/pauses background sync triggers and observers
 * during bulk data operations (such as app imports, markdown imports, or backup restores).
 */
class SyncGate {
    private val mutex = Mutex()
    private var holdCount = 0
    private val _isHeld = MutableStateFlow(false)
    val isHeld: StateFlow<Boolean> = _isHeld.asStateFlow()

    /**
     * Holds the gate for the duration of [block].
     * Increments the hold counter on entry and decrements on exit (exception and cancellation safe).
     */
    suspend fun <T> holdWhile(block: suspend () -> T): T {
        mutex.withLock {
            holdCount++
            _isHeld.value = true
        }
        return try {
            block()
        } finally {
            mutex.withLock {
                holdCount = maxOf(0, holdCount - 1)
                _isHeld.value = holdCount > 0
            }
        }
    }

    /**
     * Suspends if the gate is currently held until all active holds are released.
     */
    suspend fun awaitReleased() {
        if (!isHeld.value) return
        isHeld.first { !it }
    }
}
