package com.denser.june.core.sync

import com.denser.june.core.sync.fixtures.SyncFixtures
import com.denser.june.core.sync.fixtures.SyncFixtures.T1
import com.denser.june.core.sync.fixtures.SyncFixtures.T2
import com.denser.june.core.sync.harness.BaseSyncTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDeletionTest : BaseSyncTest() {

    @Test
    fun `when journal is permanently deleted locally, sync deletes remote file and clears tombstone`() = runTest {
        harness.cloud.putJournal(SyncFixtures.syncedJournal("j1"), modifiedAt = T1)
        harness.repo.tombstones.add("j1")

        val result = harness.sync()

        assertTrue(result.isSuccess)
        harness.assertCloudJournalDeleted("j1")
        assertFalse("Tombstone must be removed from local", harness.repo.tombstones.contains("j1"))
    }

    @Test
    fun `tombstone is cleared even when remote journal is already gone 404`() = runTest {
        harness.repo.tombstones.add("j1")

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertFalse("Tombstone must be cleared", harness.repo.tombstones.contains("j1"))
    }

    @Test
    fun `remote manifest deletedIds causes local hard delete`() = runTest {
        val j = SyncFixtures.syncedJournal("j1")
        harness.repo.seed(j)
        harness.cloud.manifest = SyncFixtures.manifest(deletedIds = listOf("j1"), totalJournals = 0)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        harness.assertLocalJournalHardDeleted("j1")
    }

    @Test
    fun `delete-edit conflict - delete wins and local edit is destroyed`() = runTest {
        val edited = SyncFixtures.syncedJournal("j1").copy(
            title = "Important edit",
            updatedAt = T2,
            syncedAt = T1
        )
        harness.repo.seed(edited)
        harness.cloud.manifest = SyncFixtures.manifest(deletedIds = listOf("j1"), totalJournals = 0)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        harness.assertLocalJournalHardDeleted("j1")
    }

    @Test
    fun `when journal is soft-deleted locally, subsequent sync does not download it from cloud`() = runTest {
        harness.givenSyncedJournal("j1", title = "My Secret Note", updatedAt = T1)

        // User soft-deletes j1 locally in app
        harness.userSoftDeletesJournal("j1")
        assertEquals("Active journals query must not include j1", 0, harness.repo.getAllJournals().size)
        harness.assertLocalJournalSoftDeleted("j1")

        // Pre-sync analysis reports 1 upload (to sync trash state) and 0 downloads
        val analysis = harness.performAnalysis().getOrThrow()
        assertEquals("Pending downloads must be 0", 0, analysis.pendingDownloadsCount)
        assertEquals("Pending uploads must be 1 to push soft-delete to cloud", 1, analysis.pendingUploadsCount)

        // Sync pushes soft-delete to cloud
        val syncResult = harness.sync()
        assertTrue("Sync succeeds", syncResult.isSuccess)

        // j1 must NOT return to active journals, must remain soft-deleted locally and on cloud
        assertEquals("Active journals must still be empty", 0, harness.repo.getAllJournals().size)
        harness.assertLocalJournalSoftDeleted("j1")
        assertNotNull("Cloud journal must have deletedAt set", harness.cloud.getJournal("j1")?.deletedAt)

        // Subsequent sync is clean no-op
        val nextSync = harness.sync()
        assertTrue(nextSync.isSuccess)
        assertEquals("Active journals must still be empty", 0, harness.repo.getAllJournals().size)
        harness.assertLocalJournalSoftDeleted("j1")
    }

    @Test
    fun `when cloud journal is soft-deleted by another device, local device downloads it into trash`() = runTest {
        harness.givenSyncedJournal("j1", title = "Active note", updatedAt = T1)
        assertEquals("Locally active", 1, harness.repo.getAllJournals().size)

        // Device B soft-deletes j1 on cloud recently (within 30 days)
        val recentDeleteTime = System.currentTimeMillis() - 5_000L
        harness.remoteJournalUpdated("j1", modifiedAt = recentDeleteTime) {
            it.copy(deletedAt = recentDeleteTime, updatedAt = recentDeleteTime)
        }

        // Analysis shows 1 download pending
        val analysis = harness.performAnalysis().getOrThrow()
        assertEquals("1 download pending", 1, analysis.pendingDownloadsCount)

        // Sync runs
        val syncResult = harness.sync()
        assertTrue("Sync succeeds", syncResult.isSuccess)

        // j1 moves into local trash
        assertEquals("Active journals must be empty", 0, harness.repo.getAllJournals().size)
        harness.assertLocalJournalSoftDeleted("j1")
    }

    @Test
    fun `when soft-deleted journal is restored locally, sync pushes restored journal to cloud`() = runTest {
        harness.givenSyncedJournal("j1", title = "Note in trash", updatedAt = T1)
        harness.userSoftDeletesJournal("j1")
        harness.sync()
        assertNotNull("Cloud has deletedAt", harness.cloud.getJournal("j1")?.deletedAt)

        // User restores j1 from trash locally with updated timestamp past sync threshold
        val restoreTime = (harness.repo.db["j1"]?.syncedAt ?: System.currentTimeMillis()) + 5_000L
        harness.userRestoresJournal("j1", restoredAt = restoreTime)
        assertEquals("Active journals has 1", 1, harness.repo.getAllJournals().size)

        // Sync pushes restored version
        val syncResult = harness.sync()
        assertTrue("Sync succeeds", syncResult.isSuccess)

        // Cloud journal now has deletedAt == null
        assertNull("Cloud journal deletedAt is cleared", harness.cloud.getJournal("j1")?.deletedAt)
        assertEquals("Active journals still 1", 1, harness.repo.getAllJournals().size)
    }

    @Test
    fun `hard-deleted journal deletes from cloud and records in manifest deletedIds for other devices`() = runTest {
        harness.givenSyncedJournal("j1", title = "To be permanently deleted", updatedAt = T1)

        // User permanently deletes j1 (hard delete / empty bin -> tombstone created)
        harness.userPermanentlyDeletesJournal("j1")
        harness.assertLocalJournalHardDeleted("j1")
        assertTrue("Tombstone exists for j1", harness.repo.tombstones.contains("j1"))

        // Sync runs
        val syncResult = harness.sync()
        assertTrue("Sync succeeds", syncResult.isSuccess)

        // Verify j1 is deleted from cloud and tombstone cleared
        harness.assertCloudJournalDeleted("j1")
        assertFalse("Tombstone cleared locally", harness.repo.tombstones.contains("j1"))

        // Verify cloud manifest records j1 in deletedIds
        harness.assertManifestDeletedIdsContains("j1")

        // Subsequent sync does not re-download j1
        val nextSync = harness.sync()
        assertTrue(nextSync.isSuccess)
        harness.assertLocalJournalHardDeleted("j1")
    }
}
