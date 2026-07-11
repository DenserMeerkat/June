package com.denser.june.core.domain.sync

import android.util.Log
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.preferences.SyncPreferences
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
import kotlinx.coroutines.flow.debounce
import com.denser.june.core.data.sync.SyncWorker
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.data.database.journal.JournalDatabase
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
    private val context: android.content.Context,
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
                        SyncWorker.enqueue(context, onlyWifi)
                    }
                }
        }
    }

    private suspend fun repairDoubleConcatenatedImages() {
        try {
            val journals = journalRepo.getAllJournalsIncludeDeletedSync()
            journals.forEach { journal ->
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
                    journalRepo.insertJournal(journal.copy(images = cleanedImages))
                }
            }
            syncPrefs.setLastCompletedDataRepairVersion(CURRENT_DATA_REPAIR_VERSION)
        } catch (e: Exception) {
            android.util.Log.e("SyncManager", "Error repairing double-concatenated image paths", e)
        }
    }

    fun resetStatus() {
        applicationScope.launch {
            val hasUnsynced = journalRepo.hasUnsyncedJournals(SYNC_THRESHOLD_MS)
            val hasTombstones = journalRepo.hasTombstones()

            _status.value = if (hasUnsynced || hasTombstones) SyncStatus.Dirty else SyncStatus.Idle
        }
    }

    suspend fun performAnalysis(): Result<SyncAnalysis> = syncMutex.withLock {
        val isSyncEnabled = syncPrefs.getSyncEnabled().first()
        if (!isSyncEnabled) return@withLock Result.failure(Exception("Sync is disabled"))

        try {
            val provider = getActiveProvider()
            provider.connect().getOrThrow()

            val remoteJournals = provider.listJournals().getOrThrow()
            val remoteMedia = provider.listMedia().getOrThrow().toSet()

            val allLocalJournals = journalRepo.getAllJournalsIncludeDeletedSync()
            val lastSyncTime = syncPrefs.getLastSyncTime().first()
            val tombstones = journalRepo.getAllTombstones()

            val referencedMediaNames = allLocalJournals.flatMap { it.images }
                .map { File(it).name }
                .distinct()
            val localMediaFiles = referencedMediaNames.toSet()


            val mediaToUpload = localMediaFiles.filter { name ->
                val localExists = File(mediaDir, name).exists()
                localExists && remoteMedia.none { it.equals(name, ignoreCase = true) }
            }
            val mediaToDownload = remoteMedia.filter { name ->
                localMediaFiles.none { it.equals(name, ignoreCase = true) }
            }

            val remoteStates = remoteJournals.associate { meta ->
                val id = meta.name.removeSuffix(".json")
                id to (meta.name to meta.lastModified)
            }

            val localJournalsMap = allLocalJournals.associateBy { it.id }

            val realPendingUploads = mutableListOf<String>()
            val realPendingDownloads = mutableListOf<String>()
            val localDeletions = mutableListOf<String>()
            val remoteDeletions = mutableListOf<String>()

            remoteStates.forEach { (id, remoteInfo) ->
                if (id in tombstones) {
                    localDeletions.add("$id.json")
                    return@forEach
                }

                val (filename, remoteTime) = remoteInfo
                val local = localJournalsMap[id]
                if (local == null) {
                    realPendingDownloads.add("$id.json")
                } else {
                    val syncedAtTime = local.syncedAt ?: 0L
                    if (remoteTime > syncedAtTime + SYNC_THRESHOLD_MS) {
                        realPendingDownloads.add(local.title.ifBlank { "Untitled" })
                    }
                }
            }

            val isRemoteEmpty = remoteJournals.isEmpty()
            allLocalJournals.forEach { local ->
                val remote = remoteStates[local.id]
                if (remote == null) {
                    if (local.syncedAt != null && !isRemoteEmpty) {
                        remoteDeletions.add(local.title.ifBlank { "Untitled" })
                    } else {
                        realPendingUploads.add(local.title.ifBlank { "Untitled" })
                    }
                } else {
                    val remoteTime = remote.second
                    val localTime = local.updatedAt ?: 0L
                    val syncAtTime = local.syncedAt ?: 0L

                    if (localTime > (syncAtTime + SYNC_THRESHOLD_MS) && localTime > (remoteTime + SYNC_THRESHOLD_MS)) {
                        realPendingUploads.add(local.title.ifBlank { "Untitled" })
                    }
                }
            }

            Result.success(
                SyncAnalysis(
                    localJournals = allLocalJournals.size,
                    remoteJournals = remoteJournals.size,
                    localMedia = localMediaFiles.size,
                    remoteMedia = remoteMedia.size,
                    pendingUploadsCount = realPendingUploads.size,
                    pendingDownloadsCount = realPendingDownloads.size + remoteDeletions.size,
                    pendingUploadsList = realPendingUploads,
                    pendingDownloadsList = realPendingDownloads,
                    localDeletionsList = localDeletions,
                    remoteDeletionsList = remoteDeletions,
                    pendingMediaUploadsCount = mediaToUpload.size,
                    pendingMediaDownloadsCount = mediaToDownload.size,
                    pendingDeletionsCount = tombstones.size,
                    pendingMediaUploadsList = mediaToUpload,
                    pendingMediaDownloadsList = mediaToDownload
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun launchSync(isFullRevalidation: Boolean = false) {
        applicationScope.launch {
            val onlyWifi = syncPrefs.getSyncOnlyOnWifi().first()
            SyncWorker.enqueue(context, onlyWifi, immediate = true, isFullRevalidation = isFullRevalidation)
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

        try {
            val provider = getActiveProvider()
            provider.connect().getOrThrow()

            val remoteManifest = provider.getManifest().getOrNull()
            if (remoteManifest != null && remoteManifest.schemaVersion > 2) {
                throw Exception("A newer version of the app is required to sync with this cloud database.")
            }
            val remoteDeletedIds = remoteManifest?.deletedIds ?: emptyList()


            remoteDeletedIds.forEach { id ->
                journalRepo.hardDeleteJournal(id)
                journalRepo.deleteTombstone(id)
            }

            val remoteMetaList = provider.listJournals().getOrThrow()
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
            val remoteMedia = if (isFullRevalidation) provider.listMedia().getOrThrow().toSet()
            else emptySet<String>()

            val tombstones = journalRepo.getAllTombstones()
            val toDownload = mutableListOf<Pair<String, Long>>()
            val toUpload = mutableListOf<Journal>()

            remoteStates.forEach { (id, remoteInfo) ->
                if (id in tombstones) return@forEach

                val (filename, remoteTime) = remoteInfo
                val local = localJournalsMap[id]

                if (local == null) {
                    toDownload.add(id to remoteTime)
                } else {
                    val localTime = local.updatedAt ?: 0L
                    val syncAtTime = local.syncedAt ?: 0L

                    val hasRemoteChange = remoteTime > (syncAtTime + SYNC_THRESHOLD_MS)
                    val hasLocalChange = localTime > (syncAtTime + SYNC_THRESHOLD_MS)

                    if (hasRemoteChange && hasLocalChange) {
                        toDownload.add(id to remoteTime)
                    } else if (hasRemoteChange) {
                        toDownload.add(id to remoteTime)
                    }
                }
            }

            val isRemoteEmpty = remoteStates.isEmpty()
            allLocalJournals.forEach { local ->
                val remote = remoteStates[local.id]
                if (remote == null) {
                    if (local.syncedAt != null && !isRemoteEmpty) {
                        journalRepo.hardDeleteJournal(local.id)
                    } else {
                        toUpload.add(local)
                    }
                } else {
                    val remoteTime = remote.second
                    val localTime = local.updatedAt ?: 0L

                    if (localTime > (remoteTime + SYNC_THRESHOLD_MS)) {
                        if (!toUpload.any { it.id == local.id }) toUpload.add(local)
                    } else if (isFullRevalidation) {
                        val isMediaMissingFromCloud = local.images.any { imagePath ->
                            val name = File(imagePath).name
                            remoteMedia.none { it.equals(name, ignoreCase = true) }
                        }
                        if (isMediaMissingFromCloud && !toUpload.any { it.id == local.id }) {
                            toUpload.add(local)
                        }
                    }
                }
            }

            if (!isFullRevalidation) {
                val actuallyModified = localsToSync.map { it.id }.toSet()
                toUpload.retainAll { it.id in actuallyModified }
            }

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

                downloadJournal(id, remoteTime).onSuccess {
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
                    currentOperation = "Pushing ${journal.title.ifBlank { "changes" }}..."
                )

                pushJournal(journal).onSuccess {
                    uploadCount++
                    completedOperations++
                }.onFailure {
                    failedCount++
                }
            }

            processTombstones(provider, tombstones)
            purgeOldBin(provider)

            val currentLocals = journalRepo.getAllJournalsIncludeDeletedSync()
            currentLocals.forEach { journal ->
                journal.images.forEach { imgPath ->
                    val file = File(imgPath)
                    if (!file.exists() || file.length() == 0L) {
                        provider.downloadMedia(journal.id, file.name, file)
                    }
                }
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

                val finalManifest = createCurrentManifest(remoteDeletedIds)
                provider.updateManifest(finalManifest).getOrThrow()
                syncPrefs.setLastSyncTime(System.currentTimeMillis())

                _status.value = SyncStatus.Success
                Result.success(Unit)
            } else {
                _status.value = SyncStatus.Error("Sync completed with $failedCount failures")
                Result.failure(Exception("Sync completed with $failedCount failures"))
            }
        } catch (e: Exception) {
            _status.value = SyncStatus.Error(e.message ?: "Sync failed")
            Result.failure(e)
        }
    }

    private suspend fun downloadJournal(id: String, remoteTime: Long): Result<Unit> {
        val provider = getActiveProvider()
        val filename = "$id.json"

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

            if (local != null) {
                journalRepo.updateSyncStatus(local.id, id, remoteTime)
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
            if (downloadResults.any { it.isFailure }) {
                Log.w("SyncManager", "Some media files failed to download for journal $id. Will retry on next sync.")
            }


            journalRepo.insertJournal(
                finalJournal.copy(
                    images = localizedImages,
                    updatedAt = finalJournal.updatedAt ?: remoteTime,
                    syncedAt = remoteTime
                )
            )
        }.map { Unit }
    }

    private suspend fun pushJournal(journal: Journal): Result<Unit> {
        val provider = getActiveProvider()
        
        val uploadResults = journal.images.map { localPath ->
            val file = File(localPath)
            if (file.exists()) {
                provider.uploadMedia(journal.id, file)
            } else {
                Result.success(localPath)
            }
        }
        val failedUploads = uploadResults.filter { it.isFailure }
        if (failedUploads.isNotEmpty()) {
            Log.w("SyncManager", "${failedUploads.size} media files failed to upload for journal ${journal.id}. Will retry on next sync.")
        }

        val sanitizedImages = journal.images.map { File(it).name }
        val sanitizedJournal = journal.copy(images = sanitizedImages)

        return provider.uploadJournal(sanitizedJournal).onSuccess { cloudId ->
            journalRepo.updateSyncStatus(journal.id, cloudId, System.currentTimeMillis())
        }.map { Unit }
    }

    private suspend fun cleanupCloudOrphanedMedia(
        provider: CloudProvider,
        journals: List<Journal>
    ) {
        try {
            val remoteMedia = provider.listMedia().getOrNull() ?: return
            val localReferencedMedia = journals.flatMap { it.images }.map { File(it).name }.toSet()

            val orphans = remoteMedia.filter { it !in localReferencedMedia }
            if (orphans.isNotEmpty()) {
                orphans.forEach { filename ->
                    provider.deleteMedia("", filename)
                }
            }
        } catch (e: Exception) {
        }
    }

    private suspend fun createCurrentManifest(remoteDeletedIds: List<String>): SyncManifest {
        val journals = journalRepo.getAllJournalsIncludeDeletedSync()
        val total = journals.size
        val totalMedia = journals.flatMap { it.images }.map { File(it).name }.distinct().size
        val devId = syncPrefs.getDeviceId()
        val localTombstones = journalRepo.getAllTombstones()
        val allDeletedIds = (localTombstones + remoteDeletedIds).distinct()
        
        return SyncManifest(
            lastSyncTime = System.currentTimeMillis(),
            lastSyncDeviceId = devId,
            databaseVersion = JournalDatabase.VERSION,
            schemaVersion = 2,
            totalJournals = total,
            totalMedia = totalMedia,
            deletedIds = allDeletedIds
        )

    }

    private suspend fun processTombstones(provider: CloudProvider, tombstones: List<String>) {
        tombstones.forEach { id ->
            _status.value = SyncStatus.Syncing(currentOperation = "Cleaning up cloud deletion...")
            val filename = "$id.json"
            provider.deleteJournal(filename).onSuccess {
                journalRepo.deleteTombstone(id)
            }
        }
    }

    private suspend fun purgeOldBin(provider: CloudProvider) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val oldDeleted = journalRepo.getOldDeletedJournals(thirtyDaysAgo)

        oldDeleted.forEach { local ->
            _status.value = SyncStatus.Syncing(currentOperation = "Purging old items in bin...")
            val filename = "${local.id}.json"
            provider.deleteJournal(filename).onSuccess {
                journalRepo.hardDeleteJournal(local.id)
            }
        }
    }
}