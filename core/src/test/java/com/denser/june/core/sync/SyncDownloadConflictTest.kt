package com.denser.june.core.sync

import com.denser.june.core.sync.fixtures.SyncFixtures
import com.denser.june.core.sync.fixtures.SyncFixtures.T0
import com.denser.june.core.sync.fixtures.SyncFixtures.T1
import com.denser.june.core.sync.fixtures.SyncFixtures.T2
import com.denser.june.core.sync.harness.BaseSyncTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDownloadConflictTest : BaseSyncTest() {

    @Test
    fun `when local is empty, sync downloads all journals from cloud`() = runTest {
        harness.givenCloudOnlyJournal("j1", modifiedAt = T1)
        harness.givenCloudOnlyJournal("j2", modifiedAt = T1)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        harness.assertLocalJournalActive("j1")
        harness.assertLocalJournalActive("j2")
    }

    @Test
    fun `conflict - local wins when local edit is newer than remote`() = runTest {
        val originalSyncedAt = T1
        val deviceAJournal = SyncFixtures.deviceAModified("j1")  // updatedAt = T2 (older edit)
        val deviceBJournal = SyncFixtures.deviceBModified("j1").copy(updatedAt = SyncFixtures.T4)  // updatedAt = T4 > T2 (newer edit)

        // Device B's local state (syncedAt=T1, updatedAt=T4)
        harness.repo.seed(deviceBJournal.copy(syncedAt = originalSyncedAt))

        // Cloud has device A's version (modifiedAt=T2)
        harness.cloud.putJournal(deviceAJournal, modifiedAt = T2)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        // Local (Device B) is newer (T4 > T2), so local version should win and push to cloud
        assertEquals(
            "Modified by B",
            harness.repo.db["j1"]?.title
        )
    }

    @Test
    fun `local wins when local edit timestamp is newer than remote`() = runTest {
        val local = SyncFixtures.syncedJournal("j1", title = "Local Newer", updatedAt = T2, syncedAt = T0)
        harness.repo.seed(local)

        val remote = SyncFixtures.syncedJournal("j1", title = "Remote Older", updatedAt = T1, syncedAt = T0)
        harness.cloud.putJournal(remote, modifiedAt = T1)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        val cloudJournal = harness.cloud.getJournal("j1")
        assertEquals("Cloud should have local newer title", "Local Newer", cloudJournal?.title)
    }

    @Test
    fun `remote wins when remote timestamp is newer than local without local edit`() = runTest {
        // Local is unchanged since T0
        val local = SyncFixtures.syncedJournal("j1", title = "Original", updatedAt = T0, syncedAt = T0)
        harness.repo.seed(local)

        // Remote has new edit at T2
        val remote = SyncFixtures.syncedJournal("j1", title = "Remote Newer", updatedAt = T2, syncedAt = T0)
        harness.cloud.putJournal(remote, modifiedAt = T2)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertEquals("Local must have updated to remote content", "Remote Newer", harness.repo.db["j1"]?.title)
    }

    @Test
    fun `no upload when content is identical despite timestamp difference`() = runTest {
        // Content identical between local and remote, but local updatedAt bumped (e.g. tag reindex)
        val j = SyncFixtures.syncedJournal("j1", title = "Same Content", updatedAt = T2, syncedAt = T1)
        harness.repo.seed(j)
        harness.cloud.putJournal(j.copy(updatedAt = T1), modifiedAt = T1)
        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 1,
            journalMetadata = mapOf("j1" to com.denser.june.core.domain.sync.JournalSyncMeta(rev = 1, contentHash = j.computeContentHash()))
        )

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertEquals("Should NOT re-upload when content hashes match", 0, harness.cloud.uploadJournalCallCount)
    }

    @Test
    fun `content hash match skips network operations`() = runTest {
        val j1 = harness.givenSyncedJournal("j1", title = "Note", updatedAt = T1)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertEquals(0, harness.cloud.uploadJournalCallCount)
        assertEquals(0, harness.cloud.downloadJournalCallCount)
    }

    @Test
    fun `empty local and empty remote produces valid manifest`() = runTest {
        val result = harness.sync()

        assertTrue("Empty sync succeeds", result.isSuccess)
        assertNotNull("Manifest is written", harness.cloud.manifest)
        assertEquals(0, harness.cloud.manifest?.totalJournals)
        assertEquals(0, harness.cloud.manifest?.journalMetadata?.size)
    }

    @Test
    fun `remote wins when remote timestamp is newer but preserves unsynced local edit in conflict copy`() = runTest {
        // Device A has uncommitted local edits at T1 (synced at T0)
        val local = SyncFixtures.syncedJournal("j1", title = "Local My Draft", updatedAt = T1, syncedAt = T0)
            .copy(content = "My Local Edits")
        harness.repo.seed(local)

        // Device B has newer edits at T2 and pushed to cloud
        val remote = SyncFixtures.syncedJournal("j1", title = "Remote Collab", updatedAt = T2, syncedAt = T0)
            .copy(content = "Remote Changes")
        harness.cloud.putJournal(remote, modifiedAt = T2)
        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 1,
            journalMetadata = mapOf("j1" to com.denser.june.core.domain.sync.JournalSyncMeta(rev = 2, contentHash = remote.computeContentHash()))
        )

        val result = harness.sync()

        assertTrue(result.isSuccess)
        // Primary journal slot j1 takes remote changes (LWW)
        assertEquals("Remote Collab", harness.repo.db["j1"]?.title)
        assertEquals("Remote Changes", harness.repo.db["j1"]?.content)

        // Local unsynced work must NOT be lost: preserved in a conflict copy!
        val conflictCopy = harness.repo.db.values.firstOrNull { it.id != "j1" }
        assertNotNull("Conflict copy must be created", conflictCopy)
        assertEquals("Local My Draft (Conflict Copy)", conflictCopy?.title)
        assertEquals("My Local Edits", conflictCopy?.content)
    }
}
