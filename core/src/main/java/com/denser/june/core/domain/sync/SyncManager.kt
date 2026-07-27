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
    val pendingUploadsCount: Int,
    val pendingDownloadsCount: Int,
    val pendingMediaUploadsCount: Int,
    val pendingMediaDownloadsCount: Int,
    val pendingDeletionsCount: Int,
    val pendingUploadsList: List<String> = emptyList(),
    val pendingDownloadsList: List<String> = emptyList(),
    val localDeletionsList: List<String> = emptyList(),
    val remoteDeletionsList: List<String> = emptyList(),
    val pendingMediaUploadsList: List<String> = emptyList(),
    val pendingMediaDownloadsList: List<String> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SyncManager(
    private val journalRepo: JournalRepository,
    private val syncPrefs: SyncPreferences,
    private val providers: Map<String, CloudProvider>,
    private val mediaDir: File,
    private val syncScheduler: SyncScheduler,
    private val applicationScope: CoroutineScope
) {
    companion object {
        const val SYNC_THRESHOLD_MS = 2000L
        const val CURRENT_DATA_REPAIR_VERSION = 1
    }

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    private val syncMutex = Mutex()
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
            if (syncPrefs.getLastCompletedDataRepairVersion().first() < CURRENT_DATA_REPAIR_VERSION) {
                repairDoubleConcatenatedImages()
            }
        }

        applicationScope.launch {
            syncPrefs.getSyncEnabled().flatMapLatest { isSyncEnabled ->
                if (!isSyncEnabled) kotlinx.coroutines.flow.flowOf(null)
                else {
                    combine(
                        journalRepo.observeHasUnsyncedJournals(SYNC_THRESHOLD_MS),
                        journalRepo.observeHasTombstones()
                    ) { hasUnsynced, hasTombstones -> hasUnsynced || hasTombstones }
                }
            }.collect { isDirty ->
                val current = _status.value
                when {
                    isDirty == true && (current is SyncStatus.Idle || current is SyncStatus.Success) -> {
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
                            journalRepo.observeHasTombstones()
                        ) { hasUnsynced, hasTombstones -> hasUnsynced || hasTombstones }
                            .debounce(10000L)
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
            syncPrefs.setLastCompletedDataRepairVersion(CURRENT_DATA_REPAIR_VERSION)
            AppLogger.d(
                AppLogger.Category.SYNC, 
                "SyncManager", 
                "Startup repair completed. Checked $checkedCount journals, repaired $repairedCount journals."
            )
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "Error repairing double-concatenated image paths", e)
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

            val referencedMediaNames = allLocalJournals.flatMap { it.images }
                .map { File(it).name }
                .distinct()
            val localMediaFiles = referencedMediaNames.toSet()

            val remoteMediaMeta = remoteManifest?.mediaMetadata ?: emptyMap()
            val mediaToUpload = localMediaFiles.filter { name ->
                val file = File(mediaDir, name)
                val isPhysicallyOnCloud = remoteMedia.any { it.equals(name, ignoreCase = true) } || remoteMediaMeta.containsKey(name)
                file.exists() && file.length() > 0L && !isPhysicallyOnCloud
            }
            val mediaToDownload = remoteMedia.filter { name ->
                localMediaFiles.none { it.equals(name, ignoreCase = true) }
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

            Result.success(
                SyncAnalysis(
                    localJournals = allLocalJournals.count { it.deletedAt == null },
                    remoteJournals = remoteJournals.size,
                    localMedia = localMediaFiles.size,
                    remoteMedia = remoteMedia.size,
                    pendingUploadsCount = plan.toUpload.size,
                    pendingDownloadsCount = plan.toDownload.size,
                    pendingUploadsList = plan.toUpload.map { it.title.ifBlank { "Untitled" } },
                    pendingDownloadsList = plan.toDownload.map { (id, _) -> id },
                    localDeletionsList = localDeletions,
                    remoteDeletionsList = emptyList(),
                    pendingMediaUploadsCount = mediaToUpload.size,
                    pendingMediaDownloadsCount = mediaToDownload.size,
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
                val activePaths = allJournals.flatMap { it.images }
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

            val totalOperations = toUpload.size + toDownload.size
            var completedOperations = 0
            var uploadCount = 0
            var downloadCount = 0
            var failedCount = 0

            toDownload.forEach { (id, remoteTime) ->
                _status.value = SyncStatus.Syncing(
                    progress = completedOperations.toFloat() / totalOperations,
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
                }
            }

            toUpload.forEach { journal ->
                _status.value = SyncStatus.Syncing(
                    progress = completedOperations.toFloat() / totalOperations,
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
                }
            }

            if (tombstones.isNotEmpty()) {
                AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Processing tombstones: ${tombstones.size}")
                processTombstones(provider, tombstones)
            }

            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Verifying local media attachments are uploaded...")
            val currentLocalsForUpload = journalRepo.getAllJournalsIncludeDeletedSync()
            var uploadedMediaCount = 0
            val remoteMediaResult = provider.listMedia()
            val remoteMediaList = remoteMediaResult.getOrDefault(emptyList()).map { it.lowercase() }.toSet()
            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Fetched remote media list (count: ${remoteMediaList.size}). Provider listMedia success: ${remoteMediaResult.isSuccess}")

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
                            progress = if (totalOperations > 0) completedOperations.toFloat() / totalOperations else 0.5f,
                            uploadCount = uploadCount,
                            downloadCount = downloadCount,
                            totalOperations = totalOperations,
                            currentOperation = "Uploading media..."
                        )
                        AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Triggering upload for media $name (file path: ${file.absolutePath})...")
                        provider.uploadMedia(journal.id, file).onSuccess {
                            uploadedMediaCount++
                            completedOperations++
                            uploadCount++
                            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Successfully uploaded media $name")
                        }.onFailure { err ->
                            AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "FAILED to upload media $name", err)
                        }
                    }
                }
            }
            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Media upload pass complete. Uploaded count: $uploadedMediaCount")

            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Purging old bin items from remote...")
            purgeOldBin(provider)

            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Verifying local media attachments are downloaded...")
            val currentLocals = journalRepo.getAllJournalsIncludeDeletedSync()
            var downloadedMediaCount = 0
            currentLocals.forEach { journal ->
                journal.images.forEach { imgPath ->
                    val file = File(imgPath)
                    val filename = file.name
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
                            progress = if (totalOperations > 0) completedOperations.toFloat() / totalOperations else 0.8f,
                            uploadCount = uploadCount,
                            downloadCount = downloadCount,
                            totalOperations = totalOperations,
                            currentOperation = "Downloading media..."
                        )
                        provider.downloadMedia(journal.id, filename, file).onSuccess {
                            downloadedMediaCount++
                            downloadCount++
                            completedOperations++
                        }
                    }
                }
            }
            if (downloadedMediaCount > 0) {
                AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Downloaded $downloadedMediaCount missing media files.")
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
                val finalManifest = createCurrentManifest(provider, remoteDeletedIds, remoteJournalMeta)
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
                        normalizedRemoteJournal
                    }
                }
            } else {
                normalizedRemoteJournal
            }

            val localizedImages = finalJournal.images.map { imgName ->
                File(mediaDir, File(imgName).name).absolutePath
            }
            val downloadResults = finalJournal.images.map { imgName ->
                val targetFile = File(mediaDir, File(imgName).name)
                val needsDownload = !targetFile.exists() || targetFile.length() == 0L
                if (needsDownload) {
                    provider.downloadMedia(id, File(imgName).name, targetFile)
                } else {
                    Result.success(targetFile)
                }
            }
            val failedDownloads = downloadResults.filter { it.isFailure }
            if (failedDownloads.isNotEmpty()) {
                AppLogger.w(AppLogger.Category.SYNC, "SyncManager", "${failedDownloads.size} media files failed to download for journal $id. Will retry on next sync.")
            }

            val effectiveSyncedAt = maxOf(remoteTime, finalJournal.updatedAt ?: 0L)

            journalRepo.insertJournal(
                finalJournal.copy(
                    images = localizedImages,
                    updatedAt = finalJournal.updatedAt ?: remoteTime,
                    syncedAt = effectiveSyncedAt
                )
            )
        }.map { Unit }
    }

    private suspend fun pushJournal(provider: CloudProvider, journal: Journal, currentRemoteRev: Int = 0): Result<Unit> {
        AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Pushing journal: ${journal.id}")
        
        val uploadResults = journal.images.map { localPath ->
            val file = if (File(localPath).isAbsolute && File(localPath).exists()) {
                File(localPath)
            } else {
                File(mediaDir, File(localPath).name)
            }
            if (file.exists()) {
                provider.uploadMedia(journal.id, file)
            } else {
                AppLogger.w(AppLogger.Category.SYNC, "SyncManager", "Media file not found locally: ${file.absolutePath}")
                Result.success(localPath)
            }
        }
        val failedUploads = uploadResults.filter { it.isFailure }
        if (failedUploads.isNotEmpty()) {
            AppLogger.w(AppLogger.Category.SYNC, "SyncManager", "${failedUploads.size} media files failed to upload for journal ${journal.id}. Will retry on next sync.")
        }

        val sanitizedImages = journal.images.map { File(it).name }
        val sanitizedJournal = journal.copy(images = sanitizedImages)

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
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.SYNC, "SyncManager", "Orphaned media cleanup failed", e)
        }
    }

    private suspend fun createCurrentManifest(
        provider: CloudProvider,
        remoteDeletedIds: List<String>,
        existingJournalMeta: Map<String, JournalSyncMeta>
    ): SyncManifest {
        val journals = journalRepo.getAllJournalsIncludeDeletedSync()
        val total = journals.size
        val devId = syncPrefs.getDeviceId()
        val localTombstones = journalRepo.getAllTombstones()
        val allDeletedIds = (localTombstones + remoteDeletedIds).distinct()

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
            if (file.exists() && file.length() > 0L) {
                updatedMediaMeta[filename] = MediaSyncMeta(size = file.length(), hash = file.computeSHA256())
            }
        }

        return SyncManifest(
            lastSyncTime = System.currentTimeMillis(),
            lastSyncDeviceId = devId,
            databaseVersion = JournalDatabase.VERSION,
            schemaVersion = SyncManifest.CURRENT_SCHEMA_VERSION,
            totalJournals = total,
            totalMedia = updatedMediaMeta.size,
            deletedIds = allDeletedIds,
            journalMetadata = updatedJournalMeta,
            mediaMetadata = updatedMediaMeta
        )
    }

    private suspend fun processTombstones(provider: CloudProvider, tombstones: List<String>) {
        tombstones.forEach { id ->
            _status.value = SyncStatus.Syncing(currentOperation = "Cleaning up cloud deletion...")
            val filename = "$id.json"
            AppLogger.d(AppLogger.Category.SYNC, "SyncManager", "Deleting remote journal for tombstone: $id")
            provider.deleteJournal(filename).onSuccess {
                journalRepo.deleteTombstone(id)
            }.onFailure { err ->
                AppLogger.w(AppLogger.Category.SYNC, "SyncManager", "Remote delete failed for tombstone $id (${err.message}). Removing local tombstone.")
                journalRepo.deleteTombstone(id)
            }
        }
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
                    if (hashMatch || local.deletedAt != null) return@forEach

                    val localTime = local.updatedAt ?: 0L
                    val syncAtTime = local.syncedAt ?: 0L
                    val hasLocalChange = localTime > (syncAtTime + SYNC_THRESHOLD_MS)

                    if (hasLocalChange && localTime > remoteTime + SYNC_THRESHOLD_MS) {
                        toUpload.add(local)
                    } else {
                        toDownload.add(id to remoteTime)
                    }
                } else {
                    if (local.deletedAt != null) return@forEach
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

        allLocalJournals.forEach { local ->
            if (local.deletedAt != null) return@forEach

            val remote = remoteStates[local.id]
            if (remote == null) {
                if (!toUpload.any { it.id == local.id }) toUpload.add(local)
            } else {
                val remoteTime = remote.second
                val localTime = local.updatedAt ?: 0L

                if (localTime > (remoteTime + SYNC_THRESHOLD_MS)) {
                    if (!toUpload.any { it.id == local.id }) {
                        toUpload.add(local)
                    }
                }
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