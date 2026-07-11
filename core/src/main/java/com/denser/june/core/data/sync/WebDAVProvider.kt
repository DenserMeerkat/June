package com.denser.june.core.data.sync
 
import android.content.Context

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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.denser.june.core.domain.sync.serialize
import com.denser.june.core.domain.sync.deserializeJournal
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import okio.buffer
import okio.sink
import com.denser.june.core.domain.sync.RemoteFileMeta

class WebDAVProvider(
    private val context: Context,
    private val client: OkHttpClient,
    private val syncPrefs: SyncPreferences
) : CloudProvider {

    override val name: String = "WebDAV"
    private val _isConnected = MutableStateFlow(false)

    private companion object {
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
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "Unknown"
        }
        header("User-Agent", "June/$versionName (Android)")
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
                    val folderResult = ensureJuneFoldersExist(authInfo)
                    if (folderResult.isFailure) {
                        return@withContext Result.failure(folderResult.exceptionOrNull() ?: Exception("Failed to setup folders"))
                    }
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

    private fun ensureJuneFoldersExist(authInfo: WebDavAuth): Result<Unit> {
        val juneFolder = authInfo.baseUrl.trimEnd('/') + "/June/"
        if (!checkRemoteResourceExists(juneFolder, authInfo.auth)) {
            val result = createRemoteFolder(juneFolder, authInfo.auth)
            if (result.isFailure) return result
        }

        val mediaFolder = juneFolder + "media/"
        if (!checkRemoteResourceExists(mediaFolder, authInfo.auth)) {
            val result = createRemoteFolder(mediaFolder, authInfo.auth)
            if (result.isFailure) return result
        }

        val journalsFolder = juneFolder + "journals/"
        if (!checkRemoteResourceExists(journalsFolder, authInfo.auth)) {
            val result = createRemoteFolder(journalsFolder, authInfo.auth)
            if (result.isFailure) return result
        }
        
        return Result.success(Unit)
    }

    private fun checkRemoteResourceExists(path: String, auth: String): Boolean {
        val request = Request.Builder()
            .url(path)
            .method("PROPFIND", XML_PROPFIND_BODY.trimIndent().toRequestBody("application/xml; charset=utf-8".toMediaType()))
            .webDavHeaders(auth, depth = "0")
            .header("Accept", "application/xml")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val isXml = response.body?.contentType()?.toString()?.contains("xml", ignoreCase = true) == true
                        || response.header("Content-Type")?.contains("xml", ignoreCase = true) == true
                
                response.isSuccessful && (response.code == 207 || (response.code == 200 && isXml))
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun createRemoteFolder(path: String, auth: String): Result<Unit> {
        val request = Request.Builder()
            .url(path)
            .method("MKCOL", null)
            .header("Content-Type", "application/xml; charset=utf-8")
            .webDavHeaders(auth, depth = null)
            .build()
            
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 405) {
                    Result.success(Unit)
                } else if (path.endsWith("/")) {
                    val fallbackPath = path.trimEnd('/')
                    val fallbackRequest = request.newBuilder().url(fallbackPath).build()
                    client.newCall(fallbackRequest).execute().use { fbResponse ->
                        if (fbResponse.isSuccessful || fbResponse.code == 405) {
                            Result.success(Unit)
                        } else {
                            Result.failure(Exception("Failed to create folder $path: ${fbResponse.code}"))
                        }
                    }
                } else {
                    Result.failure(Exception("Failed to create folder $path: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        val content = journal.serialize()

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
                    Result.success(body.deserializeJournal())
                } else {
                    Result.failure(Exception("Download failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadMedia(journalId: String, file: File): Result<String> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))
        
        val folderUrl = "${authInfo.baseUrl.trimEnd('/')}/June/media/$journalId/"
        if (!checkRemoteResourceExists(folderUrl, authInfo.auth)) {
            val createRes = createRemoteFolder(folderUrl, authInfo.auth)
            if (createRes.isFailure) return@withContext Result.failure(createRes.exceptionOrNull() ?: Exception("Failed to create namespaced subfolder"))
        }

        val mediaUrl = "$folderUrl${file.name}"
        val request = Request.Builder()
            .url(mediaUrl)
            .put(file.asRequestBody("application/octet-stream".toMediaType()))
            .webDavHeaders(authInfo.auth, depth = null)
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val legacyUrl = "${authInfo.baseUrl.trimEnd('/')}/June/media/${file.name}"
                    val deleteRequest = Request.Builder()
                        .url(legacyUrl)
                        .delete()
                        .webDavHeaders(authInfo.auth, depth = null)
                        .build()
                    try {
                        client.newCall(deleteRequest).execute().close()
                    } catch (e: Exception) {
                        // ignore delete failures
                    }
                    Result.success(file.name)
                } else {
                    Result.failure(Exception("Media upload failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadMedia(journalId: String, cloudId: String, targetFile: File): Result<File> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))

        val namespacedUrl = "${authInfo.baseUrl.trimEnd('/')}/June/media/$journalId/$cloudId"
        var request = Request.Builder()
            .url(namespacedUrl)
            .webDavHeaders(authInfo.auth, depth = null)
            .get()
            .build()

        var isSuccess = false
        var isLegacy = false

        try {
            val response = client.newCall(request).execute()
            var activeResponse = response
            if (!activeResponse.isSuccessful) {
                activeResponse.close()
                val legacyUrl = "${authInfo.baseUrl.trimEnd('/')}/June/media/$cloudId"
                request = Request.Builder()
                    .url(legacyUrl)
                    .webDavHeaders(authInfo.auth, depth = null)
                    .get()
                    .build()
                activeResponse = client.newCall(request).execute()
                isLegacy = true
            }

            activeResponse.use { res ->
                if (res.isSuccessful) {
                    res.body?.source()?.let { source ->
                        targetFile.parentFile?.mkdirs()
                        targetFile.sink().buffer().use { it.writeAll(source) }
                        isSuccess = true
                    }
                }
            }

            if (isSuccess && isLegacy) {
                val folderUrl = "${authInfo.baseUrl.trimEnd('/')}/June/media/$journalId/"
                if (!checkRemoteResourceExists(folderUrl, authInfo.auth)) {
                    createRemoteFolder(folderUrl, authInfo.auth)
                }

                val putRequest = Request.Builder()
                    .url(namespacedUrl)
                    .put(targetFile.asRequestBody("application/octet-stream".toMediaType()))
                    .webDavHeaders(authInfo.auth, depth = null)
                    .build()
                client.newCall(putRequest).execute().use { putRes ->
                    if (putRes.isSuccessful) {
                        val deleteRequest = Request.Builder()
                            .url("${authInfo.baseUrl.trimEnd('/')}/June/media/$cloudId")
                            .delete()
                            .webDavHeaders(authInfo.auth, depth = null)
                            .build()
                        try {
                            client.newCall(deleteRequest).execute().close()
                        } catch (e: Exception) {
                            // ignore delete failures
                        }
                    }
                }
            }

            if (isSuccess) {
                Result.success(targetFile)
            } else {
                Result.failure(Exception("Media download failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    override suspend fun updateManifest(manifest: SyncManifest): Result<Unit> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))

        val content = manifest.serialize()
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

    override suspend fun getManifest(): Result<SyncManifest?> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))

        val request = Request.Builder()
            .url("${authInfo.baseUrl.trimEnd('/')}/June/manifest.json")
            .webDavHeaders(authInfo.auth, depth = null)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use Result.failure(Exception("Empty manifest response"))
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; coerceInputValues = true }
                    Result.success(json.decodeFromString<SyncManifest>(body))
                } else if (response.code == 404) {
                    Result.success(null)
                } else {
                    Result.failure(Exception("Manifest get failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseWebDavPropfind(xmlInput: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        try {
            val parser = android.util.Xml.newPullParser()
            parser.setInput(xmlInput.reader())
            var eventType = parser.eventType
            var currentHref = ""
            var currentLastModified = ""
            
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        if (name.equals("href", ignoreCase = true) || name.endsWith(":href", ignoreCase = true)) {
                            currentHref = parser.nextText().trim()
                        } else if (name.equals("getlastmodified", ignoreCase = true) || name.endsWith(":getlastmodified", ignoreCase = true)) {
                            currentLastModified = parser.nextText().trim()
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        if (name.equals("response", ignoreCase = true) || name.endsWith(":response", ignoreCase = true)) {
                            if (currentHref.isNotBlank()) {
                                results.add(currentHref to currentLastModified)
                            }
                            currentHref = ""
                            currentLastModified = ""
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
        }
        return results
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
                    val parsed = parseWebDavPropfind(body)
                    val files = parsed.mapNotNull { (href, dateStr) ->
                        val decodedHref = try {
                            java.net.URLDecoder.decode(href, "UTF-8")
                        } catch (e: Exception) {
                            href
                        }
                        val filename = File(decodedHref).name
                        if (filename.endsWith(".json", ignoreCase = true)) {
                            val timestamp = parseHttpDate(dateStr)
                            RemoteFileMeta(filename, timestamp)
                        } else {
                            null
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
                    val parsed = parseWebDavPropfind(body)
                    
                    val subdirs = parsed.mapNotNull { (href, _) ->
                        val decodedHref = try {
                            java.net.URLDecoder.decode(href, "UTF-8")
                        } catch (e: Exception) {
                            href
                        }
                        val cleanHref = decodedHref.trimEnd('/')
                        val parentFolder = File(cleanHref).parentFile?.name ?: ""
                        if (parentFolder.equals("media", ignoreCase = true)) {
                            cleanHref + "/"
                        } else {
                            null
                        }
                    }.distinct()

                    val allMediaFiles = mutableListOf<String>()
                    
                    subdirs.forEach { subdirUrl ->
                        val resolvedUrl = try {
                            val baseHttpUrl = authInfo.baseUrl.toHttpUrlOrNull()
                            baseHttpUrl?.resolve(subdirUrl)?.toString() ?: subdirUrl
                        } catch (e: Exception) {
                            subdirUrl
                        }



                        val subReq = Request.Builder()
                            .url(resolvedUrl)
                            .method("PROPFIND", XML_PROPFIND_BODY.trimIndent().toRequestBody("application/xml; charset=utf-8".toMediaType()))
                            .webDavHeaders(authInfo.auth, depth = "1")
                            .header("Accept", "application/xml")
                            .build()
                        try {
                            client.newCall(subReq).execute().use { subRes ->
                                if (subRes.isSuccessful) {
                                    val subBody = subRes.body?.string() ?: ""
                                    val subParsed = parseWebDavPropfind(subBody)
                                    subParsed.forEach { (subHref, _) ->
                                        val subDecoded = try {
                                            java.net.URLDecoder.decode(subHref, "UTF-8")
                                        } catch (e: Exception) {
                                            subHref
                                        }
                                        val filename = File(subDecoded).name
                                        if (filename.isNotBlank() && !filename.equals("media", ignoreCase = true) && !filename.equals(File(subdirUrl).name, ignoreCase = true)) {
                                            allMediaFiles.add(filename)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                        }
                    }


                    parsed.forEach { (href, _) ->
                        val decodedHref = try {
                            java.net.URLDecoder.decode(href, "UTF-8")
                        } catch (e: Exception) {
                            href
                        }
                        val filename = File(decodedHref).name
                        if (filename.isNotBlank() && filename.contains(".") && !filename.equals("media", ignoreCase = true)) {
                            allMediaFiles.add(filename)
                        }
                    }

                    Result.success(allMediaFiles.distinct())
                } else {
                    Result.failure(Exception("List media failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMedia(journalId: String, filename: String): Result<Unit> = withContext(Dispatchers.IO) {
        val authInfo = getAuth() ?: return@withContext Result.failure(Exception("Missing WebDAV credentials"))

        val namespacedUrl = "${authInfo.baseUrl.trimEnd('/')}/June/media/$journalId/$filename"
        val request = Request.Builder()
            .url(namespacedUrl)
            .delete()
            .webDavHeaders(authInfo.auth, depth = null)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 404) {
                    val legacyUrl = "${authInfo.baseUrl.trimEnd('/')}/June/media/$filename"
                    val legacyReq = Request.Builder().url(legacyUrl).delete().webDavHeaders(authInfo.auth, depth = null).build()
                    try {
                        client.newCall(legacyReq).execute().close()
                    } catch (e: Exception) {}
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Delete media failed: ${response.code}"))
                }
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
