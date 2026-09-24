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

class SyncUploadTest : BaseSyncTest() {

    @Test
    fun `when cloud is empty, initial sync uploads all local journals`() = runTest {
        harness.givenLocalOnlyJournal("j1", updatedAt = T0)
        harness.givenLocalOnlyJournal("j2", updatedAt = T0)
        harness.givenLocalOnlyJournal("j3", updatedAt = T0)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        harness.assertCloudHasJournal("j1")
        harness.assertCloudHasJournal("j2")
        harness.assertCloudHasJournal("j3")
        harness.assertJournalSyncedLocally("j1")
        harness.assertJournalSyncedLocally("j2")
        harness.assertJournalSyncedLocally("j3")
        assertEquals(3, harness.cloud.manifest?.totalJournals)
    }

    @Test
    fun `when local and cloud are already in sync, sync is a no-op`() = runTest {
        harness.givenSyncedJournal("j1", updatedAt = T1)
        harness.givenSyncedJournal("j2", updatedAt = T1)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertEquals("No journals should be uploaded", 0, harness.cloud.uploadJournalCallCount)
        assertEquals("No journals should be downloaded", 0, harness.cloud.downloadJournalCallCount)
    }

    @Test
    fun `full revalidation uploads all local journals regardless of sync status`() = runTest {
        val j1 = SyncFixtures.syncedJournal("j1", syncedAt = T1, updatedAt = T1)
        val j2 = SyncFixtures.syncedJournal("j2", syncedAt = T1, updatedAt = T1)
        harness.repo.seed(j1); harness.repo.seed(j2)
        harness.cloud.putJournal(j1, modifiedAt = T1)
        harness.cloud.putJournal(j2, modifiedAt = T1)

        val result = harness.sync(isFullRevalidation = true)

        assertTrue(result.isSuccess)
        assertTrue((harness.repo.db["j1"]?.syncedAt ?: 0L) >= T1)
        assertTrue((harness.repo.db["j2"]?.syncedAt ?: 0L) >= T1)
    }

    @Test
    fun `revision counter increments when pushing local edits`() = runTest {
        harness.givenSyncedJournal("j1", title = "Original", updatedAt = T1, rev = 1)

        harness.userEditsJournalLocally("j1") {
            it.copy(title = "Edited", updatedAt = T2)
        }

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertEquals("Rev should increment from 1 to 2", 2, harness.cloud.manifest?.journalMetadata?.get("j1")?.rev)
    }

    @Test
    fun `local-wins journal is uploaded exactly once`() = runTest {
        // Local has newer edit than remote
        harness.givenSyncedJournal("j1", title = "Old Title", updatedAt = T1)
        harness.userEditsJournalLocally("j1") { it.copy(title = "Local Edit", updatedAt = T2) }

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertEquals("Journal must be uploaded exactly once", 1, harness.cloud.uploadJournalCallCount)
    }

    @Test
    fun `new local journal is uploaded exactly once`() = runTest {
        harness.givenLocalOnlyJournal("j_new", updatedAt = T0)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertEquals("New journal must be uploaded exactly once", 1, harness.cloud.uploadJournalCallCount)
    }

    @Test
    fun `large batch of new journals all upload successfully`() = runTest {
        val count = 25
        repeat(count) { i ->
            harness.givenLocalOnlyJournal("batch_$i", title = "Journal $i", updatedAt = T0)
        }

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertEquals(count, harness.cloud.uploadJournalCallCount)
        assertEquals(count, harness.cloud.manifest?.totalJournals)
        assertEquals(count, harness.cloud.manifest?.journalMetadata?.size)
    }
}
