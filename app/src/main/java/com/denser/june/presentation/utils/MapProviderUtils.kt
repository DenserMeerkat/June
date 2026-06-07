package com.denser.june.presentation.utils

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import com.denser.june.core.domain.model.JournalLocation
import com.denser.june.core.domain.model.enums.MapStyleProvider
import com.denser.june.core.domain.preferences.JournalPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import java.io.IOException
import java.util.Locale
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.denser.june.core.utils.Constants

object MapProviderUtils : KoinComponent {
    private val client: OkHttpClient by inject()
    private val journalPreferences: JournalPreferences by inject()
    private val context: Context by inject()
    private val locationProvider: LocationProvider by inject()

    fun getStyleUrlWithKeys(
        provider: MapStyleProvider,
        isDark: Boolean,
        mapTilerKey: String,
        stadiaKey: String,
        mapboxKey: String
    ): String {
        return when (provider) {
            MapStyleProvider.MAPTILER -> {
                val styleName = if (isDark) "streets-v4-dark" else "streets-v4"
                "${Constants.MAPTILER_MAPS_BASE_URL}$styleName/style.json?key=$mapTilerKey"
            }
            MapStyleProvider.STADIA -> {
                val styleName = if (isDark) "alidade_smooth_dark" else "alidade_smooth"
                val keyQuery = if (stadiaKey.isNotBlank()) "?api_key=$stadiaKey" else ""
                "${Constants.STADIA_MAPS_BASE_URL}$styleName.json$keyQuery"
            }
            MapStyleProvider.CARTO -> {
                val styleName = if (isDark) "dark-matter-gl-style" else "voyager-gl-style"
                "${Constants.CARTO_MAPS_BASE_URL}$styleName/style.json"
            }
            MapStyleProvider.MAPBOX -> {
                val styleName = if (isDark) "dark-v10" else "streets-v11"
                "${Constants.MAPBOX_MAPS_BASE_URL}$styleName?access_token=$mapboxKey"
            }
        }
    }

    suspend fun getStyleUrl(
        provider: MapStyleProvider,
        isDark: Boolean
    ): String {
        val mapTilerKey = journalPreferences.maptilerKey().first()
        val stadiaKey = journalPreferences.stadiaKey().first()
        val mapboxKey = journalPreferences.mapboxkey().first()
        return getStyleUrlWithKeys(provider, isDark, mapTilerKey, stadiaKey, mapboxKey)
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun fetchCurrentLocation(context: Context): JournalLocation? {
        return locationProvider.fetchCurrentLocation(context)
    }

    private suspend fun performGetRequest(context: Context, url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "JuneApp/${context.packageName}")
                    .header("Referer", "android://${context.packageName}")
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
    }

    suspend fun searchLocation(context: Context, query: String): List<MapSearchResult> {
        val mapTilerKey = journalPreferences.maptilerKey().first()

        if (mapTilerKey.isNotBlank()) {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "${Constants.MAPTILER_GEOCODING_BASE_URL}$encodedQuery.json?key=$mapTilerKey&limit=5"
            val response = performGetRequest(context, url)
            if (response != null) {
                try {
                    val json = JSONObject(response)
                    val features = json.getJSONArray("features")
                    val results = mutableListOf<MapSearchResult>()
                    for (i in 0 until features.length()) {
                        val feature = features.getJSONObject(i)
                        val center = feature.getJSONArray("center")
                        val placeName = feature.getString("place_name")
                        val text = feature.optString("text", "Selected Location")
                        results.add(
                            MapSearchResult(
                                name = text,
                                address = placeName,
                                latitude = center.getDouble(1),
                                longitude = center.getDouble(0)
                            )
                        )
                    }
                    if (results.isNotEmpty()) return results
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "${Constants.NOMINATIM_SEARCH_URL}?q=$encodedQuery&format=json&limit=5"
        val response = performGetRequest(context, url) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(response)
            val results = mutableListOf<MapSearchResult>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val displayName = obj.getString("display_name")
                val parts = displayName.split(",", limit = 2)
                val name = parts.firstOrNull()?.trim() ?: "Selected Location"
                val address = parts.getOrNull(1)?.trim() ?: displayName
                results.add(
                    MapSearchResult(
                        name = name,
                        address = address,
                        latitude = obj.getDouble("lat"),
                        longitude = obj.getDouble("lon")
                    )
                )
            }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun updateLocationFromCenter(context: Context, center: LatLng): JournalLocation {
        val mapTilerKey = journalPreferences.maptilerKey().first()

        if (mapTilerKey.isNotBlank()) {
            val url = "${Constants.MAPTILER_GEOCODING_BASE_URL}${center.longitude},${center.latitude}.json?key=$mapTilerKey"
            val response = performGetRequest(context, url)
            if (response != null) {
                try {
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val url = "${Constants.NOMINATIM_REVERSE_URL}?lat=${center.latitude}&lon=${center.longitude}&format=json"
        val response = performGetRequest(context, url)
        if (response != null) {
            try {
                val json = JSONObject(response)
                val displayName = json.getString("display_name")
                val addressObj = json.optJSONObject("address")
                val name = addressObj?.optString("road")
                    ?: addressObj?.optString("suburb")
                    ?: addressObj?.optString("city")
                    ?: "Selected Location"
                val locality = addressObj?.optString("city")
                    ?: addressObj?.optString("town")
                    ?: addressObj?.optString("village")
                    ?: ""

                return JournalLocation(
                    latitude = center.latitude,
                    longitude = center.longitude,
                    name = name,
                    address = displayName,
                    locality = locality
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return JournalLocation(center.latitude, center.longitude, name = "Unknown Location", address = generateFallbackLabel(center))
    }

    private fun generateFallbackLabel(center: LatLng): String {
        return "Lat: ${"%.4f".format(Locale.US, center.latitude)}, Lon: ${"%.4f".format(Locale.US, center.longitude)}"
    }

    suspend fun testApiKey(provider: MapStyleProvider, key: String): Boolean {
        if (key.isBlank()) return false
        val url = when (provider) {
            MapStyleProvider.MAPTILER -> {
                "${Constants.MAPTILER_MAPS_BASE_URL}streets-v4/style.json?key=$key"
            }
            MapStyleProvider.STADIA -> {
                "${Constants.STADIA_MAPS_BASE_URL}alidade_smooth.json?api_key=$key"
            }
            MapStyleProvider.MAPBOX -> {
                "${Constants.MAPBOX_MAPS_BASE_URL}streets-v11?access_token=$key"
            }
            MapStyleProvider.CARTO -> return true
        }

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "JuneApp/${context.packageName}")
                    .header("Referer", "android://${context.packageName}")
                    .build()
                client.newCall(request).execute().use { response ->
                    android.util.Log.d("MapProviderUtils", "testApiKey [$provider] → HTTP ${response.code}")
                    response.isSuccessful
                }
            } catch (e: Exception) {
                android.util.Log.e("MapProviderUtils", "testApiKey [$provider] → exception: ${e.message}", e)
                false
            }
        }
    }
}
