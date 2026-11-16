package com.denser.june.core.sync

import com.denser.june.core.domain.sync.SyncStatus
import com.denser.june.core.sync.fixtures.SyncFixtures.T1
import com.denser.june.core.sync.harness.BaseSyncTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncAfterBulkImportTest : BaseSyncTest() {

    @Test
    fun `during bulk import inside holdWhile observers remain silent and no auto-sync is enqueued`() = runTest {
        harness.prefs.setSyncEnabled(true)
        harness.prefs.setAutomaticSyncEnabled(true)

        harness.syncGate.holdWhile {
            for (i in 1..20) {
                harness.givenLocalOnlyJournal("imported-$i", updatedAt = T1 + i)
            }
            harness.testDispatcher.scheduler.advanceUntilIdle()

            // While gate is held, auto-sync must NOT be enqueued and status must remain Idle
            assertEquals("No auto-sync should be enqueued during import", 0, harness.syncScheduler.enqueuedCalls.size)
            assertTrue("Status should remain non-dirty during import", harness.syncManager.status.value is SyncStatus.Idle)
        }
    }

    @Test
    fun `on gate release after bulk import immediate auto-sync is triggered without waiting for debounce`() = runTest {
        harness.prefs.setSyncEnabled(true)
        harness.prefs.setAutomaticSyncEnabled(true)

        harness.syncGate.holdWhile {
            for (i in 1..10) {
                harness.givenLocalOnlyJournal("imported-$i", updatedAt = T1 + i)
            }
        }

        // Gate is released: advance scheduler slightly
        harness.testDispatcher.scheduler.advanceUntilIdle()

        // Immediate sync must have been requested
        assertTrue("Auto-sync must be enqueued immediately after gate release", harness.syncScheduler.enqueuedCalls.isNotEmpty())
    }

    @Test
    fun `on gate release when auto-sync is disabled no sync is enqueued and status transitions to Dirty`() = runTest {
        harness.prefs.setSyncEnabled(true)
        harness.prefs.setAutomaticSyncEnabled(false)

        harness.syncGate.holdWhile {
            for (i in 1..5) {
                harness.givenLocalOnlyJournal("manual-$i", updatedAt = T1 + i)
            }
        }

        harness.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("No auto-sync should be enqueued when auto-sync is disabled", 0, harness.syncScheduler.enqueuedCalls.size)
        assertTrue("Status must be Dirty indicating changes need manual sync", harness.syncManager.status.value is SyncStatus.Dirty)
    }

    @Test
    fun `mid-import sync call suspends at gate and completes cleanly once import finishes`() = runTest {
        harness.prefs.setSyncEnabled(true)

        var importFinished = false

        val syncDeferred = async {
            harness.sync()
        }

        harness.syncGate.holdWhile {
            for (i in 1..15) {
                harness.givenLocalOnlyJournal("mid-$i", updatedAt = T1 + i)
            }
            // Sync was launched, but because gate is held, it should be waiting at awaitReleased()
            harness.testDispatcher.scheduler.advanceUntilIdle()
            assertEquals("Uploads must not have started yet while import holds gate", 0, harness.cloud.uploadJournalCallCount)
            assertFalse(syncDeferred.isCompleted)
            importFinished = true
        }

        // Now gate is released: sync should proceed
        val result = syncDeferred.await()
        assertTrue("Import must have completed before sync finished", importFinished)
        assertTrue("Sync must succeed", result.isSuccess)
        assertEquals("All 15 journals must be uploaded", 15, harness.cloud.uploadJournalCallCount)
    }

    @Test
    fun `scaled bulk simulation with 1050 items uploads all entries and checkpoints correctly`() = runTest {
        harness.prefs.setSyncEnabled(true)
        val count = 1050

        // Simulate bulk import wrapped in SyncGate
        harness.syncGate.holdWhile {
            for (i in 1..count) {
                harness.givenLocalOnlyJournal("bulk-$i", updatedAt = T1 + i)
            }
        }

        val result = harness.sync()
        assertTrue("Bulk sync must succeed", result.isSuccess)
        assertEquals("All 1050 items must be uploaded to cloud", count, harness.cloud.uploadJournalCallCount)
        assertTrue(
            "Manifest must be updated at least twice (checkpoint at 1000 + final)",
            harness.cloud.updateManifestCallCount >= 2
        )
        for (i in 1..count) {
            assertNotNull(harness.cloud.getJournal("bulk-$i"))
        }
    }

    @Test
    fun `scaled bulk simulation with 3500 items uploads smoothly across multiple checkpoints`() = runTest {
        harness.prefs.setSyncEnabled(true)
        val count = 3500

        harness.syncGate.holdWhile {
            for (i in 1..count) {
                harness.givenLocalOnlyJournal("scaled-$i", updatedAt = T1 + i)
            }
        }

        val startTime = System.currentTimeMillis()
        val result = harness.sync()
        val durationMs = System.currentTimeMillis() - startTime

        assertTrue("Bulk sync must succeed", result.isSuccess)
        assertEquals("All 3500 items must be uploaded to cloud", count, harness.cloud.uploadJournalCallCount)
        assertTrue(
            "Manifest must be updated at least 4 times (checkpoints at 1000, 2000, 3000 + final)",
            harness.cloud.updateManifestCallCount >= 4
        )
        assertTrue("Syncing 3500 items in memory with batching should take < 5000ms, took: ${durationMs}ms", durationMs < 5000)
    }
}
