package com.denser.june.core.sync

import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.domain.sync.SyncManifest
import com.denser.june.core.sync.fixtures.SyncFixtures
import com.denser.june.core.sync.fixtures.SyncFixtures.T1
import com.denser.june.core.sync.harness.BaseSyncTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SyncRepairTest : BaseSyncTest() {

    @Test
    fun `case-insensitive orphan cleanup preserves files differing only by casing`() = runTest {
        val mediaFile = File(harness.mediaDir, "PHOTO_A.JPG").apply {
            writeBytes(byteArrayOf(1, 2, 3))
        }
        val j = SyncFixtures.newJournal("j1", updatedAt = T1).copy(
            images = listOf(mediaFile.absolutePath)
        )
        harness.repo.seed(j)
        harness.cloud.putMedia("j1", "photo_a.jpg", byteArrayOf(1, 2, 3))
        harness.cloud.putJournal(j, modifiedAt = T1)

        val result = harness.sync(isFullRevalidation = true)

        assertTrue(result.isSuccess)
        // File must NOT be purged because case matches case-insensitively
        assertTrue(harness.cloud.allMediaKeys().any { it.contains("photo_a.jpg", ignoreCase = true) })
    }

    @Test
    fun `rebuild remote manifest regenerates schema v5 manifest`() = runTest {
        val j = SyncFixtures.newJournal("j3", updatedAt = T1)
        harness.repo.seed(j)
        harness.cloud.putJournal(j, modifiedAt = T1)
        harness.givenAudioFileOnDisk("rec_note.m4a", byteArrayOf(5, 5))
        harness.cloud.putAudio("rec_note.m4a", byteArrayOf(5, 5))
        harness.cloud.manifest = null // Simulate missing / damaged manifest

        val rebuildResult = harness.syncManager.rebuildRemoteManifest()

        assertTrue(rebuildResult.isSuccess)
        assertNotNull(harness.cloud.manifest)
        assertEquals(SyncManifest.CURRENT_SCHEMA_VERSION, harness.cloud.manifest?.schemaVersion)
        assertEquals(1, harness.cloud.manifest?.totalJournals)
        assertEquals(1, harness.cloud.manifest?.totalAudio)
    }

    @Test
    fun `full revalidation sync cleans up orphaned cloud files`() = runTest {
        harness.cloud.putMedia("j-old", "orphan_to_delete.jpg", byteArrayOf(1))
        harness.cloud.putAudio("orphan_rec.m4a", byteArrayOf(2))

        val result = harness.sync(isFullRevalidation = true)

        assertTrue(result.isSuccess)
        assertFalse(harness.cloud.allMediaKeys().any { it.contains("orphan_to_delete.jpg") })
        assertFalse(harness.cloud.allAudioKeys().any { it.contains("orphan_rec.m4a") })
    }
}

