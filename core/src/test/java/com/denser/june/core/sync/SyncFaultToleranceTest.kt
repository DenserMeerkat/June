package com.denser.june.core.sync

import com.denser.june.core.domain.sync.SyncManager
import com.denser.june.core.domain.sync.SyncManifest
import com.denser.june.core.domain.sync.SyncStatus
import com.denser.june.core.sync.fakes.FakeCloudProvider
import com.denser.june.core.sync.fakes.FakeSyncScheduler
import com.denser.june.core.sync.fixtures.SyncFixtures
import com.denser.june.core.sync.fixtures.SyncFixtures.T0
import com.denser.june.core.sync.fixtures.SyncFixtures.T1
import com.denser.june.core.sync.harness.BaseSyncTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncFaultToleranceTest : BaseSyncTest() {

    @Test
    fun `partial upload failure does not write manifest and preserves other uploaded journals`() = runTest {
        harness.givenLocalOnlyJournal("j1", updatedAt = T0)
        harness.givenLocalOnlyJournal("j2", updatedAt = T0)
        harness.cloud.failNextUpload = true

        val result = harness.sync()

        assertFalse("Sync must report failure on partial upload", result.isSuccess)
        assertNull("Manifest must not be written if any upload failed", harness.cloud.manifest)
    }

    @Test
    fun `download failure reports error and leaves already synced items safe`() = runTest {
        harness.givenLocalOnlyJournal("local", updatedAt = T0)
        val remoteJ = SyncFixtures.newJournal("remote-fail", title = "From cloud")
        harness.cloud.putJournal(remoteJ, modifiedAt = T1)
        harness.cloud.failNextDownload = true

        val result = harness.sync()

        assertTrue("local journal should have been uploaded", harness.cloud.allJournalIds().contains("local"))
        assertNull(harness.repo.db["remote-fail"])
        assertFalse(result.isSuccess)
    }

    @Test
    fun `connect failure aborts sync cleanly`() = runTest {
        harness.cloud.failNextConnect = true
        harness.givenLocalOnlyJournal("j1", updatedAt = T0)

        val result = harness.sync()

        assertFalse("Sync must fail on connect failure", result.isSuccess)
        assertEquals(0, harness.cloud.uploadJournalCallCount)
        assertTrue(harness.syncManager.status.first() is SyncStatus.Error)
    }

    @Test
    fun `empty remote cloud does not delete previously synced local journals`() = runTest {
        val j1 = SyncFixtures.syncedJournal("j1", updatedAt = T1, syncedAt = T1)
        val j2 = SyncFixtures.syncedJournal("j2", updatedAt = T1, syncedAt = T1)
        harness.repo.seed(j1); harness.repo.seed(j2)
        // Cloud is empty (e.g. wiped or wrong folder)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        harness.assertLocalJournalActive("j1")
        harness.assertLocalJournalActive("j2")
        harness.assertCloudHasJournal("j1")
        harness.assertCloudHasJournal("j2")
    }

    @Test
    fun `sync does nothing when sync is disabled`() = runTest {
        harness.prefs.setSyncEnabled(false)
        harness.givenLocalOnlyJournal("j1", updatedAt = T0)

        val result = harness.sync()

        assertFalse("Sync returns failure when disabled", result.isSuccess)
        assertEquals("No connect should happen", 0, harness.cloud.uploadJournalCallCount)
        assertFalse("j1 not uploaded", harness.cloud.allJournalIds().contains("j1"))
    }

    @Test
    fun `when remote listing is truncated, sync aborts to prevent accidental local deletion`() = runTest {
        val j1 = SyncFixtures.syncedJournal("j1", updatedAt = T1, syncedAt = T1)
        val j2 = SyncFixtures.syncedJournal("j2", updatedAt = T1, syncedAt = T1)
        val j3 = SyncFixtures.syncedJournal("j3", updatedAt = T1, syncedAt = T1)
        val j4 = SyncFixtures.syncedJournal("j4", updatedAt = T1, syncedAt = T1)
        val j5 = SyncFixtures.syncedJournal("j5", updatedAt = T1, syncedAt = T1)
        listOf(j1, j2, j3, j4, j5).forEach {
            harness.repo.seed(it)
            harness.cloud.putJournal(it, modifiedAt = T1)
        }
        harness.cloud.manifest = SyncFixtures.manifest(totalJournals = 5)

        // Provider returns truncated listing (only 2 out of 5)
        harness.cloud.partialListingSize = 2

        harness.sync()

        assertNotNull("j3 must not be deleted due to partial listing", harness.repo.db["j3"])
        assertNotNull("j4 must not be deleted due to partial listing", harness.repo.db["j4"])
        assertNotNull("j5 must not be deleted due to partial listing", harness.repo.db["j5"])
    }

    @Test
    fun `schema version too high aborts sync`() = runTest {
        harness.cloud.manifest = SyncFixtures.manifest(
            schemaVersion = SyncManifest.CURRENT_SCHEMA_VERSION + 1,
            totalJournals = 1
        )
        harness.givenLocalOnlyJournal("j1", updatedAt = T0)

        val result = harness.sync()

        assertFalse("Sync should abort when schema version is unsupported", result.isSuccess)
        assertEquals("No upload should occur", 0, harness.cloud.uploadJournalCallCount)
        val status = harness.syncManager.status.first()
        assertTrue("Status must be Error", status is SyncStatus.Error)
        assertTrue(
            "Error message should mention app update",
            (status as SyncStatus.Error).message.contains("newer version", ignoreCase = true)
        )
    }

    @Test
    fun `provider switch - local journals missing from new provider are uploaded, not deleted`() = runTest {
        val j1 = SyncFixtures.syncedJournal("j1", updatedAt = T1, syncedAt = T1)
        val j2 = SyncFixtures.syncedJournal("j2", updatedAt = T1, syncedAt = T1)
        harness.repo.seed(j1); harness.repo.seed(j2)

        val newProvider = FakeCloudProvider()
        val multiProviderSyncManager = SyncManager(
            journalRepo = harness.repo,
            syncPrefs = harness.prefs,
            providers = mapOf("ProviderA" to harness.cloud, "ProviderB" to newProvider),
            mediaDir = harness.mediaDir,
            syncScheduler = FakeSyncScheduler(),
            applicationScope = CoroutineScope(harness.testDispatcher),
            songLibraryDao = harness.songDao,
            songMediaDir = harness.songMediaDir
        )

        harness.prefs.setSelectedProvider("ProviderB")
        val result = multiProviderSyncManager.sync()

        assertTrue(result.isSuccess)
        assertNotNull("j1 must NOT be deleted locally", harness.repo.db["j1"])
        assertNotNull("j2 must NOT be deleted locally", harness.repo.db["j2"])
        assertTrue("j1 must be uploaded to new provider", newProvider.allJournalIds().contains("j1"))
        assertTrue("j2 must be uploaded to new provider", newProvider.allJournalIds().contains("j2"))
    }

    @Test
    fun `explicit manifest deletion still works across provider switch`() = runTest {
        val j1 = SyncFixtures.syncedJournal("j1")
        harness.repo.seed(j1)

        val newProvider = FakeCloudProvider()
        newProvider.manifest = SyncFixtures.manifest(deletedIds = listOf("j1"))

        val multiProviderSyncManager = SyncManager(
            journalRepo = harness.repo,
            syncPrefs = harness.prefs,
            providers = mapOf("ProviderA" to harness.cloud, "ProviderB" to newProvider),
            mediaDir = harness.mediaDir,
            syncScheduler = FakeSyncScheduler(),
            applicationScope = CoroutineScope(harness.testDispatcher),
            songLibraryDao = harness.songDao,
            songMediaDir = harness.songMediaDir
        )

        harness.prefs.setSelectedProvider("ProviderB")
        val result = multiProviderSyncManager.sync()

        assertTrue(result.isSuccess)
        assertNull("j1 must be hard deleted when manifest explicitly specifies deletedIds", harness.repo.db["j1"])
    }

    @Test
    fun `legacy schemaVersion 2 manifest fallback succeeds`() = runTest {
        val j1 = SyncFixtures.syncedJournal("j1")
        harness.cloud.putJournal(j1, modifiedAt = T1)
        harness.cloud.manifest = SyncFixtures.manifest(
            schemaVersion = 2,
            totalJournals = 1,
            deletedIds = emptyList()
        )

        val result = harness.sync()

        assertTrue("Sync with schemaVersion 2 manifest should succeed", result.isSuccess)
        harness.assertLocalJournalActive("j1")
    }

    @Test
    fun `status is Success after sync completes not Dirty`() = runTest {
        harness.givenLocalOnlyJournal("j1", updatedAt = T0)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        val status = harness.syncManager.status.first()
        assertTrue("Status must be Success immediately after sync, was: $status", status is SyncStatus.Success)
    }

    @Test
    fun `media upload failure reports error and does not include failed media in manifest`() = runTest {
        val photo = harness.givenMediaFileOnDisk("broken.jpg", byteArrayOf(1, 2, 3))
        harness.givenLocalOnlyJournal("j1", images = listOf(photo.absolutePath), updatedAt = T0)
        harness.cloud.failNextUploadMedia = true

        val result = harness.sync()

        assertFalse("Sync must fail when media upload fails", result.isSuccess)
        val status = harness.syncManager.status.first()
        assertTrue("Status must be Error, was $status", status is SyncStatus.Error)
        assertNull("Manifest must not be written when sync fails", harness.cloud.manifest)
    }
}
