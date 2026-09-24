package com.denser.june.core.sync

import com.denser.june.core.domain.sync.MediaSyncMeta
import com.denser.june.core.sync.fixtures.SyncFixtures
import com.denser.june.core.sync.fixtures.SyncFixtures.T1
import com.denser.june.core.sync.fixtures.SyncFixtures.T2
import com.denser.june.core.sync.harness.BaseSyncTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SyncMediaTest : BaseSyncTest() {

    @Test
    fun `journal upload includes referenced media file`() = runTest {
        val mediaFile = harness.givenMediaFileOnDisk("photo1.jpg", byteArrayOf(1, 2, 3))
        harness.givenLocalOnlyJournal("j1", images = listOf(mediaFile.absolutePath), updatedAt = T1)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        harness.assertMediaExistsOnCloud("photo1.jpg")
    }

    @Test
    fun `journal download fetches missing media file`() = runTest {
        harness.cloud.putMedia("j1", "photo1.jpg", byteArrayOf(1, 2, 3))
        harness.givenCloudOnlyJournal("j1", images = listOf("photo1.jpg"), modifiedAt = T1)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        harness.assertMediaExistsOnDisk("photo1.jpg")
    }

    @Test
    fun `shared media already on cloud is not re-uploaded by media phase`() = runTest {
        val mediaFile = harness.givenMediaFileOnDisk("photo.jpg", byteArrayOf(1, 2, 3))
        val journal = SyncFixtures.syncedJournal("j1", updatedAt = T1, images = listOf(mediaFile.absolutePath))
        harness.repo.seed(journal)
        harness.cloud.putJournal(journal, modifiedAt = T1)
        harness.cloud.putMedia("j1", "photo.jpg", byteArrayOf(1, 2, 3))
        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 1,
            mediaMetadata = mapOf("photo.jpg" to MediaSyncMeta(size = 3L, hash = com.denser.june.core.utils.FileUtils.computeSHA256(mediaFile)))
        )

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertEquals("Media must not be uploaded when already in cloud with matching hash", 0, harness.cloud.uploadMediaCallCount)
    }

    @Test
    fun `media hash mismatch triggers re-upload`() = runTest {
        // Local file has different bytes (modified on disk) than cloud
        val localMedia = harness.givenMediaFileOnDisk("photo.jpg", byteArrayOf(9, 9, 9))
        val journal = SyncFixtures.syncedJournal("j1", updatedAt = T1, images = listOf(localMedia.absolutePath))
        harness.repo.seed(journal)
        harness.cloud.putJournal(journal, modifiedAt = T1)
        harness.cloud.putMedia("j1", "photo.jpg", byteArrayOf(1, 2, 3))
        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 1,
            mediaMetadata = mapOf("photo.jpg" to MediaSyncMeta(size = 3L, hash = "old_stale_hash"))
        )

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertEquals("Media must be re-uploaded on hash mismatch", 1, harness.cloud.uploadMediaCallCount)
    }

    @Test
    fun `analysis pendingMediaUploads matches actual sync uploads`() = runTest {
        val f1 = harness.givenMediaFileOnDisk("new1.jpg", byteArrayOf(1))
        val f2 = harness.givenMediaFileOnDisk("new2.jpg", byteArrayOf(2))
        val fAlready = harness.givenMediaFileOnDisk("existing.jpg", byteArrayOf(3))

        val j1 = SyncFixtures.syncedJournal("j1", updatedAt = T1, images = listOf(f1.absolutePath, fAlready.absolutePath))
        val j2 = SyncFixtures.syncedJournal("j2", updatedAt = T1, images = listOf(f2.absolutePath))
        harness.repo.seed(j1); harness.repo.seed(j2)
        harness.cloud.putJournal(j1, modifiedAt = T1); harness.cloud.putJournal(j2, modifiedAt = T1)
        harness.cloud.putMedia("j1", "existing.jpg", byteArrayOf(3))
        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 2,
            mediaMetadata = mapOf("existing.jpg" to MediaSyncMeta(size = 1L, hash = com.denser.june.core.utils.FileUtils.computeSHA256(fAlready)))
        )

        val analysis = harness.performAnalysis().getOrThrow()
        assertEquals("Analysis should report 2 media files to upload", 2, analysis.pendingMediaUploadsCount)

        harness.sync()
        assertEquals("Sync must upload exactly the 2 media files analysis predicted", 2, harness.cloud.uploadMediaCallCount)
    }

    @Test
    fun `orphaned cloud media not referenced by any journal shows zero pending downloads`() = runTest {
        harness.givenSyncedJournal("j1", updatedAt = T1)

        // Cloud has 5 "orphan" media files from another device
        repeat(5) { i -> harness.cloud.putMedia("other-device", "orphan$i.jpg", byteArrayOf(i.toByte())) }

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals("Orphaned cloud-only files must not count as pending downloads", 0, analysis.pendingMediaDownloadsCount)
        assertTrue("pendingMediaDownloadsList must be empty", analysis.pendingMediaDownloadsList.isEmpty())
    }

    @Test
    fun `missing media for synced journal is downloaded on next sync with canonical path`() = runTest {
        val j1 = SyncFixtures.syncedJournal("j1", syncedAt = T1, updatedAt = T1).copy(
            images = listOf(File(harness.mediaDir, "missing.jpg").absolutePath)
        )
        harness.repo.seed(j1)
        harness.cloud.putJournal(j1, modifiedAt = T1)
        harness.cloud.putMedia("j1", "missing.jpg", byteArrayOf(0xAA.toByte(), 0xBB.toByte()))
        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 1,
            mediaMetadata = mapOf("missing.jpg" to MediaSyncMeta(size = 2L, hash = ""))
        )

        val analysis = harness.performAnalysis().getOrThrow()
        assertEquals("Analysis must report 1 pending media download", 1, analysis.pendingMediaDownloadsCount)

        val result = harness.sync()
        assertTrue("Sync succeeds", result.isSuccess)
        harness.assertMediaExistsOnDisk("missing.jpg")
    }
}
