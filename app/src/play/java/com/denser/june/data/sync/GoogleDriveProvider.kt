@file:Suppress("DEPRECATION")

package com.denser.june.data.sync

import android.content.Context
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.preferences.SyncPreferences
import com.denser.june.core.domain.sync.CloudProvider
import com.denser.june.core.domain.sync.RemoteFileMeta
import com.denser.june.core.domain.sync.SyncManifest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.LowLevelHttpRequest
import com.google.api.client.http.LowLevelHttpResponse
import com.google.api.client.util.StreamingContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import com.google.api.client.http.ByteArrayContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.core.domain.sync.serialize
import com.denser.june.core.domain.sync.deserializeJournal
import com.google.api.client.googleapis.json.GoogleJsonResponseException

class GoogleDriveProvider(
    private val context: Context,
    private val syncPrefs: SyncPreferences,
    private val okHttpClient: OkHttpClient
) : CloudProvider {
    override val name: String = "GoogleDrive"

    private val _isConnected = MutableStateFlow(false)
    private var driveService: Drive? = null
    private var syncFolderId: String? = null
    private var journalsFolderId: String? = null
    private var mediaFolderId: String? = null
    private var songsFolderId: String? = null
    private var audiosFolderId: String? = null
    private val folderMutex = Mutex()
    private val journalFolderCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val folderValidityCache = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val folderFilesCache = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>>()
    private val _folderUrl = MutableStateFlow<String?>(null)
    override val folderUrl: Flow<String?> = _folderUrl.asStateFlow()

    init {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            setupDriveService(account)
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                getOrCreateSyncFolder()
            }
        }
    }

    private fun setupDriveService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf("https://www.googleapis.com/auth/drive.file")
        )
        credential.selectedAccount = account.account

        val transport = OkHttp3Transport(okHttpClient)
        driveService = Drive.Builder(
            transport,
            GsonFactory.getDefaultInstance(),
            credential
        )
        .setApplicationName("June")
        .build()

        _isConnected.value = true
    }

    private fun getDriveService(): Drive? {
        if (driveService != null) return driveService
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        setupDriveService(account)
        return driveService
    }

    fun handleSignIn(account: GoogleSignInAccount) {
        setupDriveService(account)
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            getOrCreateSyncFolder()
        }
    }

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Connecting to Google Drive...")
        if (getDriveService() != null) {
            _isConnected.value = true
            AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Successfully authenticated and connected to Google Drive.")
            Result.success(Unit)
        } else {
            _isConnected.value = false
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to connect: Google Sign-In required")
            Result.failure(Exception("Google Sign-In required"))
        }
    }

    override fun isConnected(): Flow<Boolean> {
        return _isConnected.asStateFlow()
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
                .build()
            val client = GoogleSignIn.getClient(context, gso)
            client.signOut()
            driveService = null
            syncFolderId = null
            journalsFolderId = null
            mediaFolderId = null
            songsFolderId = null
            audiosFolderId = null
            journalFolderCache.clear()
            folderValidityCache.clear()
            folderFilesCache.clear()
            _folderUrl.value = null
            _isConnected.value = false
        } catch (e: Exception) {
            // Ignore sign-out exceptions
        }
    }

    private fun isFolderTrashed(id: String): Boolean {
        val now = System.currentTimeMillis()
        val validUntil = folderValidityCache[id]
        if (validUntil != null && now < validUntil) return false
        val service = getDriveService() ?: return true
        return try {
            val file = service.files().get(id).setFields("trashed").execute()
            val trashed = file.trashed ?: false
            if (!trashed) folderValidityCache[id] = now + 60_000L
            trashed
        } catch (e: GoogleJsonResponseException) {
            folderValidityCache.remove(id)
            if (e.statusCode == 404) true else false
        } catch (e: Exception) {
            folderValidityCache.remove(id)
            false
        }
    }

    override suspend fun reconnectFolder(): Result<Unit> = withContext(Dispatchers.IO) {
        folderMutex.withLock {
            syncFolderId = null
            journalsFolderId = null
            mediaFolderId = null
            songsFolderId = null
            audiosFolderId = null
            journalFolderCache.clear()
            folderValidityCache.clear()
            folderFilesCache.clear()
            _folderUrl.value = null
            syncPrefs.setGoogleDriveSyncFolderId(null)
            syncPrefs.setGoogleDriveJournalsFolderId(null)
            syncPrefs.setGoogleDriveMediaFolderId(null)
            syncPrefs.setGoogleDriveSongsFolderId(null)
            syncPrefs.setGoogleDriveAudiosFolderId(null)
            getOrCreateSyncFolderLocked().map { Unit }
        }
    }

    private suspend fun getOrCreateSyncFolder(): Result<String> = withContext(Dispatchers.IO) {
        if (syncFolderId != null && !isFolderTrashed(syncFolderId!!)) return@withContext Result.success(syncFolderId!!)
        folderMutex.withLock {
            getOrCreateSyncFolderLocked()
        }
    }

    private suspend fun getOrCreateSyncFolderLocked(): Result<String> {
        if (syncFolderId != null && !isFolderTrashed(syncFolderId!!)) return Result.success(syncFolderId!!)
        val cachedId = syncPrefs.getGoogleDriveSyncFolderId().first()
        if (cachedId != null && !isFolderTrashed(cachedId)) {
            syncFolderId = cachedId
            _folderUrl.value = "https://drive.google.com/drive/folders/$cachedId"
            return Result.success(cachedId)
        }

        val service = getDriveService() ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val result = service.files().list()
                .setQ("mimeType = 'application/vnd.google-apps.folder' and name = 'June' and trashed = false")
                .setFields("files(id, createdTime)")
                .execute()
            val files = result.files ?: emptyList()
            if (files.size > 1) {
                AppLogger.w(AppLogger.Category.SYNC, "GoogleDriveProvider", "Multiple June folders found (${files.size}). Inspecting candidates...")
                var bestFolderId = files.first().id
                for (candidate in files) {
                    val candidateId = candidate.id ?: continue
                    val checkManifest = service.files().list()
                        .setQ("'$candidateId' in parents and name = 'sync_manifest.json' and trashed = false")
                        .setFields("files(id)")
                        .execute()
                    if (!checkManifest.files.isNullOrEmpty()) {
                        bestFolderId = candidateId
                        break
                    }
                }
                syncFolderId = bestFolderId
                syncPrefs.setGoogleDriveSyncFolderId(bestFolderId)
                _folderUrl.value = "https://drive.google.com/drive/folders/$bestFolderId"
                return Result.success(bestFolderId)
            }
            val existingId = files.firstOrNull()?.id
            if (existingId != null) {
                syncFolderId = existingId
                syncPrefs.setGoogleDriveSyncFolderId(existingId)
                _folderUrl.value = "https://drive.google.com/drive/folders/$existingId"
                return Result.success(existingId)
            }

            val metadata = DriveFile().apply {
                name = "June"
                mimeType = "application/vnd.google-apps.folder"
            }
            val created = service.files().create(metadata).setFields("id").execute()
            val createdId = created.id ?: throw Exception("Folder creation returned empty ID")
            syncFolderId = createdId
            syncPrefs.setGoogleDriveSyncFolderId(createdId)
            _folderUrl.value = "https://drive.google.com/drive/folders/$createdId"
            Result.success(createdId)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to get or create June folder", e)
            Result.failure(e)
        }
    }

    private suspend fun getOrCreateSubfolder(name: String): Result<String> = withContext(Dispatchers.IO) {
        val normalizedName = when (name) {
            "song_media" -> "songs"
            "journal_audio" -> "audios"
            else -> name
        }
        if (normalizedName == "journals" && journalsFolderId != null && !isFolderTrashed(journalsFolderId!!)) return@withContext Result.success(journalsFolderId!!)
        if (normalizedName == "media" && mediaFolderId != null && !isFolderTrashed(mediaFolderId!!)) return@withContext Result.success(mediaFolderId!!)
        if (normalizedName == "songs" && songsFolderId != null && !isFolderTrashed(songsFolderId!!)) return@withContext Result.success(songsFolderId!!)
        if (normalizedName == "audios" && audiosFolderId != null && !isFolderTrashed(audiosFolderId!!)) return@withContext Result.success(audiosFolderId!!)

        folderMutex.withLock {
            if (normalizedName == "journals" && journalsFolderId != null && !isFolderTrashed(journalsFolderId!!)) return@withLock Result.success(journalsFolderId!!)
            if (normalizedName == "media" && mediaFolderId != null && !isFolderTrashed(mediaFolderId!!)) return@withLock Result.success(mediaFolderId!!)
            if (normalizedName == "songs" && songsFolderId != null && !isFolderTrashed(songsFolderId!!)) return@withLock Result.success(songsFolderId!!)
            if (normalizedName == "audios" && audiosFolderId != null && !isFolderTrashed(audiosFolderId!!)) return@withLock Result.success(audiosFolderId!!)

            val cachedId = when (normalizedName) {
                "journals" -> syncPrefs.getGoogleDriveJournalsFolderId().first()
                "media" -> syncPrefs.getGoogleDriveMediaFolderId().first()
                "songs" -> syncPrefs.getGoogleDriveSongsFolderId().first()
                "audios" -> syncPrefs.getGoogleDriveAudiosFolderId().first()
                else -> null
            }
            if (cachedId != null && !isFolderTrashed(cachedId)) {
                when (normalizedName) {
                    "journals" -> journalsFolderId = cachedId
                    "media" -> mediaFolderId = cachedId
                    "songs" -> songsFolderId = cachedId
                    "audios" -> audiosFolderId = cachedId
                }
                return@withLock Result.success(cachedId)
            }

            val service = getDriveService() ?: return@withLock Result.failure(Exception("Not authenticated"))
            val rootId = getOrCreateSyncFolderLocked().getOrElse { return@withLock Result.failure(it) }

            try {
                val queryName = when (normalizedName) {
                    "songs" -> "(name = 'songs' or name = 'song_media')"
                    "audios" -> "(name = 'audios' or name = 'journal_audio')"
                    else -> "name = '$normalizedName'"
                }
                val result = service.files().list()
                    .setQ("mimeType = 'application/vnd.google-apps.folder' and '$rootId' in parents and $queryName and trashed = false")
                    .setFields("files(id, name)")
                    .execute()
                val files = result.files ?: emptyList()
                val existingFolder = when (normalizedName) {
                    "songs" -> files.find { it.name == "songs" } ?: files.find { it.name == "song_media" }
                    "audios" -> files.find { it.name == "audios" } ?: files.find { it.name == "journal_audio" }
                    else -> files.firstOrNull()
                }
                if (existingFolder != null && existingFolder.id != null) {
                    val folderId = existingFolder.id
                    if (existingFolder.name != normalizedName) {
                        try {
                            val updateMeta = DriveFile().apply { this.name = normalizedName }
                            service.files().update(folderId, updateMeta).execute()
                        } catch (e: Exception) {
                        }
                    }
                    when (normalizedName) {
                        "journals" -> {
                            journalsFolderId = folderId
                            syncPrefs.setGoogleDriveJournalsFolderId(folderId)
                        }
                        "media" -> {
                            mediaFolderId = folderId
                            syncPrefs.setGoogleDriveMediaFolderId(folderId)
                        }
                        "songs" -> {
                            songsFolderId = folderId
                            syncPrefs.setGoogleDriveSongsFolderId(folderId)
                        }
                        "audios" -> {
                            audiosFolderId = folderId
                            syncPrefs.setGoogleDriveAudiosFolderId(folderId)
                        }
                    }
                    return@withLock Result.success(folderId)
                }

                val metadata = DriveFile().apply {
                    this.name = normalizedName
                    mimeType = "application/vnd.google-apps.folder"
                    parents = listOf(rootId)
                }
                val created = service.files().create(metadata).setFields("id").execute()
                val createdId = created.id ?: throw Exception("Folder creation returned empty ID for subfolder $normalizedName")
                when (normalizedName) {
                    "journals" -> {
                        journalsFolderId = createdId
                        syncPrefs.setGoogleDriveJournalsFolderId(createdId)
                    }
                    "media" -> {
                        mediaFolderId = createdId
                        syncPrefs.setGoogleDriveMediaFolderId(createdId)
                    }
                    "songs" -> {
                        songsFolderId = createdId
                        syncPrefs.setGoogleDriveSongsFolderId(createdId)
                    }
                    "audios" -> {
                        audiosFolderId = createdId
                        syncPrefs.setGoogleDriveAudiosFolderId(createdId)
                    }
                }
                Result.success(createdId)
            } catch (e: Exception) {
                android.util.Log.e("JuneAuth", "Failed to get or create subfolder $normalizedName", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun findFileIdByName(name: String, parentFolderId: String): String? = withContext(Dispatchers.IO) {
        val cachedMap = folderFilesCache[parentFolderId]
        if (cachedMap != null && cachedMap.containsKey(name)) {
            return@withContext cachedMap[name]
        }
        val service = getDriveService() ?: return@withContext null
        try {
            val result = service.files().list()
                .setQ("'$parentFolderId' in parents and name = '$name' and trashed = false")
                .setFields("files(id, name, modifiedTime)")
                .execute()
            val files = result.files ?: emptyList()
            if (files.isEmpty()) return@withContext null
            val sorted = files.sortedByDescending { it.modifiedTime?.value ?: 0L }
            val newest = sorted.first()
            if (sorted.size > 1) {
                for (dup in sorted.drop(1)) {
                    try {
                        service.files().delete(dup.id).execute()
                    } catch (e: Exception) {
                    }
                }
            }
            if (newest.id != null) {
                folderFilesCache.getOrPut(parentFolderId) { java.util.concurrent.ConcurrentHashMap() }[name] = newest.id
            }
            newest.id
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun uploadFile(name: String, mimeType: String, content: ByteArray, parentFolderId: String): Result<String> = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val fileId = findFileIdByName(name, parentFolderId)
            val metadata = DriveFile().apply {
                this.name = name
                if (fileId == null) {
                    parents = listOf(parentFolderId)
                }
            }
            val mediaContent = ByteArrayContent(mimeType, content)
            val executedFile = if (fileId != null) {
                service.files().update(fileId, metadata, mediaContent).execute()
            } else {
                service.files().create(metadata, mediaContent).execute()
            }
            executedFile.id?.let { id ->
                folderFilesCache.getOrPut(parentFolderId) { java.util.concurrent.ConcurrentHashMap() }[name] = id
            }
            Result.success(executedFile.id ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadFile(name: String, mimeType: String, file: File, parentFolderId: String): Result<String> = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val fileId = findFileIdByName(name, parentFolderId)
            val metadata = DriveFile().apply {
                this.name = name
                if (fileId == null) {
                    parents = listOf(parentFolderId)
                }
            }
            val mediaContent = com.google.api.client.http.FileContent(mimeType, file)
            val executedFile = if (fileId != null) {
                service.files().update(fileId, metadata, mediaContent).execute()
            } else {
                service.files().create(metadata, mediaContent).execute()
            }
            executedFile.id?.let { id ->
                folderFilesCache.getOrPut(parentFolderId) { java.util.concurrent.ConcurrentHashMap() }[name] = id
            }
            Result.success(executedFile.id ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun downloadFileTo(name: String, parentFolderId: String, targetFile: File): Result<File> = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val fileId = findFileIdByName(name, parentFolderId) ?: return@withContext Result.failure(Exception("File not found: $name"))
            targetFile.parentFile?.mkdirs()
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
            tempFile.outputStream().use { os ->
                service.files().get(fileId).executeMediaAndDownloadTo(os)
            }
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun downloadFileContent(name: String, parentFolderId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val fileId = findFileIdByName(name, parentFolderId) ?: return@withContext Result.failure(Exception("File not found: $name"))
            val outputStream = ByteArrayOutputStream()
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            Result.success(outputStream.toByteArray())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deleteFileByName(name: String, parentFolderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val fileId = findFileIdByName(name, parentFolderId)
            if (fileId != null) {
                service.files().delete(fileId).execute()
                folderFilesCache[parentFolderId]?.remove(name)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadJournal(journal: Journal): Result<String> {
        val filename = "${journal.id}.json"
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Uploading journal $filename to Drive...")
        val jsonStr = journal.serialize()
        val folderId = getOrCreateSubfolder("journals").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve parent journals folder", it)
            return Result.failure(it)
        }
        return uploadFile(filename, "application/json", jsonStr.toByteArray(Charsets.UTF_8), folderId).onSuccess {
            AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Successfully uploaded journal $filename. Drive File ID: $it")
        }.onFailure {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to upload journal $filename", it)
        }
    }

    override suspend fun downloadJournal(cloudId: String): Result<Journal> {
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Downloading journal $cloudId from Drive...")
        val folderId = getOrCreateSubfolder("journals").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve parent journals folder", it)
            return Result.failure(it)
        }
        return downloadFileContent(cloudId, folderId).map { bytes ->
            val jsonStr = String(bytes, Charsets.UTF_8)
            jsonStr.deserializeJournal()
        }.onSuccess {
            AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Successfully downloaded journal $cloudId.")
        }.onFailure {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to download journal $cloudId", it)
        }
    }

    override suspend fun uploadMedia(journalId: String, file: File): Result<String> {
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Uploading media file ${file.name} to flat pool...")
        val mimeType = when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            else -> "application/octet-stream"
        }
        val mediaRootId = getOrCreateSubfolder("media").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve media root folder", it)
            return Result.failure(it)
        }
        return uploadFile(file.name, mimeType, file, mediaRootId)
    }

    override suspend fun downloadMedia(journalId: String, cloudId: String, targetFile: File): Result<File> = withContext(Dispatchers.IO) {
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Downloading media file $cloudId from flat pool...")
        val mediaRootId = getOrCreateSubfolder("media").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve media root folder", it)
            return@withContext Result.failure(it)
        }
        downloadFileTo(cloudId, mediaRootId, targetFile)
    }

    override suspend fun updateManifest(manifest: SyncManifest): Result<Unit> {
        val jsonStr = manifest.serialize()
        val rootId = getOrCreateSyncFolder().getOrElse { return Result.failure(it) }
        return uploadFile("sync_manifest.json", "application/json", jsonStr.toByteArray(Charsets.UTF_8), rootId).map { Unit }
    }

    override suspend fun getManifest(): Result<SyncManifest?> = withContext(Dispatchers.IO) {
        val rootId = getOrCreateSyncFolder().getOrElse { return@withContext Result.failure(it) }
        downloadFileContent("sync_manifest.json", rootId).map { bytes ->
            val jsonStr = String(bytes, Charsets.UTF_8)
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; coerceInputValues = true }
            json.decodeFromString<SyncManifest>(jsonStr)
        }.recover { exception ->
            if (exception.message?.contains("File not found") == true) {
                null
            } else {
                throw exception
            }
        }
    }

    override suspend fun listJournals(): Result<List<RemoteFileMeta>> = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val folderId = getOrCreateSubfolder("journals").getOrElse { return@withContext Result.failure(it) }
        try {
            val list = mutableListOf<RemoteFileMeta>()
            val cache = folderFilesCache.getOrPut(folderId) { java.util.concurrent.ConcurrentHashMap() }
            var pageToken: String? = null
            do {
                val result = service.files().list()
                    .setQ("'$folderId' in parents and trashed = false")
                    .setFields("nextPageToken, files(id, name, modifiedTime)")
                    .setPageSize(1000)
                    .setPageToken(pageToken)
                    .execute()
                result.files?.forEach { file ->
                    if (file.name != null && file.name.endsWith(".json")) {
                        if (file.id != null) cache[file.name] = file.id
                        val lastModified = file.modifiedTime?.value ?: 0L
                        list.add(RemoteFileMeta(file.name, lastModified))
                    }
                }
                pageToken = result.nextPageToken
            } while (pageToken != null)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listMedia(): Result<List<String>> = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val mediaRootId = getOrCreateSubfolder("media").getOrElse { return@withContext Result.failure(it) }
        try {
            val list = mutableListOf<String>()
            val cache = folderFilesCache.getOrPut(mediaRootId) { java.util.concurrent.ConcurrentHashMap() }
            var pageToken: String? = null
            do {
                val result = service.files().list()
                    .setQ("'$mediaRootId' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageSize(1000)
                    .setPageToken(pageToken)
                    .execute()
                result.files?.forEach { file ->
                    if (file.name != null) {
                        if (file.id != null) cache[file.name] = file.id
                        list.add(file.name)
                    }
                }
                pageToken = result.nextPageToken
            } while (pageToken != null)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMedia(journalId: String, filename: String): Result<Unit> {
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Deleting media file $filename from flat pool...")
        val mediaRootId = getOrCreateSubfolder("media").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve media root folder", it)
            return Result.failure(it)
        }
        return deleteFileByName(filename, mediaRootId)
    }

    override suspend fun uploadSongMedia(file: File): Result<String> {
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Uploading song media file ${file.name} to flat song pool...")
        val mimeType = when (file.extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
        val folderId = getOrCreateSubfolder("songs").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve songs root folder", it)
            return Result.failure(it)
        }
        return uploadFile(file.name, mimeType, file, folderId)
    }

    override suspend fun downloadSongMedia(filename: String, targetFile: File): Result<File> = withContext(Dispatchers.IO) {
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Downloading song media file $filename from flat song pool...")
        val folderId = getOrCreateSubfolder("songs").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve songs root folder", it)
            return@withContext Result.failure(it)
        }
        downloadFileTo(filename, folderId, targetFile)
    }

    override suspend fun listSongMedia(): Result<List<String>> = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val folderId = getOrCreateSubfolder("songs").getOrElse { return@withContext Result.failure(it) }
        try {
            val list = mutableListOf<String>()
            val cache = folderFilesCache.getOrPut(folderId) { java.util.concurrent.ConcurrentHashMap() }
            var pageToken: String? = null
            do {
                val result = service.files().list()
                    .setQ("'$folderId' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageSize(1000)
                    .setPageToken(pageToken)
                    .execute()
                result.files?.forEach { file ->
                    if (file.name != null) {
                        if (file.id != null) cache[file.name] = file.id
                        list.add(file.name)
                    }
                }
                pageToken = result.nextPageToken
            } while (pageToken != null)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSongMedia(filename: String): Result<Unit> {
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Deleting song media file $filename from flat song pool...")
        val folderId = getOrCreateSubfolder("songs").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve songs folder", it)
            return Result.failure(it)
        }
        return deleteFileByName(filename, folderId)
    }

    override suspend fun uploadAudio(file: File): Result<String> {
        val folderId = getOrCreateSubfolder("audios").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve audios folder", it)
            return Result.failure(it)
        }
        return uploadFile(file.name, "audio/m4a", file, folderId)
    }

    override suspend fun downloadAudio(filename: String, targetFile: File): Result<File> = withContext(Dispatchers.IO) {
        val folderId = getOrCreateSubfolder("audios").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve audios folder", it)
            return@withContext Result.failure(it)
        }
        downloadFileTo(filename, folderId, targetFile)
    }

    override suspend fun listAudios(): Result<List<String>> = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val folderId = getOrCreateSubfolder("audios").getOrElse { return@withContext Result.failure(it) }
        try {
            val list = mutableListOf<String>()
            val cache = folderFilesCache.getOrPut(folderId) { java.util.concurrent.ConcurrentHashMap() }
            var pageToken: String? = null
            do {
                val result = service.files().list()
                    .setQ("'$folderId' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageSize(1000)
                    .setPageToken(pageToken)
                    .execute()
                result.files?.forEach { file ->
                    if (file.name != null) {
                        if (file.id != null) cache[file.name] = file.id
                        list.add(file.name)
                    }
                }
                pageToken = result.nextPageToken
            } while (pageToken != null)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAudio(filename: String): Result<Unit> {
        val folderId = getOrCreateSubfolder("audios").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve audios folder", it)
            return Result.failure(it)
        }
        return deleteFileByName(filename, folderId)
    }

    override suspend fun deleteJournal(cloudId: String): Result<Unit> {
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Deleting journal file $cloudId...")
        val folderId = getOrCreateSubfolder("journals").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve journals folder", it)
            return Result.failure(it)
        }
        return deleteFileByName(cloudId, folderId).onSuccess {
            AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Successfully requested deletion of journal $cloudId.")
        }.onFailure {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to delete journal $cloudId", it)
        }
    }
}

class OkHttp3Transport(private val client: OkHttpClient) : HttpTransport() {
    override fun supportsMethod(method: String): Boolean = true

    override fun buildRequest(method: String, url: String): LowLevelHttpRequest {
        return OkHttp3Request(client, method, url)
    }
}

class OkHttp3Request(
    private val client: OkHttpClient,
    private val method: String,
    private val url: String
) : LowLevelHttpRequest() {
    private val headersBuilder = Headers.Builder()
    private var connectTimeoutMs = 0
    private var readTimeoutMs = 0

    override fun addHeader(name: String, value: String) {
        headersBuilder.add(name, value)
    }

    override fun setTimeout(connectTimeout: Int, readTimeout: Int) {
        this.connectTimeoutMs = connectTimeout
        this.readTimeoutMs = readTimeout
    }

    override fun execute(): LowLevelHttpResponse {
        val encoding = contentEncoding
        if (encoding != null) {
            headersBuilder.set("Content-Encoding", encoding)
        }
        val requestBuilder = Request.Builder()
            .url(url)
            .headers(headersBuilder.build())

        val body = if (method == "GET" || method == "HEAD") {
            null
        } else {
            val streamingContent = streamingContent
            if (streamingContent != null) {
                StreamingRequestBody(contentType, contentLength, streamingContent)
            } else if (method == "POST" || method == "PUT" || method == "PATCH") {
                ByteArray(0).toRequestBody(null)
            } else {
                null
            }
        }

        requestBuilder.method(method, body)

        var callClient = client
        if (connectTimeoutMs > 0 || readTimeoutMs > 0) {
            val builder = client.newBuilder()
            if (connectTimeoutMs > 0) {
                builder.connectTimeout(connectTimeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            }
            if (readTimeoutMs > 0) {
                builder.readTimeout(readTimeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            }
            callClient = builder.build()
        }

        val response = callClient.newCall(requestBuilder.build()).execute()
        return OkHttp3Response(response)
    }
}

class StreamingRequestBody(
    private val contentTypeStr: String?,
    private val contentLengthVal: Long,
    private val streamingContent: StreamingContent
) : RequestBody() {
    override fun contentType(): okhttp3.MediaType? {
        return contentTypeStr?.toMediaTypeOrNull()
    }

    override fun contentLength(): Long {
        return contentLengthVal
    }

    override fun writeTo(sink: BufferedSink) {
        streamingContent.writeTo(sink.outputStream())
    }
}

class OkHttp3Response(private val response: Response) : LowLevelHttpResponse() {
    override fun getContent(): InputStream? {
        return response.body?.byteStream()
    }

    override fun getContentEncoding(): String? {
        return response.header("Content-Encoding")
    }

    override fun getContentLength(): Long {
        return response.body?.contentLength() ?: -1L
    }

    override fun getContentType(): String? {
        return response.header("Content-Type")
    }

    override fun getStatusCode(): Int {
        return response.code
    }

    override fun getStatusLine(): String {
        val protocolStr = response.protocol.toString().uppercase()
        return "$protocolStr ${response.code} ${response.message}"
    }

    override fun getReasonPhrase(): String {
        return response.message
    }

    override fun getHeaderCount(): Int {
        return response.headers.size
    }

    override fun getHeaderName(index: Int): String {
        return response.headers.name(index)
    }

    override fun getHeaderValue(index: Int): String {
        return response.headers.value(index)
    }

    override fun disconnect() {
        response.close()
    }
}
