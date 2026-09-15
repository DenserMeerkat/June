package com.denser.june.core.data.remote

import com.denser.june.core.data.dto.SongLinkPageData
import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * Redirect chain followed by OkHttp:
 *   https://song.link/<streaming-url>
 *     → 308 → https://song.link/https:/<path>
 *     → 302 → https://song.link/s/<id>   ← HTML contains __NEXT_DATA__
 */
class SongLinkScraper(private val client: OkHttpClient) {

    suspend fun fetchPageData(streamingUrl: String): SongLinkPageData? =
        withContext(Dispatchers.IO) {
            val targetUrl = "https://song.link/${streamingUrl.trim()}"
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Pixel 4) AppleWebKit/537.36")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val html = response.body?.string() ?: return@withContext null
                    parsePageData(html)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private fun parsePageData(html: String): SongLinkPageData? {
        return try {
            val document = Jsoup.parse(html)
            val nextDataScript = document.getElementById("__NEXT_DATA__")
                ?.html()
                ?: return null

            val pageData = JsonPath.read<Map<String, Any>>(
                nextDataScript,
                "$.props.pageProps.pageData"
            ) ?: return null

            val entityData = (pageData["entityData"] as? Map<*, *>) ?: return null
            val title = entityData["title"] as? String ?: return null
            val artistName = entityData["artistName"] as? String ?: return null
            val thumbnailUrl = entityData["thumbnailUrl"] as? String
            val pageUrl = pageData["pageUrl"] as? String ?: return null

            val sections = pageData["sections"] as? List<*> ?: emptyList<Any>()
            val listenSection = sections
                .filterIsInstance<Map<*, *>>()
                .firstOrNull { (it["sectionId"] as? String)?.contains("links|listen") == true }

            val links = (listenSection?.get("links") as? List<*>)
                ?.filterIsInstance<Map<*, *>>()
                ?: emptyList()

            fun linkUrl(platform: String) = links
                .firstOrNull { it["platform"] == platform }
                ?.get("url") as? String

            fun linkUniqueId(platform: String) = links
                .firstOrNull { it["platform"] == platform }
                ?.get("uniqueId") as? String

            SongLinkPageData(
                title = title,
                artistName = artistName,
                thumbnailUrl = thumbnailUrl,
                pageUrl = pageUrl,
                spotifyUrl = linkUrl("spotify"),
                spotifyUniqueId = linkUniqueId("spotify"),
                appleMusicUrl = linkUrl("appleMusic"),
                youtubeMusicUrl = linkUrl("youtubeMusic"),
                youtubeUrl = linkUrl("youtube"),
                deezerUrl = linkUrl("deezer"),
                deezerUniqueId = linkUniqueId("deezer"),
                tidalUrl = linkUrl("tidal"),
                amazonMusicUrl = linkUrl("amazonMusic"),
                soundcloudUrl = linkUrl("soundcloud")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
