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

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        val url = syncPrefs.getWebDavUrl().first() ?: return@withContext Result.failure(Exception("URL not set"))
        val user = syncPrefs.getWebDavUsername().first() ?: return@withContext Result.failure(Exception("User not set"))
        val pass = syncPrefs.getWebDavPassword().first() ?: return@withContext Result.failure(Exception("Password not set"))

        val auth = createAuthHeader(user, pass)

        val propfindBody = """
            <?xml version="1.0" encoding="utf-8" ?>
            <d:propfind xmlns:d="DAV:">
                <d:prop>
                    <d:resourcetype/>
                </d:prop>
            </d:propfind>
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", propfindBody.toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .addHeader("Authorization", auth)
            .addHeader("Depth", "0")
            .addHeader("Accept", "application/xml")
            .addHeader("User-Agent", "JuneApp/1.0 (Android)")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    ensureJuneFoldersExist(url, auth)
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

    private suspend fun ensureJuneFoldersExist(baseUrl: String, auth: String) {
        val juneFolder = baseUrl.trimEnd('/') + "/June/"
        if (!checkRemoteResourceExists(juneFolder, auth)) {
            createRemoteFolder(juneFolder, auth)
        }

        val mediaFolder = juneFolder + "media/"
        if (!checkRemoteResourceExists(mediaFolder, auth)) {
            createRemoteFolder(mediaFolder, auth)
        }

        val journalsFolder = juneFolder + "journals/"
        if (!checkRemoteResourceExists(journalsFolder, auth)) {
            createRemoteFolder(journalsFolder, auth)
        }
    }

    private fun checkRemoteResourceExists(path: String, auth: String): Boolean {
        val propfindBody = """
            <?xml version="1.0" encoding="utf-8" ?>
            <d:propfind xmlns:d="DAV:">
                <d:prop>
                    <d:resourcetype/>
                </d:prop>
            </d:propfind>
        """.trimIndent()

        val request = Request.Builder()
            .url(path)
            .method("PROPFIND", propfindBody.toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .addHeader("Authorization", auth)
            .addHeader("Depth", "0")
            .addHeader("Accept", "application/xml")
            .addHeader("User-Agent", "JuneApp/1.0 (Android)")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build()
        return client.newCall(request).execute().use { it.isSuccessful }
    }

    private fun createRemoteFolder(path: String, auth: String) {
        val request = Request.Builder()
            .url(path)
            .method("MKCOL", null)
            .addHeader("Authorization", auth)
            .addHeader("Content-Type", "application/xml; charset=utf-8")
            .addHeader("User-Agent", "JuneApp/1.0 (Android)")
            .addHeader("X-Requested-With", "XMLHttpRequest")
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
        val baseUrl = syncPrefs.getWebDavUrl().first() ?: return@withContext Result.failure(Exception("URL not set"))
        val user = syncPrefs.getWebDavUsername().first() ?: ""
        val pass = syncPrefs.getWebDavPassword().first() ?: ""
        val auth = createAuthHeader(user, pass)

        val journalFileName = "${journal.id}.json"
        val journalUrl = "${baseUrl.trimEnd('/')}/June/journals/$journalFileName"
        val content = json.encodeToString(Journal.serializer(), journal)

        val request = Request.Builder()
            .url(journalUrl)
            .put(content.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", auth)
            .addHeader("User-Agent", "JuneApp/1.0 (Android)")
            .addHeader("X-Requested-With", "XMLHttpRequest")
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
        val baseUrl = syncPrefs.getWebDavUrl().first() ?: ""
        val user = syncPrefs.getWebDavUsername().first() ?: ""
        val pass = syncPrefs.getWebDavPassword().first() ?: ""
        val auth = createAuthHeader(user, pass)

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/June/journals/$cloudId")
            .addHeader("Authorization", auth)
            .addHeader("User-Agent", "JuneApp/1.0 (Android)")
            .addHeader("X-Requested-With", "XMLHttpRequest")
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
        val baseUrl = syncPrefs.getWebDavUrl().first() ?: ""
        val user = syncPrefs.getWebDavUsername().first() ?: ""
        val pass = syncPrefs.getWebDavPassword().first() ?: ""
        val auth = createAuthHeader(user, pass)
        
        val mediaUrl = "${baseUrl.trimEnd('/')}/June/media/${file.name}"
        val request = Request.Builder()
            .url(mediaUrl)
            .put(file.asRequestBody("application/octet-stream".toMediaType()))
            .addHeader("Authorization", auth)
            .addHeader("User-Agent", "JuneApp/1.0 (Android)")
            .addHeader("X-Requested-With", "XMLHttpRequest")
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
        val baseUrl = syncPrefs.getWebDavUrl().first() ?: ""
        val user = syncPrefs.getWebDavUsername().first() ?: ""
        val pass = syncPrefs.getWebDavPassword().first() ?: ""
        val auth = createAuthHeader(user, pass)

        val mediaUrl = "${baseUrl.trimEnd('/')}/June/media/$cloudId"
        val request = Request.Builder()
            .url(mediaUrl)
            .addHeader("Authorization", auth)
            .addHeader("User-Agent", "JuneApp/1.0 (Android)")
            .addHeader("X-Requested-With", "XMLHttpRequest")
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
        val baseUrl = syncPrefs.getWebDavUrl().first() ?: ""
        val user = syncPrefs.getWebDavUsername().first() ?: ""
        val pass = syncPrefs.getWebDavPassword().first() ?: ""
        val auth = createAuthHeader(user, pass)

        val content = json.encodeToString(SyncManifest.serializer(), manifest)
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/June/manifest.json")
            .put(content.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", auth)
            .addHeader("User-Agent", "JuneApp/1.0 (Android)")
            .addHeader("X-Requested-With", "XMLHttpRequest")
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
        val baseUrl = syncPrefs.getWebDavUrl().first() ?: ""
        val user = syncPrefs.getWebDavUsername().first() ?: ""
        val pass = syncPrefs.getWebDavPassword().first() ?: ""
        val auth = createAuthHeader(user, pass)

        val propfindBody = """
            <?xml version="1.0" encoding="utf-8" ?>
            <d:propfind xmlns:d="DAV:">
                <d:prop>
                    <d:displayname/>
                    <d:getlastmodified/>
                    <d:resourcetype/>
                </d:prop>
            </d:propfind>
        """.trimIndent()

        val journalsFolder = baseUrl.trimEnd('/') + "/June/journals/"
        val request = Request.Builder()
            .url(journalsFolder)
            .method("PROPFIND", propfindBody.toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .addHeader("Authorization", auth)
            .addHeader("Depth", "1")
            .addHeader("Accept", "application/xml")
            .addHeader("User-Agent", "JuneApp/1.0 (Android)")
            .addHeader("X-Requested-With", "XMLHttpRequest")
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
        val baseUrl = syncPrefs.getWebDavUrl().first() ?: ""
        val user = syncPrefs.getWebDavUsername().first() ?: ""
        val pass = syncPrefs.getWebDavPassword().first() ?: ""
        val auth = createAuthHeader(user, pass)

        val propfindBody = """
            <?xml version="1.0" encoding="utf-8" ?>
            <d:propfind xmlns:d="DAV:">
                <d:prop>
                    <d:displayname/>
                    <d:getlastmodified/>
                    <d:resourcetype/>
                </d:prop>
            </d:propfind>
        """.trimIndent()

        val mediaFolder = baseUrl.trimEnd('/') + "/June/media/"
        val request = Request.Builder()
            .url(mediaFolder)
            .method("PROPFIND", propfindBody.toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .addHeader("Authorization", auth)
            .addHeader("Depth", "1")
            .addHeader("Accept", "application/xml")
            .addHeader("User-Agent", "JuneApp/1.0 (Android)")
            .addHeader("X-Requested-With", "XMLHttpRequest")
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
        val baseUrl = syncPrefs.getWebDavUrl().first() ?: ""
        val user = syncPrefs.getWebDavUsername().first() ?: ""
        val pass = syncPrefs.getWebDavPassword().first() ?: ""
        val auth = createAuthHeader(user, pass)

        val mediaUrl = "${baseUrl.trimEnd('/')}/June/media/$filename"
        val request = Request.Builder()
            .url(mediaUrl)
            .delete()
            .addHeader("Authorization", auth)
            .addHeader("User-Agent", "JuneApp/1.0 (Android)")
            .addHeader("X-Requested-With", "XMLHttpRequest")
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
        val baseUrl = syncPrefs.getWebDavUrl().first() ?: ""
        val user = syncPrefs.getWebDavUsername().first() ?: ""
        val pass = syncPrefs.getWebDavPassword().first() ?: ""
        val auth = createAuthHeader(user, pass)

        val journalUrl = "${baseUrl.trimEnd('/')}/June/journals/$cloudId"
        val request = Request.Builder()
            .url(journalUrl)
            .delete()
            .addHeader("Authorization", auth)
            .addHeader("User-Agent", "JuneApp/1.0 (Android)")
            .addHeader("X-Requested-With", "XMLHttpRequest")
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
