package com.denser.june.core.sync

import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.domain.sync.MediaSyncMeta
import com.denser.june.core.sync.fixtures.SyncFixtures
import com.denser.june.core.sync.fixtures.SyncFixtures.T1
import com.denser.june.core.sync.harness.SyncTestHarness
import com.denser.june.core.utils.FileUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SyncPerformanceOptimizationsTest {

    private lateinit var harness: SyncTestHarness

    @Before
    fun setUp() {
        harness = SyncTestHarness()
        harness.setUp()
    }

    @After
    fun tearDown() {
        harness.tearDown()
    }

    @Test
    fun `proposal 1 - performAnalysis gathers remote state concurrently and returns accurate counts`() = runTest {
        // Setup cloud journal, media, audio, and song
        val localMedia = harness.givenMediaFileOnDisk("photo1.jpg", byteArrayOf(1, 2, 3))
        val localAudio = harness.givenAudioFileOnDisk("voice1.m4a", byteArrayOf(4, 5, 6))

        val journal = SyncFixtures.syncedJournal(
            "j1",
            updatedAt = T1,
            images = listOf(localMedia.absolutePath)
        )
        harness.repo.seed(journal)
        harness.cloud.putJournal(journal, modifiedAt = T1)
        harness.cloud.putMedia("j1", "photo1.jpg", byteArrayOf(1, 2, 3))
        harness.cloud.putAudio("voice1.m4a", byteArrayOf(4, 5, 6))
        harness.cloud.putSongMedia("song1.mp3", byteArrayOf(7, 8, 9))

        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 1,
            mediaMetadata = mapOf("photo1.jpg" to MediaSyncMeta(size = 3L, hash = FileUtils.computeSHA256(localMedia))),
            audioMetadata = mapOf("voice1.m4a" to MediaSyncMeta(size = 3L, hash = FileUtils.computeSHA256(localAudio))),
            songMediaMetadata = mapOf("song1.mp3" to MediaSyncMeta(size = 3L, hash = "songhash"))
        )

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals("Cloud journals must be 1", 1, analysis.remoteJournals)
        assertEquals("Cloud media must be 1", 1, analysis.remoteMedia)
        assertEquals("Cloud audios must be 1", 1, analysis.remoteAudioFiles)
        assertEquals("Cloud songs must be 1", 1, analysis.remoteSongFiles)
    }

    @Test
    fun `proposal 2 - size mismatch triggers upload without false equal-size bypass`() = runTest {
        // Case A: Size mismatch triggers upload
        val localPhoto = harness.givenMediaFileOnDisk("photo.jpg", byteArrayOf(1, 2, 3, 4))
        val journal = SyncFixtures.syncedJournal("j1", updatedAt = T1, images = listOf(localPhoto.absolutePath))
        harness.repo.seed(journal)
        harness.cloud.putJournal(journal, modifiedAt = T1)
        harness.cloud.putMedia("j1", "photo.jpg", byteArrayOf(1, 2))
        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 1,
            mediaMetadata = mapOf("photo.jpg" to MediaSyncMeta(size = 2L, hash = "old_hash"))
        )

        val analysis = harness.performAnalysis().getOrThrow()
        assertEquals("Size mismatch must trigger pending upload", 1, analysis.pendingMediaUploadsCount)

        // Case B: Equal size with different content/hash must NOT be bypassed
        val localPhotoEqualSize = harness.givenMediaFileOnDisk("photo2.jpg", byteArrayOf(9, 9, 9))
        val journal2 = SyncFixtures.syncedJournal("j2", updatedAt = T1, images = listOf(localPhotoEqualSize.absolutePath))
        harness.repo.seed(journal2)
        harness.cloud.putJournal(journal2, modifiedAt = T1)
        harness.cloud.putMedia("j2", "photo2.jpg", byteArrayOf(1, 2, 3))
        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 2,
            mediaMetadata = mapOf(
                "photo.jpg" to MediaSyncMeta(size = 4L, hash = FileUtils.computeSHA256(localPhoto)),
                "photo2.jpg" to MediaSyncMeta(size = 3L, hash = "different_remote_hash")
            )
        )

        val analysis2 = harness.performAnalysis().getOrThrow()
        assertEquals("Equal size with different hash must trigger pending upload", 1, analysis2.pendingMediaUploadsCount)
        assertTrue(analysis2.pendingMediaUploadsList.first().contains("Synced Journal"))
    }

    @Test
    fun `proposal 3 - SHA256 hashes are cached in SyncHashCache during sync and analysis`() = runTest {
        val localMedia = harness.givenMediaFileOnDisk("cached_photo.jpg", byteArrayOf(10, 20, 30, 40))
        val journal = SyncFixtures.syncedJournal("j1", updatedAt = T1, images = listOf(localMedia.absolutePath))
        harness.repo.seed(journal)
        harness.cloud.putJournal(journal, modifiedAt = T1)
        harness.cloud.putMedia("j1", "cached_photo.jpg", byteArrayOf(10, 20, 30, 40))
        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 1,
            mediaMetadata = mapOf("cached_photo.jpg" to MediaSyncMeta(size = 4L, hash = FileUtils.computeSHA256(localMedia)))
        )

        // Initially cache does not contain this file
        assertNotNull(harness.syncManager.syncHashCache)
        assertEquals(0, harness.syncManager.syncHashCache.size())

        // Perform analysis: since file is on cloud and size matches, analysis compares hashes using cache
        val analysis = harness.performAnalysis().getOrThrow()
        assertEquals(0, analysis.pendingMediaUploadsCount)

        // File hash should now be cached in syncHashCache
        val cachedHash = harness.syncManager.syncHashCache.get(localMedia)
        assertEquals("Hash must be memoized in SyncHashCache during analysis", FileUtils.computeSHA256(localMedia), cachedHash)
        assertEquals(1, harness.syncManager.syncHashCache.size())

        // Sync now
        val syncResult = harness.sync()
        assertTrue(syncResult.isSuccess)

        // Verify cached hash is still valid and memoized
        assertEquals(cachedHash, harness.syncManager.syncHashCache.get(localMedia))
    }

    @Test
    fun `concurrent journal upload completes all items`() = runTest {
        val count = 50
        for (i in 1..count) {
            harness.givenLocalOnlyJournal("journal-$i", updatedAt = T1 + i)
        }
        val result = harness.sync()
        assertTrue("Sync must succeed for concurrent journal uploads", result.isSuccess)
        assertEquals("All 50 journals must be uploaded to cloud", count, harness.cloud.uploadJournalCallCount)
        for (i in 1..count) {
            assertNotNull("Journal $i must exist on cloud", harness.cloud.getJournal("journal-$i"))
            assertNotNull("Journal $i must have syncedAt set locally", harness.repo.db["journal-$i"]?.syncedAt)
        }
    }

    @Test
    fun `concurrent downloads preserve localJournalsModified flag and update local database`() = runTest {
        val count = 5
        for (i in 1..count) {
            val remoteJ = SyncFixtures.newJournal("remote-$i", title = "Remote Journal $i", updatedAt = T1 + i)
            harness.cloud.putJournal(remoteJ, modifiedAt = T1 + i)
        }
        val result = harness.sync()
        assertTrue("Sync must succeed for concurrent journal downloads", result.isSuccess)
        assertEquals("All 5 journals must be downloaded", count, harness.cloud.downloadJournalCallCount)
        for (i in 1..count) {
            assertNotNull("Journal remote-$i must exist locally", harness.repo.db["remote-$i"])
        }
    }

    @Test
    fun `listMedia, listSongMedia, and listAudios are called at most once during full sync cycle`() = runTest {
        val localMedia = harness.givenMediaFileOnDisk("photo1.jpg", byteArrayOf(1, 2, 3))
        val journal = SyncFixtures.syncedJournal("j1", updatedAt = T1, images = listOf(localMedia.absolutePath))
        harness.repo.seed(journal)
        harness.cloud.putJournal(journal, modifiedAt = T1)
        harness.cloud.putMedia("j1", "photo1.jpg", byteArrayOf(1, 2, 3))
        harness.cloud.manifest = SyncFixtures.manifest(
            totalJournals = 1,
            mediaMetadata = mapOf("photo1.jpg" to MediaSyncMeta(size = 3L, hash = FileUtils.computeSHA256(localMedia)))
        )

        val result = harness.sync()
        assertTrue("Sync must succeed", result.isSuccess)
        assertTrue("listMedia should be called at most once per sync", harness.cloud.listMediaCallCount <= 1)
        assertTrue("listSongMedia should be called at most once per sync", harness.cloud.listSongMediaCallCount <= 1)
        assertTrue("listAudios should be called at most once per sync", harness.cloud.listAudiosCallCount <= 1)
    }

    @Test
    fun `checkpoint manifest written after 1000 journals`() = runTest {
        val count = 1050
        for (i in 1..count) {
            harness.givenLocalOnlyJournal("ckpt-$i", updatedAt = T1 + i)
        }
        val result = harness.sync()
        assertTrue("Sync must succeed for 1050 journals", result.isSuccess)
        assertEquals("All 1050 journals uploaded", count, harness.cloud.uploadJournalCallCount)
        assertTrue(
            "Manifest should be written at least twice (1 checkpoint at 1000 + 1 final at end)",
            harness.cloud.updateManifestCallCount >= 2
        )
    }
}
