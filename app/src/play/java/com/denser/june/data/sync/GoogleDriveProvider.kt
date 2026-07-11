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
    private val journalFolderCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val _folderUrl = MutableStateFlow<String?>(null)
    val folderUrl: Flow<String?> = _folderUrl.asStateFlow()

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
            journalFolderCache.clear()
            _folderUrl.value = null
            _isConnected.value = false
        } catch (e: Exception) {
            // Ignore sign-out exceptions
        }
    }

    private suspend fun isFolderTrashed(id: String): Boolean {
        val service = getDriveService() ?: return true
        return try {
            val file = service.files().get(id).setFields("trashed").execute()
            file.trashed ?: false
        } catch (e: Exception) {
            true
        }
    }

    private suspend fun getOrCreateSyncFolder(): Result<String> = withContext(Dispatchers.IO) {
        if (syncFolderId != null && !isFolderTrashed(syncFolderId!!)) return@withContext Result.success(syncFolderId!!)
        val cachedId = syncPrefs.getGoogleDriveSyncFolderId().first()
        if (cachedId != null && !isFolderTrashed(cachedId)) {
            syncFolderId = cachedId
            _folderUrl.value = "https://drive.google.com/drive/folders/$cachedId"
            return@withContext Result.success(cachedId)
        }

        val service = getDriveService() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val result = service.files().list()
                .setQ("mimeType = 'application/vnd.google-apps.folder' and name = 'June' and trashed = false")
                .setFields("files(id)")
                .execute()
            val files = result.files ?: emptyList()
            if (files.size > 1) {
                android.util.Log.w("JuneAuth", "Multiple June folders found in Drive. Picking the first one.")
            }
            val existingId = files.firstOrNull()?.id
            if (existingId != null) {
                syncFolderId = existingId
                syncPrefs.setGoogleDriveSyncFolderId(existingId)
                _folderUrl.value = "https://drive.google.com/drive/folders/$existingId"
                return@withContext Result.success(existingId)
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
            android.util.Log.e("JuneAuth", "Failed to get or create June folder", e)
            Result.failure(e)
        }
    }

    private suspend fun getOrCreateSubfolder(name: String): Result<String> = withContext(Dispatchers.IO) {
        if (name == "journals" && journalsFolderId != null && !isFolderTrashed(journalsFolderId!!)) return@withContext Result.success(journalsFolderId!!)
        if (name == "media" && mediaFolderId != null && !isFolderTrashed(mediaFolderId!!)) return@withContext Result.success(mediaFolderId!!)

        val cachedId = if (name == "journals") syncPrefs.getGoogleDriveJournalsFolderId().first() else syncPrefs.getGoogleDriveMediaFolderId().first()
        if (cachedId != null && !isFolderTrashed(cachedId)) {
            if (name == "journals") journalsFolderId = cachedId
            if (name == "media") mediaFolderId = cachedId
            return@withContext Result.success(cachedId)
        }

        val service = getDriveService() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val rootId = getOrCreateSyncFolder().getOrElse { return@withContext Result.failure(it) }

        try {
            val result = service.files().list()
                .setQ("mimeType = 'application/vnd.google-apps.folder' and '$rootId' in parents and name = '$name' and trashed = false")
                .setFields("files(id)")
                .execute()
            val existingId = result.files?.firstOrNull()?.id
            if (existingId != null) {
                if (name == "journals") {
                    journalsFolderId = existingId
                    syncPrefs.setGoogleDriveJournalsFolderId(existingId)
                }
                if (name == "media") {
                    mediaFolderId = existingId
                    syncPrefs.setGoogleDriveMediaFolderId(existingId)
                }
                return@withContext Result.success(existingId)
            }

            val metadata = DriveFile().apply {
                this.name = name
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(rootId)
            }
            val created = service.files().create(metadata).setFields("id").execute()
            val createdId = created.id ?: throw Exception("Folder creation returned empty ID for subfolder $name")
            if (name == "journals") {
                journalsFolderId = createdId
                syncPrefs.setGoogleDriveJournalsFolderId(createdId)
            }
            if (name == "media") {
                mediaFolderId = createdId
                syncPrefs.setGoogleDriveMediaFolderId(createdId)
            }
            Result.success(createdId)
        } catch (e: Exception) {
            android.util.Log.e("JuneAuth", "Failed to get or create subfolder $name", e)
            Result.failure(e)
        }
    }

    private suspend fun getOrCreateJournalMediaFolder(journalId: String, mediaRootId: String): Result<String> = withContext(Dispatchers.IO) {
        val cachedId = journalFolderCache[journalId]
        if (cachedId != null) return@withContext Result.success(cachedId)

        val service = getDriveService() ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val result = service.files().list()
                .setQ("mimeType = 'application/vnd.google-apps.folder' and '$mediaRootId' in parents and name = '$journalId' and trashed = false")
                .setFields("files(id)")
                .execute()
            val existingId = result.files?.firstOrNull()?.id
            if (existingId != null) {
                journalFolderCache[journalId] = existingId
                return@withContext Result.success(existingId)
            }
            val metadata = DriveFile().apply {
                this.name = journalId
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(mediaRootId)
            }
            val created = service.files().create(metadata).setFields("id").execute()
            val createdId = created.id ?: throw Exception("Folder creation returned empty ID for journal subfolder $journalId")
            journalFolderCache[journalId] = createdId
            Result.success(createdId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun findFileIdByName(name: String, parentFolderId: String): String? = withContext(Dispatchers.IO) {
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
                        // ignore delete failures
                    }
                }
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
            Result.success(executedFile.id ?: "")
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
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Uploading media file ${file.name} for journal $journalId...")
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
        val folderId = getOrCreateJournalMediaFolder(journalId, mediaRootId).getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve subfolder for journal $journalId", it)
            return Result.failure(it)
        }
        val uploadResult = uploadFile(file.name, mimeType, file, folderId)
        if (uploadResult.isSuccess) {
            AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Successfully uploaded media ${file.name}. Deleting legacy duplicate if any.")
            deleteFileByName(file.name, mediaRootId)
        } else {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to upload media file ${file.name}", uploadResult.exceptionOrNull())
        }
        return uploadResult
    }

    override suspend fun downloadMedia(journalId: String, cloudId: String, targetFile: File): Result<File> = withContext(Dispatchers.IO) {
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Downloading media file $cloudId for journal $journalId...")
        val mediaRootId = getOrCreateSubfolder("media").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve media root folder", it)
            return@withContext Result.failure(it)
        }
        
        val journalFolderId = getOrCreateJournalMediaFolder(journalId, mediaRootId).getOrNull()
        val fileBytes = if (journalFolderId != null) {
            downloadFileContent(cloudId, journalFolderId).getOrNull()
        } else {
            null
        }
        
        if (fileBytes != null) {
            try {
                targetFile.parentFile?.mkdirs()
                targetFile.writeBytes(fileBytes)
                AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Successfully downloaded media $cloudId from namespaced folder.")
                Result.success(targetFile)
            } catch (e: Exception) {
                AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to save downloaded media file $cloudId", e)
                Result.failure(e)
            }
        } else {
            AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Media $cloudId not in namespaced folder. Checking legacy root folder...")
            val legacyBytesResult = downloadFileContent(cloudId, mediaRootId)
            if (legacyBytesResult.isSuccess) {
                val bytes = legacyBytesResult.getOrThrow()
                try {
                    targetFile.parentFile?.mkdirs()
                    targetFile.writeBytes(bytes)
                    
                    if (journalFolderId != null) {
                        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Migrating legacy media file $cloudId to namespaced path...")
                        val mimeType = when (targetFile.extension.lowercase()) {
                            "jpg", "jpeg" -> "image/jpeg"
                            "png" -> "image/png"
                            "webp" -> "image/webp"
                            "mp4" -> "video/mp4"
                            else -> "application/octet-stream"
                        }
                        uploadFile(cloudId, mimeType, bytes, journalFolderId)
                        deleteFileByName(cloudId, mediaRootId)
                    }
                    AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Successfully downloaded legacy media $cloudId.")
                    Result.success(targetFile)
                } catch (e: Exception) {
                    AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to write or migrate legacy media $cloudId", e)
                    Result.failure(e)
                }
            } else {
                AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Media $cloudId not found in cloud anywhere.")
                Result.failure(legacyBytesResult.exceptionOrNull() ?: Exception("Media file not found in cloud"))
            }
        }
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
            val result = service.files().list()
                .setQ("'$folderId' in parents and trashed = false")
                .setFields("files(name, modifiedTime)")
                .execute()

            val list = result.files?.filter {
                it.name != null && it.name.endsWith(".json")
            }?.map { file ->
                val lastModified = file.modifiedTime?.value ?: 0L
                RemoteFileMeta(file.name, lastModified)
            } ?: emptyList()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listMedia(): Result<List<String>> = withContext(Dispatchers.IO) {
        val service = getDriveService() ?: return@withContext Result.failure(Exception("Not authenticated"))
        val mediaRootId = getOrCreateSubfolder("media").getOrElse { return@withContext Result.failure(it) }
        try {
            val subdirsResult = service.files().list()
                .setQ("'$mediaRootId' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
                .setFields("files(id, name)")
                .execute()
            val subdirs = subdirsResult.files ?: emptyList()
            
            val allMediaFiles = mutableListOf<String>()
            
            subdirs.forEach { subdir ->
                try {
                    val res = service.files().list()
                        .setQ("'${subdir.id}' in parents and trashed = false")
                        .setFields("files(name)")
                        .execute()
                    res.files?.forEach { file ->
                        if (file.name != null) allMediaFiles.add(file.name)
                    }
                } catch (e: Exception) {
                }
            }
            
            val rootRes = service.files().list()
                .setQ("'$mediaRootId' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false")
                .setFields("files(name)")
                .execute()
            rootRes.files?.forEach { file ->
                if (file.name != null) allMediaFiles.add(file.name)
            }
            
            Result.success(allMediaFiles.distinct())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun deleteMedia(journalId: String, filename: String): Result<Unit> {
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Deleting media file $filename for journal $journalId...")
        val mediaRootId = getOrCreateSubfolder("media").getOrElse {
            AppLogger.e(AppLogger.Category.SYNC, "GoogleDriveProvider", "Failed to resolve media root folder", it)
            return Result.failure(it)
        }
        val journalFolderId = getOrCreateJournalMediaFolder(journalId, mediaRootId).getOrNull()
        if (journalFolderId != null) {
            deleteFileByName(filename, journalFolderId)
        }
        deleteFileByName(filename, mediaRootId)
        AppLogger.d(AppLogger.Category.SYNC, "GoogleDriveProvider", "Successfully requested deletion of media file $filename.")
        return Result.success(Unit)
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
    override fun getContent(): InputStream {
        return response.body.byteStream()
    }

    override fun getContentEncoding(): String? {
        return response.header("Content-Encoding")
    }

    override fun getContentLength(): Long {
        return response.body.contentLength()
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
