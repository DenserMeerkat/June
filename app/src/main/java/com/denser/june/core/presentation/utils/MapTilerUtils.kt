package com.denser.june.core.presentation.utils

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.denser.june.BuildConfig
import com.denser.june.core.domain.data_classes.JournalLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import java.io.IOException
import java.util.Locale

object MapTilerUtils {
    private const val API_KEY = BuildConfig.MAPTILER_API_KEY

    const val STYLE_LIGHT = "https://api.maptiler.com/maps/streets-v2/style.json?key=$API_KEY"
    const val STYLE_DARK = "https://api.maptiler.com/maps/streets-v2-dark/style.json?key=$API_KEY"

    private val client = OkHttpClient()

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun fetchCurrentLocation(context: Context): JournalLocation? {
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val cancellationTokenSource = CancellationTokenSource()

            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            location?.let {
                updateLocationFromCenter(context, LatLng(it.latitude, it.longitude))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun performGetRequest(context: Context, url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", context.packageName)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                return@withContext response.body?.string()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    suspend fun searchLocation(context: Context, query: String): LatLng? {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api.maptiler.com/geocoding/$encodedQuery.json?key=$API_KEY&limit=1"

        val response = performGetRequest(context, url) ?: return null

        return try {
            val json = JSONObject(response)
            val features = json.getJSONArray("features")

            if (features.length() > 0) {
                val feature = features.getJSONObject(0)
                val center = feature.getJSONArray("center")
                LatLng(center.getDouble(1), center.getDouble(0))
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateLocationFromCenter(context: Context, center: LatLng): JournalLocation {
        val url = "https://api.maptiler.com/geocoding/${center.longitude},${center.latitude}.json?key=$API_KEY"

        val response = performGetRequest(context, url)

        return try {
            if (response != null) {
                val json = JSONObject(response)
                val features = json.getJSONArray("features")

                if (features.length() > 0) {
                    val feature = features.getJSONObject(0)
                    val placeName = feature.getString("place_name")
                    val text = feature.optString("text", "Selected Location")

                    var locality = ""
                    val contextArray = feature.optJSONArray("context")
                    if (contextArray != null) {
                        for (i in 0 until contextArray.length()) {
                            val item = contextArray.getJSONObject(i)
                            val id = item.getString("id")
                            if (id.startsWith("place") || id.startsWith("region")) {
                                locality = item.getString("text")
                                break
                            }
                        }
                    }

                    return JournalLocation(
                        latitude = center.latitude,
                        longitude = center.longitude,
                        name = text,
                        address = placeName,
                        locality = locality
                    )
                }
            }
            JournalLocation(center.latitude, center.longitude, name = "Unknown Location", address = generateFallbackLabel(center))
        } catch (e: Exception) {
            e.printStackTrace()
            JournalLocation(center.latitude, center.longitude, name = "Selected Location", address = generateFallbackLabel(center))
        }
    }

    private fun generateFallbackLabel(center: LatLng): String {
        return "Lat: ${"%.4f".format(Locale.US, center.latitude)}, Lon: ${"%.4f".format(Locale.US, center.longitude)}"
    }
}