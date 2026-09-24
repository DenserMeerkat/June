package com.denser.june.core.sync.harness

import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.domain.sync.JournalSyncMeta
import com.denser.june.core.domain.sync.MediaSyncMeta
import com.denser.june.core.domain.sync.SyncAnalysis
import com.denser.june.core.domain.sync.SyncManager
import com.denser.june.core.domain.sync.SyncManifest
import com.denser.june.core.sync.fakes.FakeCloudProvider
import com.denser.june.core.sync.fakes.FakeJournalRepository
import com.denser.june.core.sync.fakes.FakeSongLibraryDao
import com.denser.june.core.sync.fakes.FakeSyncPreferences
import com.denser.june.core.sync.fakes.FakeSyncScheduler
import com.denser.june.core.sync.fixtures.SyncFixtures
import com.denser.june.core.sync.fixtures.SyncFixtures.T0
import com.denser.june.core.sync.fixtures.SyncFixtures.T1
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.io.File
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalCoroutinesApi::class)
class SyncTestHarness {
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
    lateinit var cloud: FakeCloudProvider
    lateinit var repo: FakeJournalRepository
    lateinit var prefs: FakeSyncPreferences
    lateinit var songDao: FakeSongLibraryDao
    lateinit var mediaDir: File
    lateinit var songMediaDir: File
    lateinit var syncManager: SyncManager

    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        cloud = FakeCloudProvider()
        repo = FakeJournalRepository()
        prefs = FakeSyncPreferences()
        songDao = FakeSongLibraryDao()
        mediaDir = createTempDirectory("june_test_media").toFile()
        songMediaDir = createTempDirectory("june_test_song_media").toFile()

        syncManager = SyncManager(
            journalRepo = repo,
            syncPrefs = prefs,
            providers = mapOf("FakeCloud" to cloud),
            mediaDir = mediaDir,
            syncScheduler = FakeSyncScheduler(),
            applicationScope = CoroutineScope(testDispatcher),
            songLibraryDao = songDao,
            songMediaDir = songMediaDir
        )
    }

    fun tearDown() {
        Dispatchers.resetMain()
        mediaDir.deleteRecursively()
        songMediaDir.deleteRecursively()
    }

    // =========================================================================
    // GIVEN (State Setup)
    // =========================================================================

    fun givenSyncedJournal(
        id: String,
        title: String = "Title $id",
        content: String = "Content $id",
        images: List<String> = emptyList(),
        songDetails: SongDetails? = null,
        updatedAt: Long = T1,
        syncedAt: Long = updatedAt,
        rev: Int = 1
    ): Journal {
        val journal = SyncFixtures.syncedJournal(
            id = id,
            title = title,
            syncedAt = syncedAt,
            updatedAt = updatedAt,
            images = images
        ).copy(content = content, songDetails = songDetails)

        repo.seed(journal)
        cloud.putJournal(journal, modifiedAt = syncedAt)

        val currentManifest = cloud.manifest ?: SyncFixtures.manifest(totalJournals = 0)
        val updatedJournalMeta = currentManifest.journalMetadata.toMutableMap()
        updatedJournalMeta[id] = JournalSyncMeta(rev = rev, contentHash = journal.computeContentHash())
        cloud.manifest = currentManifest.copy(
            totalJournals = updatedJournalMeta.size,
            journalMetadata = updatedJournalMeta
        )
        return journal
    }

    fun givenLocalOnlyJournal(
        id: String,
        title: String = "Local $id",
        content: String = "Content $id",
        images: List<String> = emptyList(),
        songDetails: SongDetails? = null,
        updatedAt: Long = T0
    ): Journal {
        val journal = SyncFixtures.newJournal(
            id = id,
            title = title,
            content = content,
            images = images,
            updatedAt = updatedAt
        ).copy(songDetails = songDetails)
        repo.seed(journal)
        return journal
    }

    fun givenCloudOnlyJournal(
        id: String,
        title: String = "Cloud $id",
        content: String = "Content $id",
        images: List<String> = emptyList(),
        songDetails: SongDetails? = null,
        modifiedAt: Long = T1,
        rev: Int = 1
    ): Journal {
        val journal = SyncFixtures.syncedJournal(
            id = id,
            title = title,
            syncedAt = modifiedAt,
            updatedAt = modifiedAt,
            images = images
        ).copy(content = content, songDetails = songDetails)

        cloud.putJournal(journal, modifiedAt = modifiedAt)
        val currentManifest = cloud.manifest ?: SyncFixtures.manifest(totalJournals = 0)
        val updatedJournalMeta = currentManifest.journalMetadata.toMutableMap()
        updatedJournalMeta[id] = JournalSyncMeta(rev = rev, contentHash = journal.computeContentHash())
        cloud.manifest = currentManifest.copy(
            totalJournals = updatedJournalMeta.size,
            journalMetadata = updatedJournalMeta
        )
        return journal
    }

    fun givenMediaFileOnDisk(name: String, bytes: ByteArray = byteArrayOf(1, 2, 3)): File {
        return File(mediaDir, name).apply {
            parentFile?.mkdirs()
            writeBytes(bytes)
        }
    }

    fun givenCloudMedia(
        journalId: String,
        name: String,
        bytes: ByteArray = byteArrayOf(1, 2, 3)
    ) {
        cloud.putMedia(journalId, name, bytes)
        val currentManifest = cloud.manifest ?: SyncFixtures.manifest(totalJournals = 0)
        val updatedMediaMeta = currentManifest.mediaMetadata.toMutableMap()
        val tempFile = File(mediaDir, "temp_hash_$name").apply { writeBytes(bytes) }
        updatedMediaMeta[name] = MediaSyncMeta(size = bytes.size.toLong(), hash = com.denser.june.core.utils.FileUtils.computeSHA256(tempFile))
        tempFile.delete()
        cloud.manifest = currentManifest.copy(
            totalMedia = updatedMediaMeta.size,
            mediaMetadata = updatedMediaMeta
        )
    }

    suspend fun givenSongInLibrary(
        id: String,
        filename: String,
        artFilename: String? = null,
        bytes: ByteArray = byteArrayOf(10, 20, 30),
        addedAt: Long = System.currentTimeMillis()
    ): File {
        val libraryDir = File(songMediaDir, "library").apply { if (!exists()) mkdirs() }
        val audioFile = File(libraryDir, filename).apply { writeBytes(bytes) }
        val artFile = artFilename?.let {
            val artDir = File(songMediaDir, "art").apply { if (!exists()) mkdirs() }
            File(artDir, it).apply { writeBytes(byteArrayOf(1, 2)) }
        }
        songDao.upsert(
            com.denser.june.core.data.database.song.SongLibraryEntity(
                contentHash = id,
                title = "Title $id",
                artistName = "Artist $id",
                localPath = audioFile.absolutePath,
                localArtPath = artFile?.absolutePath,
                addedAt = addedAt
            )
        )
        return audioFile
    }

    fun givenCloudSongMedia(name: String, bytes: ByteArray = byteArrayOf(10, 20, 30)) {
        cloud.putSongMedia(name, bytes)
        val currentManifest = cloud.manifest ?: SyncFixtures.manifest(totalJournals = 0)
        val updatedSongMeta = currentManifest.songMediaMetadata.toMutableMap()
        val tempFile = File(mediaDir, "temp_song_$name").apply { writeBytes(bytes) }
        updatedSongMeta[name] = MediaSyncMeta(size = bytes.size.toLong(), hash = com.denser.june.core.utils.FileUtils.computeSHA256(tempFile))
        tempFile.delete()
        cloud.manifest = currentManifest.copy(
            totalSongMedia = updatedSongMeta.size,
            songMediaMetadata = updatedSongMeta
        )
    }

    // =========================================================================
    // WHEN (Simulated User & Remote Actions)
    // =========================================================================

    suspend fun userEditsJournalLocally(id: String, transform: (Journal) -> Journal): Journal {
        val original = repo.db[id] ?: error("Journal $id does not exist locally")
        val updated = transform(original)
        repo.updateJournal(updated)
        return updated
    }

    suspend fun userSoftDeletesJournal(id: String) {
        repo.softDeleteJournal(id)
    }

    suspend fun userPermanentlyDeletesJournal(id: String) {
        repo.hardDeleteJournal(id)
    }

    suspend fun userEmptiesBin() {
        repo.emptyBin()
    }

    suspend fun userRestoresJournal(id: String, restoredAt: Long = System.currentTimeMillis()) {
        val original = repo.db[id] ?: error("Journal $id not found in DB")
        repo.updateJournal(original.copy(deletedAt = null, updatedAt = restoredAt))
    }

    fun remoteJournalUpdated(id: String, modifiedAt: Long, transform: (Journal) -> Journal): Journal {
        val original = cloud.getJournal(id) ?: error("Journal $id not found on cloud")
        val updated = transform(original)
        cloud.putJournal(updated, modifiedAt = modifiedAt)
        val currentManifest = cloud.manifest ?: SyncFixtures.manifest(totalJournals = 1)
        val updatedMeta = currentManifest.journalMetadata.toMutableMap()
        val existingRev = updatedMeta[id]?.rev ?: 1
        updatedMeta[id] = JournalSyncMeta(rev = existingRev + 1, contentHash = updated.computeContentHash())
        cloud.manifest = currentManifest.copy(journalMetadata = updatedMeta)
        return updated
    }

    suspend fun sync(isFullRevalidation: Boolean = false): Result<Unit> {
        return syncManager.sync(isFullRevalidation)
    }

    suspend fun performAnalysis(): Result<SyncAnalysis> {
        return syncManager.performAnalysis()
    }

    // =========================================================================
    // THEN (Domain Assertions)
    // =========================================================================

    suspend fun assertLocalJournalActive(id: String) {
        val j = repo.getJournalById(id)
        assertNotNull("Journal $id must exist locally", j)
        assertNull("Journal $id must not be deleted", j?.deletedAt)
        assertTrue("Journal $id must appear in active journals flow", repo.getAllJournals().any { it.id == id })
    }

    fun assertLocalJournalSoftDeleted(id: String) {
        val j = repo.db[id]
        assertNotNull("Journal $id must exist in database", j)
        assertNotNull("Journal $id must have deletedAt timestamp", j?.deletedAt)
    }

    fun assertLocalJournalHardDeleted(id: String) {
        assertNull("Journal $id must be completely removed from DB", repo.db[id])
    }

    fun assertJournalSyncedLocally(id: String) {
        val j = repo.db[id]
        assertNotNull("Journal $id must exist locally", j)
        assertNotNull("Journal $id must have syncedAt timestamp", j?.syncedAt)
    }

    fun assertCloudHasJournal(id: String) {
        assertTrue("Cloud must have journal $id", cloud.allJournalIds().contains(id))
    }

    fun assertCloudJournalDeleted(id: String) {
        assertFalse("Cloud must not have journal $id", cloud.allJournalIds().contains(id))
    }

    fun assertManifestDeletedIdsContains(id: String) {
        assertTrue("Manifest deletedIds must contain $id", cloud.manifest?.deletedIds?.contains(id) == true)
    }

    fun assertMediaExistsOnDisk(filename: String) {
        val file = File(mediaDir, filename)
        assertTrue("Media file $filename must exist on disk", file.exists() && file.length() > 0L)
    }

    fun assertMediaExistsOnCloud(filename: String, journalId: String? = null) {
        val key = if (journalId != null) "$journalId/$filename" else filename
        assertTrue(
            "Media file $filename must exist on cloud",
            cloud.allMediaKeys().any { it.endsWith("/$filename", ignoreCase = true) || it.equals(key, ignoreCase = true) }
        )
    }
}
