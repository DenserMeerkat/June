package com.denser.june.core.sync

import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.domain.sync.SyncManifest
import com.denser.june.core.sync.fixtures.SyncFixtures
import com.denser.june.core.sync.fixtures.SyncFixtures.T1
import com.denser.june.core.sync.harness.BaseSyncTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SyncAudioTest : BaseSyncTest() {

    @Test
    fun `audio upload and manifest schema 5 metadata`() = runTest {
        val audioFile = harness.givenAudioFileOnDisk("audio_note_1.m4a", byteArrayOf(10, 20, 30, 40))
        val audio = JournalAudio(
            id = "aud-1",
            path = audioFile.absolutePath,
            durationMs = 5000L,
            title = "Test Voice Note"
        )
        val j1 = SyncFixtures.newJournal("j1", updatedAt = T1).copy(
            audios = listOf(audio)
        )
        harness.repo.seed(j1)

        val result = harness.sync()

        assertTrue(result.isSuccess)
        assertTrue(harness.cloud.allAudioKeys().contains("audio_note_1.m4a"))
        assertEquals(5, harness.cloud.manifest?.schemaVersion)
        assertEquals(1, harness.cloud.manifest?.totalAudio)
        assertNotNull(harness.cloud.manifest?.audioMetadata?.get("audio_note_1.m4a"))
    }

    @Test
    fun `audio analysis counts pending uploads`() = runTest {
        val audioFile = harness.givenAudioFileOnDisk("audio_note_2.m4a", byteArrayOf(1, 2, 3))
        val audio = JournalAudio(
            id = "aud-2",
            path = audioFile.absolutePath,
            durationMs = 3000L,
            title = "Quick Note"
        )
        val j2 = SyncFixtures.newJournal("j2", updatedAt = T1).copy(
            audios = listOf(audio)
        )
        harness.repo.seed(j2)

        val analysis = harness.performAnalysis().getOrThrow()

        assertEquals(1, analysis.localAudioFiles)
        assertEquals(1, analysis.pendingAudioUploadsCount)
        assertEquals(0, analysis.remoteAudioFiles)
    }

    @Test
    fun `audio download restores local audio file from cloud`() = runTest {
        val audioBytes = byteArrayOf(99, 88, 77)
        harness.cloud.putAudio("remote_audio.m4a", audioBytes)

        val remoteAudio = JournalAudio(
            id = "aud-3",
            path = "remote_audio.m4a",
            durationMs = 8000L,
            title = "Cloud Voice Note"
        )
        val cloudJournal = SyncFixtures.newJournal("j3", updatedAt = T1).copy(
            audios = listOf(remoteAudio)
        )
        harness.cloud.putJournal(cloudJournal, modifiedAt = T1)
        harness.cloud.manifest = SyncFixtures.manifest(
            schemaVersion = 5,
            totalJournals = 1,
            totalAudio = 1,
            audioMetadata = mapOf(
                "remote_audio.m4a" to com.denser.june.core.domain.sync.MediaSyncMeta(size = 3L)
            )
        )

        val result = harness.sync(isFullRevalidation = true)

        assertTrue(result.isSuccess)
        val localDownloadedFile = File(harness.audioDir, "remote_audio.m4a")
        assertTrue(localDownloadedFile.exists())
        assertEquals(3L, localDownloadedFile.length())
    }

    @Test
    fun `audio upload failure aborts manifest commit`() = runTest {
        val audioFile = harness.givenAudioFileOnDisk("fail_audio.m4a", byteArrayOf(1, 2, 3))
        val audio = JournalAudio(
            id = "aud-fail",
            path = audioFile.absolutePath,
            durationMs = 2000L,
            title = "Failing Note"
        )
        val j = SyncFixtures.newJournal("jFail", updatedAt = T1).copy(
            audios = listOf(audio)
        )
        harness.repo.seed(j)
        harness.cloud.failNextUploadAudio = true

        val result = harness.sync()

        // Sync should report failure and NOT commit audio to manifest
        assertTrue(result.isFailure)
        assertEquals(null, harness.cloud.manifest)
    }
}

