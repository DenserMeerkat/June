package com.denser.june.core.domain.sync

import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.preferences.SyncPreferences
import com.denser.june.core.domain.logging.AppLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import com.denser.june.core.data.database.journal.JournalDatabase
import com.denser.june.core.utils.computeSHA256
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import com.denser.june.core.data.database.song.SongLibraryDao
import com.denser.june.core.data.database.song.SongLibraryEntity
import com.denser.june.core.domain.model.SongSourceType

@OptIn(ExperimentalCoroutinesApi::class)
sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Preparing : SyncStatus()
    data class Syncing(
        val progress: Float = 0f,
        val uploadCount: Int = 0,
        val downloadCount: Int = 0,
        val totalOperations: Int = 0,
        val currentOperation: String = ""
    ) : SyncStatus()

    data object Success : SyncStatus()
    data object Dirty : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

data class SyncAnalysis(
    val localJournals: Int,
    val remoteJournals: Int,
    val localMedia: Int,
    val remoteMedia: Int,
    val localSongFiles: Int = 0,
    val remoteSongFiles: Int = 0,
    val pendingUploadsCount: Int,
    val pendingDownloadsCount: Int,
    val pendingMediaUploadsCount: Int,
    val pendingMediaDownloadsCount: Int,
    val pendingSongUploadsCount: Int = 0,
    val pendingSongDownloadsCount: Int = 0,
    val pendingDeletionsCount: Int,
    val pendingUploadsList: List<String> = emptyList(),
    val pendingDownloadsList: List<String> = emptyList(),
    val localDeletionsList: List<String> = emptyList(),
    val remoteDeletionsList: List<String> = emptyList(),
    val pendingMediaUploadsList: List<String> = emptyList(),
    val pendingMediaDownloadsList: List<String> = emptyList(),
    val pendingSongUploadsList: List<String> = emptyList(),
    val pendingSongDownloadsList: List<String> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SyncManager(
    private val journalRepo: JournalRepository,
    private val syncPrefs: SyncPreferences,
    private val providers: Map<String, CloudProvider>,
    private val mediaDir: File,
    private val syncScheduler: SyncScheduler,
    private val applicationScope: CoroutineScope,
    private val songLibraryDao: SongLibraryDao,
    private val songMediaDir: File
) {
    private val songMediaLibraryDir = File(songMediaDir, "library").apply { if (!exists()) mkdirs() }
    private val songMediaArtDir = File(songMediaDir, "art").apply { if (!exists()) mkdirs() }
    companion object {
        const val SYNC_THRESHOLD_MS = 2000L
        const val CURRENT_DATA_REPAIR_VERSION = 2
    }

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    private val syncMutex = Mutex()
    private val _syncActive = AtomicBoolean(false)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    init {
        applicationScope.launch {
            syncPrefs.getSyncLoggingEnabled().collect { enabled ->
                AppLogger.setCategoryEnabled(AppLogger.Category.SYNC, enabled)
            }
        }
        applicationScope.launch {
            syncPrefs.getBackupLoggingEnabled().collect { enabled ->
                AppLogger.setCategoryEnabled(AppLogger.Category.BACKUP, enabled)
            }
        }
        applicationScope.launch {
            syncPrefs.getDatabaseLoggingEnabled().collect { enabled ->
                AppLogger.setCategoryEnabled(AppLogger.Category.DATABASE, enabled)
            }
        }

        applicationScope.launch(Dispatchers.IO) {
            val lastVersion = syncPrefs.getLastCompletedDataRepairVersion().first()
            if (lastVersion < 1) {
                repairDoubleConcatenatedImages()
            }
            if (lastVersion < 2) {
                repairSongDetailsAndPaths()
            }
            syncPrefs.setLastCompletedDataRepairVersion(CURRENT_DATA_REPAIR_VERSION)
        }

        applicationScope.launch {
            syncPrefs.getSyncEnabled().flatMapLatest { isSyncEnabled ->
                if (!isSyncEnabled) kotlinx.coroutines.flow.flowOf(null)
                else {
                    combine(
                        journalRepo.observeHasUnsyncedJournals(SYNC_THRESHOLD_MS),
                        journalRepo.observeHasTombstones(),
                        songLibraryDao.observeAll(),
                        syncPrefs.getLastSyncTime()
                    ) { hasUnsynced, hasTombstones, songs, lastSyncTime ->
                        hasUnsynced || hasTombstones || songs.any { it.addedAt > (lastSyncTime + SYNC_THRESHOLD_MS) }
                    }
                }
            }.collect { isDirty ->
                val current = _status.value
                when {
                    isDirty == true && !_syncActive.get() && (current is SyncStatus.Idle || current is SyncStatus.Success) -> {
                        _status.value = SyncStatus.Dirty
                    }
                    isDirty == false && current is SyncStatus.Dirty -> {
                        _status.value = SyncStatus.Success
                    }
                    isDirty == null -> {
                        _status.value = SyncStatus.Idle
                    }
                }
            }
        }

        applicationScope.launch {
            combine(
                syncPrefs.getSyncEnabled(),
                syncPrefs.isAutomaticSyncEnabled()
            ) { enabled, auto -> enabled && auto }
                .flatMapLatest { autoSyncReady ->
                    if (!autoSyncReady) kotlinx.coroutines.flow.flowOf(false)
                    else {
                        combine(
                            journalRepo.observeHasUnsyncedJournals(SYNC_THRESHOLD_MS),
                            journalRepo.observeHasTombstones(),
                            songLibraryDao.observeAll(),
                            syncPrefs.getLastSyncTime()
                        ) { hasUnsynced, hasTombstones, songs, lastSyncTime ->
                            hasUnsynced || hasTombstones || songs.any { it.addedAt > (lastSyncTime + SYNC_THRESHOLD_MS) }
                        }.debounce(10000L)
                    }
                }.collect { shouldSync ->
                    if (shouldSync) {
                        val onlyWifi = syncPrefs.getSyncOnlyOnWifi().first()
                        syncScheduler.enqueue(onlyWifi)
                    }
                }
        }
    }

    private suspend fun repairDoubleConcatenatedImages() {
        try {
            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Starting programmatic startup image path repair...")
            val journals = journalRepo.getAllJournalsIncludeDeletedSync()
            var checkedCount = 0
            var repairedCount = 0
            journals.forEach { journal ->
                checkedCount++
                var modified = false
                val cleanedImages = journal.images.map { path ->
                    val file = File(path)
                    val name = file.name
                    val canonicalPath = file.absolutePath
                    val occurrences = canonicalPath.split("journal_media").size - 1
                    if (occurrences > 1) {
                        modified = true
                        File(mediaDir, name).absolutePath
                    } else {
                        path
                    }
                }
                if (modified) {
                    repairedCount++
                    journalRepo.insertJournal(journal.copy(images = cleanedImages))
                }
            }
            AppLogger.d(
                AppLogger.Category.SYNC,
                "SyncManager",
                "Startup image repair completed. Checked $checkedCount journals, repaired $repairedCount journals."
            )
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "Error repairing double-concatenated image paths", e)
        }
    }

    private suspend fun repairSongDetailsAndPaths() {
        try {
            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Starting programmatic startup song details and paths repair...")
            val journals = journalRepo.getAllJournalsIncludeDeletedSync()
            val librarySongs = songLibraryDao.getAll()
            var checkedCount = 0
            var repairedCount = 0

            journals.forEach { journal ->
                val song = journal.songDetails
                if (song != null) {
                    checkedCount++
                    var modified = false
                    var repairedAudioPath = song.localPreviewPath
                    var repairedArtPath = song.localThumbnailPath

                    val existingAudioFile = song.localPreviewPath?.let { p ->
                        val f = File(p)
                        if (f.exists() && f.length() > 0L) f else File(songMediaLibraryDir, f.name).takeIf { it.exists() && it.length() > 0L }
                    }

                    if (existingAudioFile != null) {
                        if (song.localPreviewPath != existingAudioFile.absolutePath) {
                            repairedAudioPath = existingAudioFile.absolutePath
                            modified = true
                        }
                    } else {
                        val match = librarySongs.firstOrNull {
                            it.title.trim().equals(song.title.trim(), ignoreCase = true) &&
                            it.artistName.trim().equals(song.artistName.trim(), ignoreCase = true)
                        }
                        if (match != null) {
                            val libFile = File(match.localPath).takeIf { it.exists() && it.length() > 0L }
                                ?: File(songMediaLibraryDir, File(match.localPath).name).takeIf { it.exists() && it.length() > 0L }
                            if (libFile != null) {
                                repairedAudioPath = libFile.absolutePath
                                if (repairedArtPath == null && match.localArtPath != null) {
                                    repairedArtPath = match.localArtPath
                                }
                                modified = true
                            }
                        }
                    }

                    val existingArtFile = repairedArtPath?.let { p ->
                        val f = File(p)
                        if (f.exists() && f.length() > 0L) f else File(songMediaArtDir, f.name).takeIf { it.exists() && it.length() > 0L }
                    }
                    if (existingArtFile != null && repairedArtPath != existingArtFile.absolutePath) {
                        repairedArtPath = existingArtFile.absolutePath
                        modified = true
                    }

                    if (modified) {
                        repairedCount++
                        val updatedSong = song.copy(
                            localPreviewPath = repairedAudioPath,
                            localThumbnailPath = repairedArtPath
                        )
                        journalRepo.insertJournal(journal.copy(songDetails = updatedSong, updatedAt = System.currentTimeMillis()))
                    }
                }
            }
            AppLogger.d(
                AppLogger.Category.SYNC,
                "SyncManager",
                "Startup song details repair completed. Checked $checkedCount journals, repaired $repairedCount journals."
            )
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "Error repairing song details and paths", e)
        }
    }

    fun resetStatus() {
        applicationScope.launch {
            val isSyncEnabled = syncPrefs.getSyncEnabled().first()
            if (!isSyncEnabled) {
                _status.value = SyncStatus.Idle
                return@launch
            }
            val hasUnsynced = journalRepo.hasUnsyncedJournals(SYNC_THRESHOLD_MS)
            val hasTombstones = journalRepo.hasTombstones()

            _status.value = if (hasUnsynced || hasTombstones) SyncStatus.Dirty else SyncStatus.Idle
        }
    }

    suspend fun performAnalysis(): Result<SyncAnalysis> = syncMutex.withLock {
        val isSyncEnabled = syncPrefs.getSyncEnabled().first()
        if (!isSyncEnabled) return@withLock Result.failure(Exception("Sync is disabled"))

        AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Starting sync analysis...")

        try {
            val provider = getActiveProvider()
            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Active provider: ${provider.name}")
            provider.connect().getOrThrow()

            val remoteManifest = provider.getManifest().getOrNull()
            val remoteJournalMeta = remoteManifest?.journalMetadata ?: emptyMap()

            val remoteJournals = provider.listJournals().getOrThrow()
            val remoteMedia = provider.listMedia().getOrThrow().toSet()

            val allLocalJournals = journalRepo.getAllJournalsIncludeDeletedSync()
            val tombstones = journalRepo.getAllTombstones()
            val tombstoneIds = tombstones.toSet()

            // For sync decisions: use all journals (including deleted) so we don't re-upload still-referenced media.
            val allReferencedMediaNames = allLocalJournals.flatMap { it.images }
                .map { File(it).name }.distinct()
            val localMediaFiles = allReferencedMediaNames.toSet()

            // For display (BUG-02): count only active journals' media that physically exist on disk.
            val activeLocalJournals = allLocalJournals.filter { it.deletedAt == null }
            val localMediaOnDisk = activeLocalJournals.flatMap { it.images }
                .map { File(it).name }.distinct()
                .count { name -> File(mediaDir, name).let { it.exists() && it.length() > 0L } }

            val remoteMediaMeta = remoteManifest?.mediaMetadata ?: emptyMap()
            val mediaToUpload = localMediaFiles.filter { name ->
                val file = File(mediaDir, name)
                val isPhysicallyOnCloud = remoteMedia.any { it.equals(name, ignoreCase = true) } || remoteMediaMeta.containsKey(name)
                file.exists() && file.length() > 0L && !isPhysicallyOnCloud
            }
            // "Media to download" = files referenced by active local journals that are on cloud
            // but missing or empty on this device's disk.
            //
            // Previously this was "cloud files not referenced locally", which is WRONG:
            //   - sync() only downloads files that local journals reference, so analysis showed
            //     persistent phantom counts for orphaned / other-device cloud files.
            //   - This definition now matches sync()'s actual download loop exactly.
            val mediaToDownload = activeLocalJournals
                .flatMap { it.images }
                .map { File(it).name }
                .distinct()
                .filter { name ->
                    val localFile = File(mediaDir, name)
                    val isOnCloud = remoteMedia.any { it.equals(name, ignoreCase = true) } || remoteMediaMeta.containsKey(name)
                    isOnCloud && (!localFile.exists() || localFile.length() == 0L)
                }

            val remoteStates = remoteJournals.associate { meta ->
                val id = meta.name.removeSuffix(".json")
                id to (meta.name to meta.lastModified)
            }

            val plan = buildSyncPlan(
                allLocalJournals = allLocalJournals,
                remoteStates = remoteStates,
                remoteJournalMeta = remoteJournalMeta,
                tombstoneIds = tombstoneIds,
                localsToSync = emptyList(),
                isFullRevalidation = true
            )

            val localDeletions = remoteStates.keys
                .filter { id -> id in tombstoneIds }
                .map { id -> allLocalJournals.find { it.id == id }?.title?.ifBlank { "Untitled" } ?: id }

            AppLogger.d(
                AppLogger.Category.SYNC,
                "SyncManager",
                "Analysis complete. Local active journals: ${allLocalJournals.count { it.deletedAt == null }}, " +
                    "remote files: ${remoteJournals.size}, pending uploads: ${plan.toUpload.size}, " +
                    "pending downloads: ${plan.toDownload.size}, " +
                    "media uploads: ${mediaToUpload.size}, media downloads: ${mediaToDownload.size}, " +
                    "local deletions: ${localDeletions.size}, tombstones: ${tombstones.size}"
            )

            val remoteSongMediaMeta = remoteManifest?.songMediaMetadata ?: emptyMap()
            val remoteSongMedia = provider.listSongMedia().getOrDefault(emptyList()).toSet()

            val allLocalSongs = songLibraryDao.getAll()
            val localSongFileNames = allLocalSongs.map { File(it.localPath).name }.toSet()

            val isAudioFile: (String) -> Boolean = { name ->
                name.endsWith(".mp3", ignoreCase = true) ||
                name.endsWith(".m4a", ignoreCase = true) ||
                name.endsWith(".wav", ignoreCase = true) ||
                name.endsWith(".ogg", ignoreCase = true) ||
                name.endsWith(".aac", ignoreCase = true) ||
                name.endsWith(".flac", ignoreCase = true)
            }

            val songsToUpload = allLocalSongs.filter { entry ->
                val fileName = File(entry.localPath).name
                val file = File(songMediaLibraryDir, fileName)
                val isPhysicallyOnCloud = remoteSongMedia.any { it.equals(fileName, ignoreCase = true) } || remoteSongMediaMeta.containsKey(fileName)
                file.exists() && file.length() > 0L && !isPhysicallyOnCloud
            }
            val songsToDownload = remoteSongMedia.filter { name ->
                isAudioFile(name) && localSongFileNames.none { it.equals(name, ignoreCase = true) }
            }
            val remoteSongAudioCount = remoteSongMedia.count { isAudioFile(it) }

            Result.success(
                SyncAnalysis(
                    localJournals = allLocalJournals.size,
                    remoteJournals = remoteJournals.size,
                    localMedia = localMediaOnDisk,             // BUG-02: physical files only
                    remoteMedia = remoteMedia.size,
                    localSongFiles = allLocalSongs.size,
                    remoteSongFiles = remoteSongAudioCount,   // Audio tracks only, excluding album artwork images
                    pendingUploadsCount = plan.toUpload.size,
                    pendingDownloadsCount = plan.toDownload.size,
                    pendingUploadsList = plan.toUpload.map {
                        val title = it.title.ifBlank { "Untitled" }
                        if (it.deletedAt != null) "[Bin] $title" else title
                    },
                    pendingDownloadsList = plan.toDownload.map { (id, _) -> id },
                    localDeletionsList = localDeletions,
                    remoteDeletionsList = emptyList(),
                    pendingMediaUploadsCount = mediaToUpload.size,
                    pendingMediaDownloadsCount = mediaToDownload.size,
                    pendingSongUploadsCount = songsToUpload.size,
                    pendingSongDownloadsCount = songsToDownload.size,
                    pendingSongUploadsList = songsToUpload.map { "${it.title} — ${it.artistName}" },
                    pendingSongDownloadsList = songsToDownload.toList(),
                    pendingDeletionsCount = tombstones.size,
                    pendingMediaUploadsList = mediaToUpload,
                    pendingMediaDownloadsList = mediaToDownload
                )
            )
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "Analysis failed", e)
            Result.failure(e)
        }
    }


    fun launchSync(isFullRevalidation: Boolean = false) {
        applicationScope.launch {
            val onlyWifi = syncPrefs.getSyncOnlyOnWifi().first()
            syncScheduler.enqueue(onlyWifi, immediate = true, isFullRevalidation = isFullRevalidation)
        }
    }

    fun cancelSync() {
        syncScheduler.cancel()
        _status.value = SyncStatus.Idle
        AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Sync cancelled by user.")
    }

    fun repairSync(context: android.content.Context) {
        applicationScope.launch(Dispatchers.IO) {
            try {
                AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Starting comprehensive sync repair...")
                repairDoubleConcatenatedImages()
                val allJournals = journalRepo.getAllJournalsIncludeDeletedSync()
                val activePaths = allJournals.flatMap { it.images } + allJournals.mapNotNull { it.songDetails?.localThumbnailPath }
                com.denser.june.core.utils.FileUtils.cleanOrphanedFiles(context, activePaths)
            } catch (e: Exception) {
                AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "Error during local data repair before sync", e)
            }
            val onlyWifi = syncPrefs.getSyncOnlyOnWifi().first()
            syncScheduler.enqueue(onlyWifi, immediate = true, isFullRevalidation = true)
        }
    }

    fun getAvailableProviders(): List<String> {
        val list = providers.keys.toList()
        return if (list.contains("GoogleDrive")) {
            listOf("GoogleDrive", "WebDAV").filter { list.contains(it) }
        } else {
            list
        }
    }

    fun isProviderConnected(type: String): kotlinx.coroutines.flow.Flow<Boolean> {
        return providers[type]?.isConnected() ?: kotlinx.coroutines.flow.flowOf(false)
    }

    private suspend fun getActiveProvider(): CloudProvider {
        val default = if (getAvailableProviders().contains("GoogleDrive")) "GoogleDrive" else "WebDAV"
        val selected = syncPrefs.getSelectedProvider().first() ?: default
        return providers[selected] ?: providers["WebDAV"]!!
    }

    suspend fun testProviderConnection(type: String): Result<Unit> {
        return providers[type]?.connect() ?: Result.failure(Exception("Provider NOT found"))
    }

    suspend fun sync(isFullRevalidation: Boolean = false): Result<Unit> = syncMutex.withLock {
        val isSyncEnabled = syncPrefs.getSyncEnabled().first()
        if (!isSyncEnabled) return@withLock Result.failure(Exception("Sync is disabled"))

        _syncActive.set(true)
        _status.value = SyncStatus.Preparing
        AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Starting sync. isFullRevalidation: $isFullRevalidation")

        try {
            val provider = getActiveProvider()
            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Active provider: ${provider.name}. Connecting...")
            provider.connect().getOrThrow()

            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Fetching remote manifest...")
            val remoteManifest = provider.getManifest().getOrNull()
            if (remoteManifest != null && remoteManifest.schemaVersion > SyncManifest.CURRENT_SCHEMA_VERSION) {
                throw Exception("A newer version of the app is required to sync with this cloud database.")
            }
            val remoteDeletedIds = remoteManifest?.deletedIds ?: emptyList()
            val remoteJournalMeta = remoteManifest?.journalMetadata ?: emptyMap()
            val remoteMediaMeta = remoteManifest?.mediaMetadata ?: emptyMap()
            val remoteSongMediaMeta = remoteManifest?.songMediaMetadata ?: emptyMap()
            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Remote manifest fetched (schema ${remoteManifest?.schemaVersion ?: 1}). Deleted IDs: ${remoteDeletedIds.size}, Metadata entries: ${remoteJournalMeta.size}")

            if (remoteDeletedIds.isNotEmpty()) {
                remoteDeletedIds.forEach { id ->
                    journalRepo.hardDeleteJournal(id)
                    journalRepo.deleteTombstone(id)
                }
                AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Processed ${remoteDeletedIds.size} remote deletions locally.")
            }

            val remoteMetaList = provider.listJournals().getOrThrow()
            val expectedRemoteTotal = remoteManifest?.totalJournals ?: -1
            val isListingTruncated = expectedRemoteTotal > 0 && remoteMetaList.size < (expectedRemoteTotal * 0.8).toInt()

            if (isListingTruncated) {
                AppLogger.w(
                    AppLogger.Category.SYNC,
                    "SyncManager",
                    "PROPFIND listing count (${remoteMetaList.size}) is significantly lower than manifest count ($expectedRemoteTotal). Skipping remote deletion checks."
                )
            }

            val hasUnsynced = journalRepo.hasUnsyncedJournals(SYNC_THRESHOLD_MS)
            val hasTombstones = journalRepo.hasTombstones()

            val localsToSync = if (hasUnsynced || hasTombstones || isFullRevalidation) {
                journalRepo.getJournalsToSync(SYNC_THRESHOLD_MS)
            } else {
                emptyList()
            }

            val remoteStates = remoteMetaList.associate { meta ->
                val id = meta.name.removeSuffix(".json")
                id to (meta.name to meta.lastModified)
            }

            val allLocalJournals = journalRepo.getAllJournalsIncludeDeletedSync()
            val localJournalsMap = allLocalJournals.associateBy { it.id }
            val remoteMedia = if (isFullRevalidation) {
                provider.listMedia().getOrThrow().toSet()
            } else {
                remoteManifest?.mediaMetadata?.keys ?: emptySet()
            }

            val tombstones = journalRepo.getAllTombstones()
            val tombstoneIds = tombstones.toSet()

            val plan = buildSyncPlan(
                allLocalJournals = allLocalJournals,
                remoteStates = remoteStates,
                remoteJournalMeta = remoteJournalMeta,
                tombstoneIds = tombstoneIds,
                localsToSync = localsToSync,
                isFullRevalidation = isFullRevalidation,
                remoteMedia = remoteMedia
            )

            val toDownload = plan.toDownload.toMutableList()
            val toUpload = plan.toUpload.toMutableList()

            AppLogger.d(
                AppLogger.Category.SYNC,
                "SyncManager",
                "Sync execution plan - To download: ${toDownload.size}, To upload: ${toUpload.size}, Tombstones: ${tombstones.size}"
            )

            // Pre-fetch songs early so we can include them in the total operation estimate.
            // This avoids an extra DAO query later and prevents progress from exceeding 1.0 (BUG-06).
            val preFetchedSongs = songLibraryDao.getAll()

            // Pre-estimate the full operation count using already-fetched manifest data + local FS checks.
            // No extra network calls: remoteMedia and remoteSongMediaMeta come from the manifest fetched above.
            val activeJournalsForPreScan = allLocalJournals.filter { it.deletedAt == null }
            val estimatedMediaUploads = activeJournalsForPreScan
                .flatMap { it.images }.map { File(it).name }.distinct()
                .count { name ->
                    val f = File(mediaDir, name)
                    f.exists() && f.length() > 0L && !remoteMedia.contains(name)
                }
            val estimatedMediaDownloads = remoteMedia.count { name ->
                val f = File(mediaDir, name)
                !f.exists() || f.length() == 0L
            }
            val estimatedSongUploads = preFetchedSongs.count { entry ->
                val name = File(entry.localPath).name
                val f = File(songMediaLibraryDir, name)
                f.exists() && f.length() > 0L && !remoteSongMediaMeta.containsKey(name)
            }
            val estimatedSongDownloads = remoteSongMediaMeta.keys.count { name ->
                val f = File(songMediaLibraryDir, name)
                !f.exists() || f.length() == 0L
            }
            var totalOperations = maxOf(
                toUpload.size + toDownload.size + tombstones.size +
                    estimatedMediaUploads + estimatedMediaDownloads +
                    estimatedSongUploads + estimatedSongDownloads,
                1
            )
            var completedOperations = 0
            var uploadCount = 0
            var downloadCount = 0
            var failedCount = 0

            toDownload.forEach { (id, remoteTime) ->
                _status.value = SyncStatus.Syncing(
                    progress = (completedOperations.toFloat() / totalOperations).coerceIn(0f, 0.99f),
                    uploadCount = uploadCount,
                    downloadCount = downloadCount,
                    totalOperations = totalOperations,
                    currentOperation = "Downloading update..."
                )

                downloadJournal(provider, id, remoteTime).onSuccess {
                    downloadCount++
                    completedOperations++
                }.onFailure {
                    failedCount++
                    completedOperations++
                }
            }

            toUpload.forEach { journal ->
                _status.value = SyncStatus.Syncing(
                    progress = (completedOperations.toFloat() / totalOperations).coerceIn(0f, 0.99f),
                    uploadCount = uploadCount,
                    downloadCount = downloadCount,
                    totalOperations = totalOperations,
                    currentOperation = "Pushing changes..."
                )

                pushJournal(provider, journal, remoteJournalMeta[journal.id]?.rev ?: 0).onSuccess {
                    uploadCount++
                    completedOperations++
                }.onFailure {
                    failedCount++
                    completedOperations++
                }
            }

            val processedTombstoneIds = if (tombstones.isNotEmpty()) {
                AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Processing tombstones: ${tombstones.size}")
                processTombstones(provider, tombstones)
            } else {
                emptyList()
            }

            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Verifying local media attachments are uploaded...")
            val currentLocalsForUpload = journalRepo.getAllJournalsIncludeDeletedSync()
            var uploadedMediaCount = 0
            val remoteMediaResult = provider.listMedia()
            val remoteMediaList = remoteMediaResult.getOrDefault(emptyList()).map { it.lowercase() }.toSet()
            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Fetched remote media list (count: ${remoteMediaList.size}). Provider listMedia success: ${remoteMediaResult.isSuccess}")

            val confirmedCloudMedia = (remoteMediaList + remoteMediaMeta.keys.map { it.lowercase() }).toMutableSet()

            var mediaUploadIndex = 0
            val totalMediaUploadsNeeded = currentLocalsForUpload.filter { it.deletedAt == null }.flatMap { it.images }.size
            currentLocalsForUpload.filter { it.deletedAt == null }.forEach { journal ->
                AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Checking journal ${journal.id} with ${journal.images.size} image references: ${journal.images}")
                journal.images.forEach { imgPath ->
                    val name = File(imgPath).name
                    val file = if (File(imgPath).isAbsolute && File(imgPath).exists()) File(imgPath) else File(mediaDir, name)
                    val remoteMeta = remoteMediaMeta[name]

                    val localExists = file.exists() && file.length() > 0L
                    val needsUpload = if (localExists) {
                        val isPhysicallyOnCloud = remoteMediaList.contains(name.lowercase())
                        if (remoteMeta != null && remoteMeta.hash.isNotBlank()) {
                            val localHash = file.computeSHA256()
                            val hashMismatch = localHash != remoteMeta.hash
                            val shouldUpload = hashMismatch || !isPhysicallyOnCloud
                            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Media $name: manifest meta exists. In cloud: $isPhysicallyOnCloud, hashMismatch: $hashMismatch -> shouldUpload: $shouldUpload")
                            shouldUpload
                        } else {
                            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Media $name: manifest meta null. In cloud: $isPhysicallyOnCloud -> shouldUpload: ${!isPhysicallyOnCloud}")
                            !isPhysicallyOnCloud
                        }
                    } else {
                        AppLogger.w(AppLogger.Category.SYNC, "SyncManager", "Media $name: local file DOES NOT EXIST at ${file.absolutePath}")
                        false
                    }

                    if (needsUpload) {
                        _status.value = SyncStatus.Syncing(
                            progress = (completedOperations.toFloat() / totalOperations).coerceIn(0f, 0.99f),
                            uploadCount = uploadCount,
                            downloadCount = downloadCount,
                            totalOperations = totalOperations,
                            currentOperation = "Uploading media..."
                        )
                        AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Triggering upload for media $name (file path: ${file.absolutePath})...")
                        provider.uploadMedia(journal.id, file).onSuccess {
                            uploadedMediaCount++
                            confirmedCloudMedia.add(name.lowercase())
                            completedOperations++
                            uploadCount++
                            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Successfully uploaded media $name")
                        }.onFailure { err ->
                            failedCount++
                            completedOperations++
                            AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "FAILED to upload media $name", err)
                        }
                    }
                }
            }
            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Media upload pass complete. Uploaded count: $uploadedMediaCount")

            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Verifying local song media attachments are uploaded...")
            val allLocalSongsToUpload = preFetchedSongs // re-use pre-fetched list (no extra DAO call)
            val remoteSongMediaList = provider.listSongMedia().getOrDefault(emptyList()).map { it.lowercase() }.toSet()
            val confirmedCloudSongMedia = (remoteSongMediaList + remoteSongMediaMeta.keys.map { it.lowercase() }).toMutableSet()
            var uploadedSongCount = 0

            allLocalSongsToUpload.forEach { entry ->
                val fileName = File(entry.localPath).name
                val audioFile = if (File(entry.localPath).isAbsolute && File(entry.localPath).exists()) {
                    File(entry.localPath)
                } else {
                    File(songMediaLibraryDir, fileName)
                }
                val remoteMeta = remoteSongMediaMeta[fileName]
                val localExists = audioFile.exists() && audioFile.length() > 0L
                val needsUpload = if (localExists) {
                    val isPhysicallyOnCloud = remoteSongMediaList.contains(fileName.lowercase())
                    if (remoteMeta != null && remoteMeta.hash.isNotBlank()) {
                        audioFile.computeSHA256() != remoteMeta.hash || !isPhysicallyOnCloud
                    } else {
                        !isPhysicallyOnCloud
                    }
                } else false

                if (needsUpload) {
                    _status.value = SyncStatus.Syncing(
                        progress = (completedOperations.toFloat() / totalOperations).coerceIn(0f, 0.99f),
                        uploadCount = uploadCount,
                        downloadCount = downloadCount,
                        totalOperations = totalOperations,
                        currentOperation = "Uploading song files..."
                    )
                    provider.uploadSongMedia(audioFile).onSuccess {
                        uploadedSongCount++
                        confirmedCloudSongMedia.add(fileName.lowercase())
                        completedOperations++
                        uploadCount++
                    }.onFailure { err ->
                        failedCount++
                        completedOperations++
                        AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "FAILED to upload song media $fileName", err)
                    }
                }

                entry.localArtPath?.let { artPath ->
                    val artFile = if (File(artPath).isAbsolute && File(artPath).exists()) File(artPath) else File(songMediaArtDir, File(artPath).name)
                    if (artFile.exists() && artFile.length() > 0L && !remoteSongMediaList.contains(artFile.name.lowercase())) {
                        provider.uploadSongMedia(artFile).onSuccess {
                            confirmedCloudSongMedia.add(artFile.name.lowercase())
                        }.onFailure { err ->
                            AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "FAILED to upload song art ${artFile.name}", err)
                        }
                    }
                }
            }
            if (uploadedSongCount > 0) {
                AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Uploaded $uploadedSongCount song files.")
            }

            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Purging old bin items from remote...")
            purgeOldBin(provider)

            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Verifying local media attachments are downloaded...")
            val currentLocals = journalRepo.getAllJournalsIncludeDeletedSync()
            var downloadedMediaCount = 0
            currentLocals.forEach { journal ->
                journal.images.forEach { imgPath ->
                    // Always resolve to canonical mediaDir path so we are immune to stale
                    // absolute paths stored from another device's filesystem layout.
                    val filename = File(imgPath).name
                    val file = File(mediaDir, filename)
                    val remoteMeta = remoteMediaMeta[filename]

                    val isPhysicallyOnCloud = remoteMediaList.contains(filename.lowercase()) || remoteMediaMeta.containsKey(filename)
                    val needsDownload = if (!isPhysicallyOnCloud) {
                        false
                    } else if (!file.exists() || file.length() == 0L) {
                        true
                    } else if (remoteMeta != null && remoteMeta.hash.isNotBlank()) {
                        file.computeSHA256() != remoteMeta.hash
                    } else {
                        false
                    }

                    if (needsDownload) {
                        _status.value = SyncStatus.Syncing(
                            progress = (completedOperations.toFloat() / totalOperations).coerceIn(0f, 0.99f),
                            uploadCount = uploadCount,
                            downloadCount = downloadCount,
                            totalOperations = totalOperations,
                            currentOperation = "Downloading media..."
                        )
                        provider.downloadMedia(journal.id, filename, file).onSuccess {
                            downloadedMediaCount++
                            downloadCount++
                            completedOperations++
                        }.onFailure { err ->
                            failedCount++
                            completedOperations++
                            AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "FAILED to download media $filename", err)
                        }
                    }
                }
            }
            if (downloadedMediaCount > 0) {
                AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Downloaded $downloadedMediaCount missing media files.")
            }

            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Verifying local song attachments are downloaded...")
            var downloadedSongCount = 0
            currentLocals.forEach { journal ->
                journal.songDetails?.let { song ->
                    var previewPath = song.localPreviewPath
                    if (previewPath == null) {
                        val match = songLibraryDao.getAll().firstOrNull {
                            it.title.trim().equals(song.title.trim(), ignoreCase = true) &&
                            it.artistName.trim().equals(song.artistName.trim(), ignoreCase = true)
                        }
                        if (match != null) {
                            previewPath = match.localPath
                        }
                    }
                    if (previewPath != null) {
                        val fileName = File(previewPath).name
                        val targetFile = File(songMediaLibraryDir, fileName)
                        val remoteMeta = remoteSongMediaMeta[fileName]
                        val isPhysicallyOnCloud = remoteSongMediaList.contains(fileName.lowercase()) || remoteSongMediaMeta.containsKey(fileName)
                        val needsDownload = if (!isPhysicallyOnCloud) {
                            false
                        } else if (!targetFile.exists() || targetFile.length() == 0L) {
                            true
                        } else if (remoteMeta != null && remoteMeta.hash.isNotBlank()) {
                            targetFile.computeSHA256() != remoteMeta.hash
                        } else {
                            false
                        }

                        if (needsDownload) {
                            _status.value = SyncStatus.Syncing(
                                progress = (completedOperations.toFloat() / totalOperations).coerceIn(0f, 0.99f),
                                uploadCount = uploadCount,
                                downloadCount = downloadCount,
                                totalOperations = totalOperations,
                                currentOperation = "Downloading song files..."
                            )
                            provider.downloadSongMedia(fileName, targetFile).onSuccess {
                                downloadedSongCount++
                                downloadCount++
                                completedOperations++

                                val contentHash = fileName.removeSuffix(".mp3")
                                if (songLibraryDao.getByHash(contentHash) == null) {
                                    songLibraryDao.upsert(
                                        SongLibraryEntity(
                                            contentHash = contentHash,
                                            localPath = targetFile.absolutePath,
                                            localArtPath = song.localThumbnailPath?.let { File(songMediaArtDir, File(it).name).absolutePath },
                                            sourceUrl = song.previewUrl,
                                            sourceType = song.sourceType.name,
                                            title = song.title,
                                            artistName = song.artistName,
                                            albumName = song.albumName,
                                            genre = song.genre,
                                            thumbnailUrl = song.thumbnailUrl
                                        )
                                    )
                                }
                                if (song.localPreviewPath != targetFile.absolutePath) {
                                    journalRepo.updateJournal(journal.copy(songDetails = song.copy(localPreviewPath = targetFile.absolutePath)))
                                }
                            }.onFailure { err ->
                                failedCount++
                                completedOperations++
                                AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "FAILED to download song $fileName", err)
                            }
                        } else if (targetFile.exists() && targetFile.length() > 0L) {
                            val contentHash = fileName.removeSuffix(".mp3")
                            if (songLibraryDao.getByHash(contentHash) == null) {
                                songLibraryDao.upsert(
                                    SongLibraryEntity(
                                        contentHash = contentHash,
                                        localPath = targetFile.absolutePath,
                                        localArtPath = song.localThumbnailPath?.let { File(songMediaArtDir, File(it).name).absolutePath },
                                        sourceUrl = song.previewUrl,
                                        sourceType = song.sourceType.name,
                                        title = song.title,
                                        artistName = song.artistName,
                                        albumName = song.albumName,
                                        genre = song.genre,
                                        thumbnailUrl = song.thumbnailUrl
                                    )
                                )
                            }
                            if (song.localPreviewPath != targetFile.absolutePath) {
                                journalRepo.updateJournal(journal.copy(songDetails = song.copy(localPreviewPath = targetFile.absolutePath)))
                            }
                        }
                    }

                    val thumbPath = song.localThumbnailPath
                    if (thumbPath != null) {
                        val artFileName = File(thumbPath).name
                        val targetArtFile = File(songMediaArtDir, artFileName)
                        val isPhysicallyOnCloud = remoteSongMediaList.contains(artFileName.lowercase()) || remoteSongMediaMeta.containsKey(artFileName)
                        if (isPhysicallyOnCloud && (!targetArtFile.exists() || targetArtFile.length() == 0L)) {
                            provider.downloadSongMedia(artFileName, targetArtFile)
                        }
                    }
                }
            }
            if (downloadedSongCount > 0) {
                AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Downloaded $downloadedSongCount song files.")
            }

            if (failedCount == 0) {
                if (isFullRevalidation) {
                    _status.value = SyncStatus.Syncing(
                        1f,
                        uploadCount,
                        downloadCount,
                        totalOperations,
                        "Cleaning up cloud media..."
                    )
                    cleanupCloudOrphanedMedia(provider, currentLocals)
                }

                AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Updating remote manifest...")
                val finalManifest = createCurrentManifest(
                    provider = provider,
                    processedDeletedIds = processedTombstoneIds,
                    remoteDeletedIds = remoteDeletedIds,
                    existingJournalMeta = remoteJournalMeta,
                    confirmedCloudMedia = confirmedCloudMedia,
                    confirmedCloudSongMedia = confirmedCloudSongMedia
                )
                provider.updateManifest(finalManifest).getOrThrow()
                syncPrefs.setLastSyncTime(System.currentTimeMillis())

                _status.value = SyncStatus.Success
                AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Sync successfully completed.")
                Result.success(Unit)
            } else {
                _status.value = SyncStatus.Error("Sync completed with $failedCount failures")
                AppLogger.w(AppLogger.Category.SYNC, "SyncManager", "Sync completed with $failedCount failures")
                Result.failure(Exception("Sync completed with $failedCount failures"))
            }
        } catch (e: Exception) {
            _status.value = SyncStatus.Error(e.message ?: "Sync failed")
            AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "Sync failed with exception", e)
            Result.failure(e)
        } finally {
            _syncActive.set(false)
        }
    }

    private suspend fun downloadJournal(provider: CloudProvider, id: String, remoteTime: Long): Result<Unit> {
        val filename = "$id.json"

        AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Downloading journal: $id")
        return provider.downloadJournal(filename).onSuccess { journal ->
            val local = journalRepo.getJournalById(id)

            val normalizedRemoteImages = journal.images.map { File(it).name }
            val normalizedRemoteJournal = journal.copy(images = normalizedRemoteImages)

            val finalJournal = if (local != null && (local.updatedAt ?: 0L) > (local.syncedAt ?: 0L)) {
                if (local.isContentEqualTo(normalizedRemoteJournal)) {
                    local.copy(syncedAt = remoteTime)
                } else {
                    val localTime = local.updatedAt ?: 0L
                    val remoteTimeField = normalizedRemoteJournal.updatedAt ?: remoteTime
                    if (localTime > remoteTimeField) {
                        local.copy(syncedAt = remoteTime)
                    } else {
                        // Remote is newer, but local has unsynced modifications.
                        // Preserve the local modifications as a Conflict Copy so user work is never lost.
                        val conflictTitle = if (local.title.isNotBlank()) "${local.title} (Conflict Copy)" else "Untitled (Conflict Copy)"
                        val conflictCopy = local.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            title = conflictTitle,
                            createdAt = local.updatedAt ?: System.currentTimeMillis(),
                            updatedAt = local.updatedAt ?: System.currentTimeMillis(),
                            syncedAt = null // Ensures it syncs to cloud on next pass
                        )
                        journalRepo.insertJournal(conflictCopy)
                        AppLogger.w(AppLogger.Category.SYNC, "SyncManager", "Conflict detected for journal $id: preserved local modifications in conflict copy ${conflictCopy.id}")
                        normalizedRemoteJournal
                    }
                }
            } else {
                normalizedRemoteJournal
            }

            val localizedImages = finalJournal.images.map { imgName ->
                File(mediaDir, File(imgName).name).absolutePath
            }
            val localizedSong = finalJournal.songDetails?.copy(
                localPreviewPath = finalJournal.songDetails.localPreviewPath?.let { File(songMediaLibraryDir, File(it).name).absolutePath },
                localThumbnailPath = finalJournal.songDetails.localThumbnailPath?.let { File(songMediaArtDir, File(it).name).absolutePath }
            )

            val effectiveSyncedAt = maxOf(remoteTime, finalJournal.updatedAt ?: 0L)

            journalRepo.insertJournal(
                finalJournal.copy(
                    images = localizedImages,
                    songDetails = localizedSong,
                    updatedAt = finalJournal.updatedAt ?: remoteTime,
                    syncedAt = effectiveSyncedAt
                )
            )
        }.map { Unit }
    }

    private suspend fun pushJournal(provider: CloudProvider, journal: Journal, currentRemoteRev: Int = 0): Result<Unit> {
        AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Pushing journal: ${journal.id}")

        val sanitizedImages = journal.images.map { File(it).name }
        val sanitizedSong = journal.songDetails?.copy(
            localPreviewPath = journal.songDetails.localPreviewPath?.let { File(it).name },
            localThumbnailPath = journal.songDetails.localThumbnailPath?.let { File(it).name }
        )
        val sanitizedJournal = journal.copy(images = sanitizedImages, songDetails = sanitizedSong)

        val now = System.currentTimeMillis()
        return provider.uploadJournal(sanitizedJournal).onSuccess { cloudId ->
            val effectiveTime = maxOf(now, journal.updatedAt ?: 0L)
            journalRepo.updateSyncStatus(journal.id, cloudId, effectiveTime)
        }.map { Unit }
    }

    private suspend fun cleanupCloudOrphanedMedia(
        provider: CloudProvider,
        journals: List<Journal>
    ) {
        try {
            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Cleaning up orphaned cloud media...")
            val remoteMedia = provider.listMedia().getOrNull() ?: return
            val localReferencedMedia = journals.flatMap { it.images }.map { File(it).name }.toSet()

            val orphans = remoteMedia.filter { it !in localReferencedMedia }
            if (orphans.isNotEmpty()) {
                AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Deleting ${orphans.size} orphaned media files from cloud.")
                val mediaOwnerMap = journals.flatMap { j -> j.images.map { File(it).name to j.id } }.toMap()
                orphans.forEach { filename ->
                    val journalId = mediaOwnerMap[filename] ?: ""
                    provider.deleteMedia(journalId, filename)
                }
            }

            val remoteSongs = provider.listSongMedia().getOrNull() ?: emptyList()
            val localSongs = songLibraryDao.getAll()
            val localLiveSongNames = journals.mapNotNull { it.songDetails }.flatMap {
                listOfNotNull(it.localPreviewPath?.let { p -> File(p).name }, it.localThumbnailPath?.let { p -> File(p).name })
            }.toSet() + localSongs.flatMap {
                listOfNotNull(File(it.localPath).name, it.localArtPath?.let { p -> File(p).name })
            }.toSet()

            val songOrphans = remoteSongs.filter { it !in localLiveSongNames }
            if (songOrphans.isNotEmpty()) {
                AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Deleting ${songOrphans.size} orphaned song files from cloud.")
                songOrphans.forEach { filename ->
                    provider.deleteSongMedia(filename)
                }
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "Orphaned media cleanup failed", e)
        }
    }

    private suspend fun createCurrentManifest(
        provider: CloudProvider,
        processedDeletedIds: List<String>,
        remoteDeletedIds: List<String>,
        existingJournalMeta: Map<String, JournalSyncMeta>,
        confirmedCloudMedia: Set<String>,
        confirmedCloudSongMedia: Set<String>
    ): SyncManifest {
        val journals = journalRepo.getAllJournalsIncludeDeletedSync()
        val total = journals.size
        val devId = syncPrefs.getDeviceId()
        val localTombstones = journalRepo.getAllTombstones()
        val allDeletedIds = (processedDeletedIds + localTombstones + remoteDeletedIds).distinct().takeLast(500)

        val updatedJournalMeta = mutableMapOf<String, JournalSyncMeta>()
        journals.forEach { j ->
            val existing = existingJournalMeta[j.id]
            val currentHash = j.computeContentHash()
            val newRev = if (existing != null && existing.contentHash != currentHash) existing.rev + 1 else existing?.rev ?: 1
            updatedJournalMeta[j.id] = JournalSyncMeta(rev = newRev, contentHash = currentHash)
        }

        val updatedMediaMeta = mutableMapOf<String, MediaSyncMeta>()
        journals.flatMap { it.images }.map { File(it).name }.distinct().forEach { filename ->
            val file = File(mediaDir, filename)
            val isConfirmedOnCloud = confirmedCloudMedia.contains(filename.lowercase())
            if (file.exists() && file.length() > 0L && isConfirmedOnCloud) {
                updatedMediaMeta[filename] = MediaSyncMeta(size = file.length(), hash = file.computeSHA256())
            }
        }

        val updatedSongMediaMeta = mutableMapOf<String, MediaSyncMeta>()
        val libraryFiles = songMediaLibraryDir.listFiles() ?: emptyArray()
        val artFiles = songMediaArtDir.listFiles() ?: emptyArray()
        (libraryFiles + artFiles).forEach { file ->
            val isConfirmedOnCloud = confirmedCloudSongMedia.contains(file.name.lowercase())
            if (file.exists() && file.length() > 0L && isConfirmedOnCloud) {
                updatedSongMediaMeta[file.name] = MediaSyncMeta(size = file.length(), hash = file.computeSHA256())
            }
        }

        return SyncManifest(
            lastSyncTime = System.currentTimeMillis(),
            lastSyncDeviceId = devId,
            databaseVersion = JournalDatabase.VERSION,
            schemaVersion = SyncManifest.CURRENT_SCHEMA_VERSION,
            totalJournals = total,
            totalMedia = updatedMediaMeta.size,
            totalSongMedia = updatedSongMediaMeta.size,
            deletedIds = allDeletedIds,
            journalMetadata = updatedJournalMeta,
            mediaMetadata = updatedMediaMeta,
            songMediaMetadata = updatedSongMediaMeta
        )
    }

    private suspend fun processTombstones(provider: CloudProvider, tombstones: List<String>): List<String> {
        val successfullyDeleted = mutableListOf<String>()
        tombstones.forEach { id ->
            _status.value = SyncStatus.Syncing(currentOperation = "Cleaning up cloud deletion...")
            val filename = "$id.json"
            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Deleting remote journal for tombstone: $id")
            provider.deleteJournal(filename).onSuccess {
                journalRepo.deleteTombstone(id)
                successfullyDeleted.add(id)
            }.onFailure { err ->
                AppLogger.w(AppLogger.Category.SYNC, "SyncManager", "Remote delete failed for tombstone $id (${err.message}). Keeping local tombstone for retry.")
            }
        }
        return successfullyDeleted
    }

    private suspend fun purgeOldBin(provider: CloudProvider) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val oldDeleted = journalRepo.getOldDeletedJournals(thirtyDaysAgo)

        if (oldDeleted.isNotEmpty()) {
            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Found ${oldDeleted.size} items in bin past 30 days. Purging...")
            oldDeleted.forEach { local ->
                _status.value = SyncStatus.Syncing(currentOperation = "Purging old items in bin...")
                val filename = "${local.id}.json"
                provider.deleteJournal(filename).onSuccess {
                    journalRepo.hardDeleteJournal(local.id)
                }.onFailure { err ->
                    AppLogger.w(AppLogger.Category.SYNC, "SyncManager", "Remote purge failed for ${local.id} (${err.message}). Hard deleting locally anyway.")
                    journalRepo.hardDeleteJournal(local.id)
                }
            }
        }
    }

    private data class SyncPlan(
        val toUpload: List<Journal>,
        val toDownload: List<Pair<String, Long>>
    )

    private fun buildSyncPlan(
        allLocalJournals: List<Journal>,
        remoteStates: Map<String, Pair<String, Long>>,
        remoteJournalMeta: Map<String, JournalSyncMeta>,
        tombstoneIds: Set<String>,
        localsToSync: List<Journal>,
        isFullRevalidation: Boolean,
        remoteMedia: Set<String> = emptySet()
    ): SyncPlan {
        val localJournalsMap = allLocalJournals.associateBy { it.id }
        val toDownload = mutableListOf<Pair<String, Long>>()
        val toUpload = mutableListOf<Journal>()

        AppLogger.d(
            AppLogger.Category.SYNC,
            "SyncManager",
            "buildSyncPlan: activeLocals=${allLocalJournals.count { it.deletedAt == null }}, " +
                "remoteFiles=${remoteStates.size}, manifestEntries=${remoteJournalMeta.size}, " +
                "tombstones=${tombstoneIds.size}, isFullRevalidation=$isFullRevalidation"
        )

        remoteStates.forEach { (id, remoteInfo) ->
            if (id in tombstoneIds) return@forEach

            val (_, remoteTime) = remoteInfo
            val local = localJournalsMap[id]

            if (local == null) {
                toDownload.add(id to remoteTime)
            } else {
                val localHash = local.computeContentHash()
                val remoteMetaEntry = remoteJournalMeta[id]

                if (remoteMetaEntry != null) {
                    val hashMatch = localHash == remoteMetaEntry.contentHash
                    if (hashMatch) return@forEach

                    val localTime = local.updatedAt ?: 0L
                    val syncAtTime = local.syncedAt ?: 0L
                    val hasLocalChange = localTime > (syncAtTime + SYNC_THRESHOLD_MS)

                    if (hasLocalChange && localTime > remoteTime + SYNC_THRESHOLD_MS) {
                        toUpload.add(local)
                    } else {
                        toDownload.add(id to remoteTime)
                    }
                } else {
                    val localTime = local.updatedAt ?: 0L
                    val syncAtTime = local.syncedAt ?: 0L

                    val hasRemoteChange = remoteTime > (syncAtTime + SYNC_THRESHOLD_MS) && remoteTime > (localTime + SYNC_THRESHOLD_MS)
                    val hasLocalChange = localTime > (syncAtTime + SYNC_THRESHOLD_MS) && localTime > (remoteTime + SYNC_THRESHOLD_MS)

                    if (hasRemoteChange) {
                        toDownload.add(id to remoteTime)
                    } else if (hasLocalChange) {
                        toUpload.add(local)
                    }
                }
            }
        }

        // Second pass: upload any local journals that have no remote counterpart yet.
        // "Local is newer than remote" is already fully handled in the first pass above,
        // so the else-branch here was redundant and could cause duplicate queue entries.
        val alreadyQueuedIds = toUpload.map { it.id }.toSet()
        allLocalJournals.forEach { local ->
            if (remoteStates[local.id] == null && local.id !in alreadyQueuedIds) {
                toUpload.add(local)
            }
        }

        if (!isFullRevalidation) {
            val actuallyModified = localsToSync.map { it.id }.toSet()
            val beforeFilterCount = toUpload.size
            toUpload.retainAll { journal ->
                journal.id in actuallyModified || remoteStates.isEmpty() || remoteStates[journal.id] == null
            }
            if (beforeFilterCount != toUpload.size) {
                AppLogger.d(
                    AppLogger.Category.SYNC,
                    "SyncManager",
                    "retainAll filter reduced upload queue from $beforeFilterCount to ${toUpload.size}"
                )
            }
        }

        return SyncPlan(toUpload = toUpload, toDownload = toDownload)
    }
}