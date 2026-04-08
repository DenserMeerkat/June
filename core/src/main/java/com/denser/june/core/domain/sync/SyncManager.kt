package com.denser.june.core.domain.sync

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
import java.io.File

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
    data class Error(val message: String) : SyncStatus()
}

data class SyncAnalysis(
    val localJournals: Int,
    val remoteJournals: Int,
    val localMedia: Int,
    val remoteMedia: Int,
    val pendingUploads: List<String>,
    val pendingDownloads: List<String>,
    val pendingUploadsCount: Int,
    val pendingDownloadsCount: Int,
    val pendingMediaUploads: Int,
    val pendingMediaDownloads: Int
)

class SyncManager(
    private val journalRepo: JournalRepository,
    private val syncPrefs: SyncPreferences,
    private val providers: Map<String, CloudProvider>,
    private val mediaDir: File,
    private val applicationScope: CoroutineScope
) {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    private val syncMutex = Mutex()
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    fun resetStatus() {
        _status.value = SyncStatus.Idle
    }

    suspend fun performAnalysis(): Result<SyncAnalysis> = syncMutex.withLock {
        val isSyncEnabled = syncPrefs.getSyncEnabled().first()
        if (!isSyncEnabled) return@withLock Result.failure(Exception("Sync is disabled"))

        try {
            val provider = getActiveProvider()
            provider.connect().getOrThrow()

            val remoteJournals = provider.listJournals().getOrNull() ?: emptyList<RemoteFileMeta>()
            val remoteMedia = provider.listMedia().getOrNull() ?: emptyList<String>().toSet()

            val allLocalJournals = journalRepo.getAllJournals()
            val lastSyncTime = syncPrefs.getLastSyncTime().first()

            val localMediaFiles = mediaDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
            val allLocalMediaNames = allLocalJournals.flatMap { it.images }.map { File(it).name }.distinct()
            
            val mediaToUploadCount = allLocalMediaNames.count { name ->
                remoteMedia.none { it.equals(name, ignoreCase = true) }
            }
            val mediaToDownloadCount = remoteMedia.count { name ->
                localMediaFiles.none { it.equals(name, ignoreCase = true) }
            }

            val remoteStates = remoteJournals.associate { meta ->
                val id = meta.name.removeSuffix(".json")
                id to (meta.name to meta.lastModified)
            }

            val toDownloadTitles = mutableListOf<String>()
            val toUploadTitles = mutableListOf<String>()

            val localJournalsMap = allLocalJournals.associateBy { it.id }

            val realPendingUploads = mutableListOf<String>()
            val realPendingDownloads = mutableListOf<String>()

            remoteStates.forEach { (id, remoteInfo) ->
                val (filename, remoteTime) = remoteInfo
                val local = localJournalsMap[id]
                if (local == null) {
                    realPendingDownloads.add("New from cloud ($id)")
                } else if (remoteTime > (local.updatedAt ?: 0L) + 2000) {
                    realPendingDownloads.add("${local.title.ifBlank { "Untitled" }} (Cloud is newer)")
                }
            }

            allLocalJournals.forEach { local ->
                val remote = remoteStates[local.id]
                if (remote == null) {
                    realPendingUploads.add("${local.title.ifBlank { "Untitled" }} (Missing from cloud)")
                } else {
                    val missingMediaNames = local.images.filter { imagePath ->
                        val name = File(imagePath).name
                        remoteMedia.none { it.equals(name, ignoreCase = true) }
                    }.map { File(it).name }

                    if (missingMediaNames.isNotEmpty()) {
                        realPendingUploads.add("${local.title.ifBlank { "Untitled" }} (Missing media: ${missingMediaNames.first()})")
                    } else {
                        val localTime = local.updatedAt ?: 0L
                        val diff = localTime - (remote?.second ?: 0L)
                        if (diff > 2000) {
                            realPendingUploads.add("${local.title.ifBlank { "Untitled" }} (Local is newer)")
                        }
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
                    pendingDownloadsCount = realPendingDownloads.size,
                    pendingUploads = realPendingUploads.distinct().take(10),
                    pendingDownloads = realPendingDownloads.distinct().take(10),
                    pendingMediaUploads = mediaToUploadCount,
                    pendingMediaDownloads = mediaToDownloadCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun launchSync(isFullRevalidation: Boolean = false) {
        applicationScope.launch {
            sync(isFullRevalidation)
        }
    }
    private suspend fun getActiveProvider(): CloudProvider {
        val selected = syncPrefs.getSelectedProvider().first() ?: "WebDAV"
        return providers[selected] ?: providers["WebDAV"]!!
    }

    suspend fun testConnection(): Result<Unit> {
        return getActiveProvider().connect()
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

            val remoteMetaList = provider.listJournals().getOrNull() ?: emptyList<RemoteFileMeta>()
            val lastSyncTime = if (isFullRevalidation) 0L else syncPrefs.getLastSyncTime().first()
            val localsToSync = journalRepo.getJournalsToSync(lastSyncTime)

            val remoteStates = remoteMetaList.associate { meta ->
                val id = meta.name.removeSuffix(".json")
                id to (meta.name to meta.lastModified)
            }

            val allLocalJournals = journalRepo.getAllJournals()
            val localJournalsMap = allLocalJournals.associateBy { it.id }
            val remoteMedia = if (isFullRevalidation) provider.listMedia().getOrNull()?.toSet() ?: emptySet<String>() else emptySet<String>()
            
            val toDownload = mutableListOf<Pair<String, Long>>()
            val toUpload = mutableListOf<Journal>()

            remoteStates.forEach { (id, remoteInfo) ->
                val (filename, remoteTime) = remoteInfo
                val local = localJournalsMap[id]
                
                if (local == null) {
                    toDownload.add(id to remoteTime)
                } else {
                    val localTime = local.updatedAt ?: 0L
                    
                    if (remoteTime > localTime + 2000) {
                        toDownload.add(id to remoteTime)
                    }
                }
            }

            allLocalJournals.forEach { local ->
                val remote = remoteStates[local.id]
                if (remote == null) {
                    toUpload.add(local)
                } else {
                    val remoteTime = remote.second
                    val localTime = local.updatedAt ?: 0L

                    if (localTime > remoteTime + 2000) {
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
                }
            }

            if (isFullRevalidation) {
                _status.value = SyncStatus.Syncing(1f, uploadCount, downloadCount, totalOperations, "Cleaning up cloud media...")
                cleanupCloudOrphanedMedia(provider, allLocalJournals)
            }

            val finalManifest = createCurrentManifest()
            provider.updateManifest(finalManifest).getOrThrow()
            syncPrefs.setLastSyncTime(System.currentTimeMillis())

            _status.value = SyncStatus.Success
            Result.success(Unit)
        } catch (e: Exception) {
            _status.value = SyncStatus.Error(e.message ?: "Sync failed")
            Result.failure(e)
        }
    }

    private suspend fun downloadJournal(id: String, remoteTime: Long): Result<Unit> {
        val provider = getActiveProvider()
        val filename = "$id.json"
        return provider.downloadJournal(filename).onSuccess { journal ->
            val localizedImages = journal.images.map { imgName ->
                File(mediaDir, imgName).absolutePath
            }
            journal.images.forEach { imgName ->
                val targetFile = File(mediaDir, imgName)
                if (!targetFile.exists()) {
                    provider.downloadMedia(imgName, targetFile)
                }
            }
            journalRepo.insertJournal(journal.copy(
                images = localizedImages,
                updatedAt = remoteTime
            ))
        }.map { Unit }
    }

    private suspend fun pushJournal(journal: Journal): Result<Unit> {
        val provider = getActiveProvider()
        journal.images.forEach { localPath ->
            val file = File(localPath)
            if (file.exists()) {
                provider.uploadMedia(file)
            }
        }
        val sanitizedImages = journal.images.map { File(it).name }
        val sanitizedJournal = journal.copy(images = sanitizedImages)

        return provider.uploadJournal(sanitizedJournal).onSuccess { cloudId ->
            journalRepo.updateSyncStatus(journal.id, cloudId, System.currentTimeMillis())
        }.map { Unit }
    }

    private suspend fun cleanupCloudOrphanedMedia(provider: CloudProvider, journals: List<Journal>) {
        try {
            val remoteMedia = provider.listMedia().getOrNull() ?: return
            val localReferencedMedia = journals.flatMap { it.images }.map { File(it).name }.toSet()
            
            val orphans = remoteMedia.filter { it !in localReferencedMedia }
            if (orphans.isNotEmpty()) {
                orphans.forEach { filename ->
                    provider.deleteMedia(filename)
                }
            }
        } catch (e: Exception) {
        }
    }

    private suspend fun createCurrentManifest(): SyncManifest {
        val journals = journalRepo.getAllJournals()
        val total = journals.size
        val totalMedia = journals.flatMap { it.images }.map { File(it).name }.distinct().size
        val devId = syncPrefs.getDeviceId()
        return SyncManifest(
            lastSyncTime = System.currentTimeMillis(),
            lastSyncDeviceId = devId,
            databaseVersion = 3,
            schemaVersion = 1,
            totalJournals = total,
            totalMedia = totalMedia
        )
    }
}
