package com.denser.june.core.sync

import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.sync.fixtures.SyncFixtures
import com.denser.june.core.sync.fixtures.SyncFixtures.T1
import com.denser.june.core.sync.fixtures.SyncFixtures.T2
import com.denser.june.core.sync.harness.BaseSyncTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SyncAnalysisTest : BaseSyncTest() {

    @Test
    fun `analysis localMedia counts only files that physically exist on disk`() = runTest {
        val realFile = harness.givenMediaFileOnDisk("real.jpg", byteArrayOf(1, 2, 3))
        // j1 references one real file and one phantom (non-existent) file
        val j1 = SyncFixtures.newJournal(
            "j1",
            updatedAt = T1,
            images = listOf(realFile.absolutePath, "${harness.mediaDir.absolutePath}/phantom.jpg")
        )
        harness.repo.seed(j1)
        harness.cloud.putJournal(j1, modifiedAt = T1)

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals("Only 1 file physically exists — phantom must not be counted", 1, analysis.localMedia)
    }

    @Test
    fun `analysis localMedia excludes media from soft-deleted journals`() = runTest {
        val mediaFile = harness.givenMediaFileOnDisk("deleted_journal_img.jpg", byteArrayOf(1))
        val active = SyncFixtures.newJournal("active", updatedAt = T1)
        val deleted = SyncFixtures.newJournal("deleted", updatedAt = T1, images = listOf(mediaFile.absolutePath))
            .copy(deletedAt = T1)
        harness.repo.seed(active)
        harness.repo.seed(deleted)

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals("Deleted journal's media must not inflate localMedia count", 0, analysis.localMedia)
    }

    @Test
    fun `analysis remoteSongFiles uses live listing not stale manifest total`() = runTest {
        harness.cloud.manifest = SyncFixtures.manifest(
            totalSongMedia = 99 // stale manifest claims 99 songs
        )
        // Live cloud storage actually has 2 audio songs and 1 album art image
        harness.cloud.putSongMedia("songA.mp3", byteArrayOf(1))
        harness.cloud.putSongMedia("songB.mp3", byteArrayOf(2))
        harness.cloud.putSongMedia("album_art.jpg", byteArrayOf(3))

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals("remoteSongFiles must reflect audio song count (2), excluding album art", 2, analysis.remoteSongFiles)
    }

    @Test
    fun `analysis pendingSongDownloads includes unreferenced songs from cloud song pool`() = runTest {
        // Cloud has 2 songs in the cloud song library pool not attached to any active local journal
        harness.cloud.putSongMedia("library_song1.mp3", byteArrayOf(1, 2, 3))
        harness.cloud.putSongMedia("library_song2.mp3", byteArrayOf(4, 5, 6))

        val active = SyncFixtures.newJournal("active", updatedAt = T1)
        harness.repo.seed(active)

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals("Cloud songs in song pool must be counted in pendingSongDownloads", 2, analysis.pendingSongDownloadsCount)
        assertEquals(2, analysis.pendingSongDownloadsList.size)
    }

    @Test
    fun `analysis pendingSongDownloads includes songs referenced by active journals when missing locally`() = runTest {
        harness.cloud.putSongMedia("attached.mp3", byteArrayOf(10, 20, 30))

        val activeWithSong = SyncFixtures.newJournal("active_with_song", updatedAt = T1).copy(
            songDetails = SongDetails(
                title = "Attached Track",
                artistName = "Attached Artist",
                localPreviewPath = "${harness.songMediaDir.absolutePath}/library/attached.mp3"
            )
        )
        harness.repo.seed(activeWithSong)

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals("Attached song on cloud missing locally must be in pendingSongDownloads", 1, analysis.pendingSongDownloadsCount)
        assertEquals("Attached Track — Attached Artist", analysis.pendingSongDownloadsList.first())
    }

    @Test
    fun `analysis pendingMediaUploads maps media filenames to parent journal title and extension`() = runTest {
        val mediaFile = harness.givenMediaFileOnDisk("photo1.jpg", byteArrayOf(1, 2, 3))
        val journal = SyncFixtures.newJournal(
            "j_vacation",
            title = "Trip to Tokyo",
            updatedAt = T1,
            images = listOf(mediaFile.absolutePath)
        )
        harness.repo.seed(journal)
        // Cloud already has the journal synced, but missing the media file
        harness.cloud.putJournal(journal, modifiedAt = T1)

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals(1, analysis.pendingMediaUploadsCount)
        assertEquals("Trip to Tokyo • JPG", analysis.pendingMediaUploadsList.first())
    }

    @Test
    fun `analysis localDeletions maps deleted journal IDs to journal title`() = runTest {
        val journal = SyncFixtures.newJournal(
            "j_deleted",
            title = "Old Thoughts",
            updatedAt = T1
        )
        harness.repo.seed(journal)
        harness.repo.tombstones.add("j_deleted")
        // Cloud still has the journal, so it will be recognized as pending local deletion from cloud
        harness.cloud.putJournal(journal, modifiedAt = T1)

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals(1, analysis.pendingDeletionsCount)
        assertEquals("Old Thoughts", analysis.localDeletionsList.first())
    }

    @Test
    fun `analysis pendingSongDownloads includes cloud song when cloud journal is pending download`() = runTest {
        harness.cloud.putSongMedia("incoming_track.mp3", byteArrayOf(1, 2, 3))
        harness.givenCloudOnlyJournal(
            "j_remote",
            title = "Distant Memory",
            songDetails = SongDetails(
                title = "Incoming Track",
                artistName = "Incoming Artist",
                localPreviewPath = "incoming_track.mp3"
            ),
            modifiedAt = T1
        )

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals("Pending cloud journal must trigger 1 journal download", 1, analysis.pendingDownloadsCount)
        assertEquals("Journal title must be peeked from remote journal JSON", "Distant Memory", analysis.pendingDownloadsList.first())
        assertEquals("Incoming cloud song must be counted in pendingSongDownloads", 1, analysis.pendingSongDownloadsCount)
        assertEquals("Song title and artist must be peeked from remote journal JSON", "Incoming Track — Incoming Artist", analysis.pendingSongDownloadsList.first())
    }

    @Test
    fun `analysis pendingSongDownloads resolves song name and artist even when localPreviewPath is null in journal`() = runTest {
        harness.cloud.putSongMedia("uuid_song_123.mp3", byteArrayOf(1, 2, 3))
        harness.givenCloudOnlyJournal(
            "j_legacy_remote",
            title = "Legacy Memory",
            songDetails = SongDetails(
                title = "Bohemian Rhapsody",
                artistName = "Queen",
                localPreviewPath = null,
                localThumbnailPath = "uuid_song_123.jpg"
            ),
            modifiedAt = T1
        )

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals("Cloud song must be counted in pendingSongDownloads", 1, analysis.pendingSongDownloadsCount)
        assertEquals("Song title and artist must be resolved from journal songDetails", "Bohemian Rhapsody — Queen", analysis.pendingSongDownloadsList.first())
    }

    @Test
    fun `analysis pendingUploads includes predicted conflict copies with Conflict Copy label`() = runTest {
        val baseJournal = harness.givenSyncedJournal("j_conflict", title = "Project Plan", content = "Initial plan", rev = 1)
        harness.repo.seed(baseJournal.copy(content = "My local edits", updatedAt = T2))

        val remoteJournal = baseJournal.copy(content = "Remote colleague edits", updatedAt = T2)
        harness.cloud.putJournal(remoteJournal, modifiedAt = T2)
        val manifest = harness.cloud.manifest!!
        harness.cloud.manifest = manifest.copy(
            journalMetadata = manifest.journalMetadata + ("j_conflict" to com.denser.june.core.domain.sync.JournalSyncMeta(rev = 2, contentHash = remoteJournal.computeContentHash()))
        )

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals("Conflict copy must be counted in pendingUploadsCount", 1, analysis.pendingUploadsCount)
        assertEquals(1, analysis.pendingUploadsList.size)
        assertEquals("Project Plan (Conflict Copy)", analysis.pendingUploadsList.first())
        assertEquals("Remote journal must be counted in pendingDownloadsCount", 1, analysis.pendingDownloadsCount)
        assertEquals("Project Plan", analysis.pendingDownloadsList.first())
    }

    @Test
    fun `analysis pendingSongUploads counts song with only un-synced album art as 1 and lists as Album Art`() = runTest {
        // Song audio already on cloud
        harness.givenCloudSongMedia("synced_audio.mp3", byteArrayOf(1, 2, 3))
        harness.givenSongInLibrary("s1", "synced_audio.mp3", artFilename = "cover.jpg", bytes = byteArrayOf(1, 2, 3))

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals("Song with only album art missing on cloud must contribute 1 to count", 1, analysis.pendingSongUploadsCount)
        assertEquals(1, analysis.pendingSongUploadsList.size)
        assertEquals("Title s1 — Artist s1 (Album Art)", analysis.pendingSongUploadsList.first())
    }

    @Test
    fun `analysis pendingSongUploads counts song with both audio and art as 1 song and lists both separately`() = runTest {
        harness.givenSongInLibrary("s2", "new_audio.mp3", artFilename = "new_art.jpg")

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals("Song with both audio and art must contribute 1 to table count", 1, analysis.pendingSongUploadsCount)
        assertEquals(2, analysis.pendingSongUploadsList.size)
        assertTrue(analysis.pendingSongUploadsList.contains("Title s2 — Artist s2"))
        assertTrue(analysis.pendingSongUploadsList.contains("Title s2 — Artist s2 (Album Art)"))
    }

    @Test
    fun `analysis pendingDownloadsList resolves titles for multiple simultaneous conflicts`() = runTest {
        for (i in 1..3) {
            val base = harness.givenSyncedJournal("j$i", title = "Journal $i", content = "v1", rev = 1)
            harness.repo.seed(base.copy(content = "local v2", updatedAt = T2))
            val remote = base.copy(content = "remote v2", updatedAt = T2)
            harness.cloud.putJournal(remote, modifiedAt = T2)
            val m = harness.cloud.manifest!!
            harness.cloud.manifest = m.copy(
                journalMetadata = m.journalMetadata + ("j$i" to com.denser.june.core.domain.sync.JournalSyncMeta(rev = 2, contentHash = remote.computeContentHash()))
            )
        }

        val analysis = harness.performAnalysis().getOrThrow()
        assertEquals("3 conflicts must count as 3 pending downloads", 3, analysis.pendingDownloadsCount)
        assertEquals(listOf("Journal 1", "Journal 2", "Journal 3"), analysis.pendingDownloadsList.sorted())
    }
}
