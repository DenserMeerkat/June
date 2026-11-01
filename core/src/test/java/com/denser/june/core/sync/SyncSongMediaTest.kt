package com.denser.june.core.sync

import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.domain.sync.SyncStatus
import com.denser.june.core.sync.fixtures.SyncFixtures
import com.denser.june.core.sync.fixtures.SyncFixtures.T1
import com.denser.june.core.sync.harness.BaseSyncTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SyncSongMediaTest : BaseSyncTest() {

    @Test
    fun `song media upload and download passes succeed`() = runTest {
        val testAudio = harness.givenSongInLibrary("s1", "hash123.mp3", bytes = byteArrayOf(1, 2, 3, 4))
        val j1 = SyncFixtures.newJournal("j1", updatedAt = T1).copy(
            songDetails = SongDetails(
                title = "Test Song",
                artistName = "Test Artist",
                localPreviewPath = testAudio.absolutePath
            )
        )
        harness.repo.seed(j1)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertTrue("Song media was uploaded to cloud", harness.cloud.allSongMediaKeys().contains("hash123.mp3"))
        assertEquals("Manifest totalSongMedia is 1", 1, harness.cloud.manifest?.totalSongMedia)
        assertNotNull("Manifest songMediaMetadata contains hash123.mp3", harness.cloud.manifest?.songMediaMetadata?.get("hash123.mp3"))
    }

    @Test
    fun `sync with journal media and song completes successfully with consistent manifest`() = runTest {
        val imgFile = harness.givenMediaFileOnDisk("img1.jpg", byteArrayOf(1, 2))
        val audioFile = harness.givenSongInLibrary("s1", "song1.mp3", bytes = byteArrayOf(3, 4))

        val j1 = SyncFixtures.newJournal("j1", updatedAt = T1, images = listOf(imgFile.absolutePath)).copy(
            songDetails = SongDetails(
                title = "Song One",
                artistName = "Artist One",
                localPreviewPath = audioFile.absolutePath
            )
        )
        harness.repo.seed(j1)

        val result = harness.sync()

        assertTrue("Sync must succeed", result.isSuccess)
        assertEquals("Manifest totalJournals must be 1", 1, harness.cloud.manifest?.totalJournals)
        assertEquals("Manifest totalMedia must be 1", 1, harness.cloud.manifest?.totalMedia)
        assertEquals("Manifest totalSongMedia must be 1", 1, harness.cloud.manifest?.totalSongMedia)
    }

    @Test
    fun `library song without journal reference is uploaded to cloud`() = runTest {
        harness.givenSongInLibrary("orphan_song", "library_only.mp3", bytes = byteArrayOf(1, 2, 3))
        harness.givenSyncedJournal("j1", updatedAt = T1)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertTrue("Library song uploaded even without journal reference", harness.cloud.allSongMediaKeys().contains("library_only.mp3"))
    }

    @Test
    fun `downloaded journal song registers in SongLibraryDao`() = runTest {
        harness.cloud.putSongMedia("song_dl.mp3", byteArrayOf(10, 20, 30))
        harness.givenCloudOnlyJournal(
            "j1",
            songDetails = SongDetails(
                title = "Remote Track",
                artistName = "Remote Artist",
                localPreviewPath = "song_dl.mp3"
            ),
            modifiedAt = T1
        )

        val result = harness.sync()

        assertTrue(result.isSuccess)
        val allSongs = harness.songDao.getAll()
        val registeredSong = allSongs.find { it.title == "Remote Track" }
        assertNotNull("Song must be inserted into SongLibraryDao", registeredSong)
        assertEquals("Remote Artist", registeredSong?.artistName)
    }

    @Test
    fun `song art file is uploaded alongside audio`() = runTest {
        harness.givenSongInLibrary("song_with_art", "track.mp3", artFilename = "art.jpg", bytes = byteArrayOf(1, 2, 3))
        harness.givenSyncedJournal("j1", updatedAt = T1)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertTrue("Song art file uploaded to flat song pool", harness.cloud.allSongMediaKeys().contains("art.jpg"))
    }

    @Test
    fun `null song localPreviewPath does not crash sync`() = runTest {
        val j = SyncFixtures.newJournal("j1", updatedAt = T1).copy(
            songDetails = SongDetails(
                title = "Stream Only",
                artistName = "Stream Artist",
                localPreviewPath = null,
                localThumbnailPath = null
            )
        )
        harness.repo.seed(j)

        val result = harness.sync()

        assertTrue("Sync succeeds without audio file", result.isSuccess)
        harness.assertCloudHasJournal("j1")
    }

    @Test
    fun `adding song after sync updates status to Dirty`() = runTest(harness.testDispatcher) {
        // Initial state: synced
        val result = harness.sync()
        assertTrue(result.isSuccess)
        testScheduler.advanceUntilIdle()
        assertEquals(SyncStatus.Success, harness.syncManager.status.value)

        // User imports a new song into the library after last sync
        harness.givenSongInLibrary(
            id = "new_song",
            filename = "new_track.mp3",
            bytes = byteArrayOf(1, 2, 3),
            addedAt = System.currentTimeMillis() + 10_000L
        )
        testScheduler.advanceUntilIdle()

        // Verify status becomes Dirty
        val status = harness.syncManager.status.value
        assertEquals("Status must become Dirty after new song added to library", SyncStatus.Dirty, status)
    }

    @Test
    fun `downloaded journal song updates journal record with localPreviewPath`() = runTest {
        harness.cloud.putSongMedia("song_sync.mp3", byteArrayOf(1, 2, 3))
        harness.givenCloudOnlyJournal(
            "j1",
            songDetails = SongDetails(
                title = "Cloud Track",
                artistName = "Cloud Artist",
                localPreviewPath = "song_sync.mp3"
            ),
            modifiedAt = T1
        )

        val result = harness.sync()

        assertTrue(result.isSuccess)
        val journal = harness.repo.getJournalById("j1")
        assertNotNull(journal?.songDetails?.localPreviewPath)
        assertTrue(
            "Journal localPreviewPath must point to existing downloaded file",
            File(journal!!.songDetails!!.localPreviewPath!!).exists()
        )
    }

    @Test
    fun `journal with null localPreviewPath matching library song downloads from cloud`() = runTest {
        harness.cloud.putSongMedia("matched_song.mp3", byteArrayOf(4, 5, 6))
        // Song is in local library but journal on another device had localPreviewPath = null
        harness.songDao.upsert(
            com.denser.june.core.data.database.song.SongLibraryEntity(
                contentHash = "matched_song",
                localPath = File(harness.songMediaDir, "library/matched_song.mp3").absolutePath,
                title = "Matched Song",
                artistName = "Matched Artist"
            )
        )
        harness.givenCloudOnlyJournal(
            "j1",
            songDetails = SongDetails(
                title = "Matched Song",
                artistName = "Matched Artist",
                localPreviewPath = null
            ),
            modifiedAt = T1
        )

        val result = harness.sync()

        assertTrue(result.isSuccess)
        val journal = harness.repo.getJournalById("j1")
        assertNotNull("Journal should have its localPreviewPath resolved", journal?.songDetails?.localPreviewPath)
        assertTrue(
            "Downloaded song exists on disk",
            File(journal!!.songDetails!!.localPreviewPath!!).exists()
        )
    }

    @Test
    fun `journal attached song without library entry is uploaded to cloud`() = runTest {
        val songFile = File(harness.songMediaDir, "library/uncataloged.mp3")
        songFile.parentFile?.mkdirs()
        songFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))

        val j1 = SyncFixtures.newJournal("j1", updatedAt = T1).copy(
            songDetails = SongDetails(
                title = "Linked Song",
                artistName = "Web Artist",
                localPreviewPath = songFile.absolutePath
            )
        )
        harness.repo.seed(j1)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertTrue(harness.cloud.allSongMediaKeys().contains("uncataloged.mp3"))
        assertNotNull(harness.cloud.manifest?.songMediaMetadata?.get("uncataloged.mp3"))
    }

    @Test
    fun `song upload failure does not fail sync and updates manifest`() = runTest {
        val testAudio = harness.givenSongInLibrary("s1", "failing_song.mp3", bytes = byteArrayOf(1, 2, 3))
        val j1 = SyncFixtures.newJournal("j1", updatedAt = T1).copy(
            songDetails = SongDetails(
                title = "Failing Track",
                artistName = "Artist",
                localPreviewPath = testAudio.absolutePath
            )
        )
        harness.repo.seed(j1)
        harness.cloud.failNextUploadSongMedia = true

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertNotNull(harness.cloud.manifest)
    }

    @Test
    fun `downloaded non-mp3 song registers in SongLibraryDao with correct hash`() = runTest {
        harness.cloud.putSongMedia("song_dl.m4a", byteArrayOf(10, 20, 30))
        harness.givenCloudOnlyJournal(
            "j1",
            songDetails = SongDetails(
                title = "M4A Track",
                artistName = "M4A Artist",
                localPreviewPath = "song_dl.m4a"
            ),
            modifiedAt = T1
        )

        val result = harness.sync()

        assertTrue(result.isSuccess)
        val entry = harness.songDao.getByHash("song_dl")
        assertNotNull(entry)
        assertEquals("M4A Track", entry?.title)
    }
}
