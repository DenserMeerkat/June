package com.denser.june.core.data.remote

import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class DeezerTrackData(
    val previewUrl: String?,
    val albumName: String? = null
)

data class DeezerSearchResult(
    val previewUrl: String?,
    val albumName: String? = null,
    val coverUrl: String? = null,
    val trackUrl: String? = null
)

class DeezerFetcher(private val client: OkHttpClient) {

    suspend fun fetchTrackData(trackId: String): DeezerTrackData? = withContext(Dispatchers.IO) {
        val url = "https://api.deezer.com/track/$trackId"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val body = response.body?.string() ?: return@withContext null
                val preview = try {
                    JsonPath.read<String>(body, "$.preview")
                } catch (_: Exception) {
                    null
                }
                val album = try {
                    JsonPath.read<String>(body, "$.album.title")
                } catch (_: Exception) {
                    null
                }
                DeezerTrackData(previewUrl = preview, albumName = album)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchPreviewUrl(trackId: String): String? = fetchTrackData(trackId)?.previewUrl

    suspend fun searchTrack(query: String): DeezerSearchResult? = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api.deezer.com/search?q=$encoded&limit=1"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val body = response.body?.string() ?: return@withContext null
                val preview = try {
                    JsonPath.read<String>(body, "$.data[0].preview")
                } catch (_: Exception) {
                    null
                }
                val album = try {
                    JsonPath.read<String>(body, "$.data[0].album.title")
                } catch (_: Exception) {
                    null
                }
                val cover = try {
                    JsonPath.read<String>(body, "$.data[0].album.cover_big")
                } catch (_: Exception) {
                    null
                }
                val trackUrl = try {
                    JsonPath.read<String>(body, "$.data[0].link")
                } catch (_: Exception) {
                    null
                }

                if (preview != null || trackUrl != null) {
                    DeezerSearchResult(previewUrl = preview, albumName = album, coverUrl = cover, trackUrl = trackUrl)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
