package com.denser.june.core.data.remote

import com.jayway.jsonpath.JsonPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class SpotifyScraper(private val client: OkHttpClient) {

    suspend fun fetchPreviewUrl(trackId: String): String? = withContext(Dispatchers.IO) {
        val embedUrl = "https://open.spotify.com/embed/track/$trackId"

        val request = Request.Builder()
            .url(embedUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val html = response.body?.string() ?: return@withContext null
                val document = Jsoup.parse(html)
                val scriptElements = document.getElementsByTag("script")

                for (element in scriptElements) {
                    val scriptContent = element.html()
                    if (scriptContent.contains("audioPreview")) {
                        return@withContext extractUrlFromJson(scriptContent)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun extractUrlFromJson(jsonString: String): String? {
        return try {
            val query = "$..audioPreview.url"
            when (val result = JsonPath.read<Any>(jsonString, query)) {
                is List<*> -> if (result.isNotEmpty()) result[0].toString() else null
                is String -> result
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}