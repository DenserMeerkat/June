package com.denser.june.core.sync

import com.denser.june.core.sync.fixtures.SyncFixtures
import com.denser.june.core.sync.fixtures.SyncFixtures.T1
import com.denser.june.core.sync.harness.BaseSyncTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}
