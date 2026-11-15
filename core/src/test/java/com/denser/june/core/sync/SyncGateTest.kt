package com.denser.june.core.sync

import com.denser.june.core.domain.sync.SyncGate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncGateTest {

    @Test
    fun `holdWhile sets isHeld to true during execution and returns to false upon completion`() = runTest {
        val gate = SyncGate()
        assertFalse("Initial gate must not be held", gate.isHeld.value)

        var executed = false
        val result = gate.holdWhile {
            assertTrue("Gate must be held inside holdWhile block", gate.isHeld.value)
            executed = true
            42
        }

        assertTrue("Block must have executed", executed)
        assertEquals(42, result)
        assertFalse("Gate must be released after holdWhile completes", gate.isHeld.value)
    }

    @Test
    fun `holdWhile releases gate even when block throws an exception`() = runTest {
        val gate = SyncGate()
        try {
            gate.holdWhile {
                assertTrue("Gate must be held", gate.isHeld.value)
                throw IllegalStateException("Simulated import error")
            }
            fail("Expected exception was not thrown")
        } catch (e: IllegalStateException) {
            assertEquals("Simulated import error", e.message)
        }
        assertFalse("Gate must be released after exception", gate.isHeld.value)
    }

    @Test
    fun `nested holdWhile invocations count holds and only release when outermost block finishes`() = runTest {
        val gate = SyncGate()
        assertFalse(gate.isHeld.value)

        gate.holdWhile {
            assertTrue("Outermost hold", gate.isHeld.value)
            gate.holdWhile {
                assertTrue("Inner hold", gate.isHeld.value)
            }
            assertTrue("Gate must still be held after inner release", gate.isHeld.value)
        }
        assertFalse("Gate must be released after all holds exit", gate.isHeld.value)
    }

    @Test
    fun `awaitReleased suspends while held and resumes immediately upon release`() = runTest {
        val gate = SyncGate()
        var resumed = false

        val job = launch {
            gate.holdWhile {
                delay(100)
            }
        }

        val waiter = launch {
            gate.awaitReleased()
            resumed = true
        }

        job.join()
        waiter.join()
        assertTrue("Waiter must resume after gate release", resumed)
        assertFalse(gate.isHeld.value)
    }
}
