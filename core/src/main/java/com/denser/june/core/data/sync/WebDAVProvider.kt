package com.denser.june.core.data.sync

import android.util.Base64
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.preferences.SyncPreferences
import com.denser.june.core.domain.sync.CloudProvider
import com.denser.june.core.domain.sync.SyncManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import okio.buffer
import okio.sink
import com.denser.june.core.domain.sync.RemoteFileMeta

class WebDAVProvider(
    private val client: OkHttpClient,
    private val syncPrefs: SyncPreferences
) : CloudProvider {

    override val name: String = "WebDAV"
    private val _isConnected = MutableStateFlow(false)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private companion object {
        const val USER_AGENT = "June/0.6.0 (Android)"
        const val XML_PROPFIND_BODY = """
            <?xml version="1.0" encoding="utf-8" ?>
            <d:propfind xmlns:d="DAV:">
                <d:prop>
                    <d:resourcetype/>
                    <d:displayname/>
                    <d:getlastmodified/>
                </d:prop>
            </d:propfind>
        """
    }

    private data class WebDavAuth(val baseUrl: String, val auth: String)

    private suspend fun getAuth(): WebDavAuth? {
        val url = syncPrefs.getWebDavUrl().first() ?: return null
        val user = syncPrefs.getWebDavUsername().first() ?: ""
        val pass = syncPrefs.getWebDavPassword().first() ?: ""
        val auth = createAuthHeader(user, pass)
        return WebDavAuth(url, auth)
    }

    private fun Request.Builder.webDavHeaders(auth: String, depth: String? = "0") = apply {
        header("Authorization", auth)
        header("User-Agent", USER_AGENT)
        header("X-Requested-With", "XMLHttpRequest")
        depth?.let { header("Depth", it) }
    }

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))

        val request = Request.Builder()
            .url(authInfo.baseUrl)
            .method("PROPFIND", XML_PROPFIND_BODY.trimIndent().toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .webDavHeaders(authInfo.auth, depth = "0")
            .header("Accept", "application/xml")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    ensureJuneFoldersExist(authInfo)
                    _isConnected.value = true
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Auth failed: ${response.code}"))
                }
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    private fun ensureJuneFoldersExist(authInfo: WebDavAuth) {
        val juneFolder = authInfo.baseUrl.trimEnd('/') + "/June/"
        if (!checkRemoteResourceExists(juneFolder, authInfo.auth)) {
            createRemoteFolder(juneFolder, authInfo.auth)
        }

        val mediaFolder = juneFolder + "media/"
        if (!checkRemoteResourceExists(mediaFolder, authInfo.auth)) {
            createRemoteFolder(mediaFolder, authInfo.auth)
        }

        val journalsFolder = juneFolder + "journals/"
        if (!checkRemoteResourceExists(journalsFolder, authInfo.auth)) {
            createRemoteFolder(journalsFolder, authInfo.auth)
        }
    }

    private fun checkRemoteResourceExists(path: String, auth: String): Boolean {
        val request = Request.Builder()
            .url(path)
            .method("PROPFIND", XML_PROPFIND_BODY.trimIndent().toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .webDavHeaders(auth, depth = "0")
            .header("Accept", "application/xml")
            .build()
        return client.newCall(request).execute().use { it.isSuccessful }
    }

    private fun createRemoteFolder(path: String, auth: String) {
        val request = Request.Builder()
            .url(path)
            .method("MKCOL", null)
            .header("Content-Type", "application/xml; charset=utf-8")
            .webDavHeaders(auth, depth = null)
            .build()
        client.newCall(request).execute().use { }
    }

    private fun createAuthHeader(user: String, pass: String): String {
        val credentials = "$user:$pass"
        return "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
    }

    override fun isConnected(): Flow<Boolean> = _isConnected

    override suspend fun disconnect() {
        _isConnected.value = false
        syncPrefs.setSelectedProvider(null)
    }

    override suspend fun uploadJournal(journal: Journal): Result<String> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))

        val journalFileName = "${journal.id}.json"
        val journalUrl = "${authInfo.baseUrl.trimEnd('/')}/June/journals/$journalFileName"
        val content = json.encodeToString(Journal.serializer(), journal)

        val request = Request.Builder()
            .url(journalUrl)
            .put(content.toRequestBody("application/json".toMediaType()))
            .webDavHeaders(authInfo.auth, depth = null)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(journalFileName) 
                else Result.failure(Exception("Upload failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadJournal(cloudId: String): Result<Journal> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))

        val request = Request.Builder()
            .url("${authInfo.baseUrl.trimEnd('/')}/June/journals/$cloudId")
            .webDavHeaders(authInfo.auth, depth = null)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use Result.failure(Exception("Empty body"))
                    Result.success(json.decodeFromString(Journal.serializer(), body))
                } else {
                    Result.failure(Exception("Download failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadMedia(file: File): Result<String> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))
        
        val mediaUrl = "${authInfo.baseUrl.trimEnd('/')}/June/media/${file.name}"
        val request = Request.Builder()
            .url(mediaUrl)
            .put(file.asRequestBody("application/octet-stream".toMediaType()))
            .webDavHeaders(authInfo.auth, depth = null)
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(file.name)
                else Result.failure(Exception("Media upload failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadMedia(cloudId: String, targetFile: File): Result<File> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))

        val mediaUrl = "${authInfo.baseUrl.trimEnd('/')}/June/media/$cloudId"
        val request = Request.Builder()
            .url(mediaUrl)
            .webDavHeaders(authInfo.auth, depth = null)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.source()?.let { source ->
                        targetFile.parentFile?.mkdirs()
                        targetFile.sink().buffer().use { it.writeAll(source) }
                        Result.success(targetFile)
                    } ?: Result.failure(Exception("Empty media response"))
                } else {
                    Result.failure(Exception("Media download failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateManifest(manifest: SyncManifest): Result<Unit> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))

        val content = json.encodeToString(SyncManifest.serializer(), manifest)
        val request = Request.Builder()
            .url("${authInfo.baseUrl.trimEnd('/')}/June/manifest.json")
            .put(content.toRequestBody("application/json".toMediaType()))
            .webDavHeaders(authInfo.auth, depth = null)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Manifest update failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listJournals(): Result<List<RemoteFileMeta>> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))

        val journalsFolder = authInfo.baseUrl.trimEnd('/') + "/June/journals/"
        val request = Request.Builder()
            .url(journalsFolder)
            .method("PROPFIND", XML_PROPFIND_BODY.trimIndent().toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .webDavHeaders(authInfo.auth, depth = "1")
            .header("Accept", "application/xml")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val files = mutableListOf<RemoteFileMeta>()

                    val responseRegex = Regex("<[^>]*?response>(.*?)</[^>]*?response>", RegexOption.DOT_MATCHES_ALL)
                    val nameRegex = Regex("<[^>:]*?:?displayname>([^<>]*?\\.json)</[^>:]*?:?displayname>", RegexOption.IGNORE_CASE)
                    val dateRegex = Regex("<[^>:]*?:?getlastmodified>([^<>]*?)</[^>:]*?:?getlastmodified>", RegexOption.IGNORE_CASE)
                    

                    responseRegex.findAll(body).forEach { responseMatch ->
                        val block = responseMatch.groupValues[1]
                        val nameMatch = nameRegex.find(block)
                        val dateMatch = dateRegex.find(block)
                        
                        if (nameMatch != null) {
                            val name = nameMatch.groupValues[1].trim()
                            val dateStr = dateMatch?.groupValues[1]?.trim()
                            val timestamp = parseHttpDate(dateStr)
                            files.add(RemoteFileMeta(name, timestamp))
                        }
                    }
                    Result.success(files.distinctBy { it.name })
                } else {
                    Result.failure(Exception("List journals failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseHttpDate(dateStr: String?): Long {
        if (dateStr == null) return 0L
        val trimmed = dateStr.trim()

        try {
            val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
            format.timeZone = TimeZone.getTimeZone("GMT")
            return format.parse(trimmed)?.time ?: 0L
        } catch (e: Exception) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                return format.parse(trimmed)?.time ?: 0L
            } catch (e2: Exception) {
                return 0L
            }
        }
    }
    override suspend fun listMedia(): Result<List<String>> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))

        val mediaFolder = authInfo.baseUrl.trimEnd('/') + "/June/media/"
        val request = Request.Builder()
            .url(mediaFolder)
            .method("PROPFIND", XML_PROPFIND_BODY.trimIndent().toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .webDavHeaders(authInfo.auth, depth = "1")
            .header("Accept", "application/xml")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val filenames = mutableListOf<String>()
                    val regex = Regex("<[^>:]*?:?displayname>([^<>]+?\\.[^<>]+?)</[^>:]*?:?displayname>", RegexOption.IGNORE_CASE)
                    regex.findAll(body).forEach { match ->
                        val name = match.groupValues[1].trim()
                        filenames.add(name)
                    }
                    Result.success(filenames.distinct())
                } else {
                    Result.failure(Exception("List media failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMedia(filename: String): Result<Unit> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))

        val mediaUrl = "${authInfo.baseUrl.trimEnd('/')}/June/media/$filename"
        val request = Request.Builder()
            .url(mediaUrl)
            .delete()
            .webDavHeaders(authInfo.auth, depth = null)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404) Result.success(Unit)
                else Result.failure(Exception("Delete media failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteJournal(cloudId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))

        val journalUrl = "${authInfo.baseUrl.trimEnd('/')}/June/journals/$cloudId"
        val request = Request.Builder()
            .url(journalUrl)
            .delete()
            .webDavHeaders(authInfo.auth, depth = null)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404) Result.success(Unit)
                else Result.failure(Exception("Delete journal failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
