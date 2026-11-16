package com.denser.june.core.sync

import com.denser.june.core.domain.sync.AdaptiveConcurrencyLimiter
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveConcurrencyLimiterTest {

    @Test
    fun `initial limit starts at configured initial value`() = runTest {
        val limiter = AdaptiveConcurrencyLimiter(minLimit = 2, maxLimit = 8, initialLimit = 4)
        assertEquals(4, limiter.currentLimit)
        assertEquals(0, limiter.inFlight)
    }

    @Test
    fun `consecutive successes ramp up concurrency limit to max`() = runTest {
        val limiter = AdaptiveConcurrencyLimiter(minLimit = 2, maxLimit = 6, initialLimit = 4, successThreshold = 5)

        repeat(5) {
            limiter.acquire()
            limiter.release(isSuccess = true, isThrottle = false)
        }
        assertEquals(5, limiter.currentLimit)

        repeat(5) {
            limiter.acquire()
            limiter.release(isSuccess = true, isThrottle = false)
        }
        assertEquals(6, limiter.currentLimit)

        repeat(5) {
            limiter.acquire()
            limiter.release(isSuccess = true, isThrottle = false)
        }
        assertEquals(6, limiter.currentLimit)
    }

    @Test
    fun `throttle backs off concurrency limit to half and clamped by min`() = runTest {
        val limiter = AdaptiveConcurrencyLimiter(minLimit = 3, maxLimit = 10, initialLimit = 8)
        assertEquals(8, limiter.currentLimit)

        limiter.acquire()
        limiter.release(isSuccess = false, isThrottle = true)
        assertEquals(4, limiter.currentLimit)

        limiter.acquire()
        limiter.release(isSuccess = false, isThrottle = true)
        assertEquals(3, limiter.currentLimit)
    }

    @Test
    fun `waiters queue and resume as permits become available`() = runTest {
        val limiter = AdaptiveConcurrencyLimiter(minLimit = 1, maxLimit = 4, initialLimit = 2)

        limiter.acquire()
        limiter.acquire()
        assertEquals(2, limiter.inFlight)

        var thirdAcquired = false
        val job = launch {
            limiter.acquire()
            thirdAcquired = true
        }

        testScheduler.runCurrent()
        assertTrue("Third worker should be suspended waiting for a permit", !thirdAcquired)

        limiter.release(isSuccess = true, isThrottle = false)
        testScheduler.runCurrent()

        assertTrue("Third worker should now be resumed", thirdAcquired)
        assertEquals(2, limiter.inFlight)

        limiter.release(isSuccess = true, isThrottle = false)
        job.join()
    }

    @Test
    fun `isThrottleError identifies 429, 503, rate limit and timeout errors`() {
        assertTrue(AdaptiveConcurrencyLimiter.isThrottleError(Exception("Upload failed: 429")))
        assertTrue(AdaptiveConcurrencyLimiter.isThrottleError(Exception("Upload failed: 503")))
        assertTrue(AdaptiveConcurrencyLimiter.isThrottleError(Exception("Server returned 504 Gateway Timeout")))
        assertTrue(AdaptiveConcurrencyLimiter.isThrottleError(Exception("Rate limit exceeded")))
        assertTrue(AdaptiveConcurrencyLimiter.isThrottleError(Exception("UserRateLimitExceeded")))
        assertTrue(AdaptiveConcurrencyLimiter.isThrottleError(java.net.SocketTimeoutException("Read timed out")))
        assertTrue(!AdaptiveConcurrencyLimiter.isThrottleError(Exception("404 Not Found")))
        assertTrue(!AdaptiveConcurrencyLimiter.isThrottleError(Exception("Bad Request: 400")))
        assertTrue(!AdaptiveConcurrencyLimiter.isThrottleError(null))
    }
}
