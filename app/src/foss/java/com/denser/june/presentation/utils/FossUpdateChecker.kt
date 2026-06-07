package com.denser.june.presentation.utils

import android.content.Context
import com.denser.june.BuildConfig
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class FossUpdateChecker(
    private val client: OkHttpClient,
    private val privacyPreferences: PrivacyPreferences
) : UpdateChecker {

    override fun checkForUpdates(
        context: Context,
        onUpdateAvailable: (versionName: String, changelog: String, downloadUrl: String) -> Unit,
        onNoUpdate: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isInternetAllowed = privacyPreferences.getIsInternetAllowedFlow().first()
                if (!isInternetAllowed) {
                    launch(Dispatchers.Main) {
                        onError(InternetDisabledException())
                    }
                    return@launch
                }

                val request = Request.Builder()
                    .url(Constants.GITHUB_RELEASES_LATEST_URL)
                    .header("User-Agent", "JuneApp/${context.packageName}")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code $response")
                    }

                    val bodyString = response.body?.string() ?: throw IOException("Empty response body")
                    val json = JSONObject(bodyString)
                    val latestTag = json.getString("tag_name")
                    val changelog = json.optString("body", "No changelog provided.")
                    val downloadUrl = json.getString("html_url")

                    val currentVersion = BuildConfig.VERSION_NAME

                    launch(Dispatchers.Main) {
                        if (isNewerVersion(currentVersion, latestTag)) {
                            onUpdateAvailable(latestTag, changelog, downloadUrl)
                        } else {
                            onNoUpdate()
                        }
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentClean = current.removePrefix("v").substringBefore("-")
        val latestClean = latest.removePrefix("v").substringBefore("-")

        val currentParts = currentClean.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latestClean.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until length) {
            val currentPart = currentParts.getOrNull(i) ?: 0
            val latestPart = latestParts.getOrNull(i) ?: 0
            if (latestPart > currentPart) return true
            if (currentPart > latestPart) return false
        }
        return false
    }
}
