package com.denser.june.core.data.remote

import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class ItunesTrackData(
    val previewUrl: String?,
    val albumName: String? = null,
    val genre: String? = null
)

data class ItunesSearchResult(
    val previewUrl: String?,
    val albumName: String? = null,
    val genre: String? = null,
    val artworkUrl: String? = null,
    val trackViewUrl: String? = null
)

class ItunesFetcher(private val client: OkHttpClient) {

    suspend fun fetchTrackData(trackId: String): ItunesTrackData? = withContext(Dispatchers.IO) {
        val url = "https://itunes.apple.com/lookup?id=$trackId"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val body = response.body?.string() ?: return@withContext null
                val preview = try {
                    JsonPath.read<String>(body, "$.results[0].previewUrl")
                } catch (_: Exception) {
                    null
                }
                val album = try {
                    JsonPath.read<String>(body, "$.results[0].collectionName")
                } catch (_: Exception) {
                    null
                }
                val genre = try {
                    JsonPath.read<String>(body, "$.results[0].primaryGenreName")
                } catch (_: Exception) {
                    null
                }
                ItunesTrackData(previewUrl = preview, albumName = album, genre = genre)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchPreviewUrl(trackId: String): String? = fetchTrackData(trackId)?.previewUrl

    suspend fun searchTrack(query: String): ItunesSearchResult? = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encoded&media=music&entity=song&limit=1"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val body = response.body?.string() ?: return@withContext null
                val preview = try {
                    JsonPath.read<String>(body, "$.results[0].previewUrl")
                } catch (_: Exception) {
                    null
                }
                val album = try {
                    JsonPath.read<String>(body, "$.results[0].collectionName")
                } catch (_: Exception) {
                    null
                }
                val genre = try {
                    JsonPath.read<String>(body, "$.results[0].primaryGenreName")
                } catch (_: Exception) {
                    null
                }
                val artwork = try {
                    JsonPath.read<String>(body, "$.results[0].artworkUrl100")?.replace("100x100bb", "600x600bb")
                } catch (_: Exception) {
                    null
                }
                val trackViewUrl = try {
                    JsonPath.read<String>(body, "$.results[0].trackViewUrl")
                } catch (_: Exception) {
                    null
                }

                if (preview != null || trackViewUrl != null) {
                    ItunesSearchResult(
                        previewUrl = preview,
                        albumName = album,
                        genre = genre,
                        artworkUrl = artwork,
                        trackViewUrl = trackViewUrl
                    )
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
