package com.example.june.core.presentation.utils

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.*
import com.example.june.core.domain.data_classes.JournalLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import java.net.URL
import java.util.Locale

@Composable
fun rememberManagedOsmMapView(
    initialLocation: JournalLocation? = null,
    debounceTime: Long = 800L,
    context: Context,
    isDarkMode: Boolean = false,
    isTerrainMode: Boolean = false,
    onMapStateChange: (isMoving: Boolean) -> Unit = {},
    onCenterChanged: (GeoPoint) -> Unit = {}
): MapView {
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            minZoomLevel = 3.0

            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)

            val startZoom = if (initialLocation != null) 17.0 else 3.0
            val startPoint = initialLocation?.let { GeoPoint(it.latitude, it.longitude) }
                ?: GeoPoint(0.0, 0.0)

            controller.setZoom(startZoom)
            controller.setCenter(startPoint)
        }
    }

    LaunchedEffect(isTerrainMode) {
        val newSource = if (isTerrainMode) {
            TileSourceFactory.USGS_SAT
        } else {
            TileSourceFactory.MAPNIK
        }

        if (mapView.tileProvider.tileSource != newSource) {
            mapView.setTileSource(newSource)
        }
    }

    LaunchedEffect(isDarkMode) {
        if (isDarkMode) {
            mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        } else {
            mapView.overlayManager.tilesOverlay.setColorFilter(null)
        }
        mapView.invalidate()
    }

    DisposableEffect(mapView) {
        var debounceJob: Job? = null
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                onMapStateChange(true)
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(debounceTime)
                    onMapStateChange(false)
                    onCenterChanged(mapView.mapCenter as GeoPoint)
                }
                return true
            }
            override fun onZoom(event: ZoomEvent?): Boolean = true
        }
        mapView.addMapListener(listener)
        onDispose {
            debounceJob?.cancel()
            mapView.removeMapListener(listener)
            mapView.onDetach()
        }
    }
    return mapView
}

object OsmMapUtils {

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
                updateLocationFromCenter(GeoPoint(it.latitude, it.longitude))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun searchNominatim(query: String): GeoPoint? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=1"
            val connection = URL(url).openConnection()
            connection.setRequestProperty("User-Agent", "JuneApp/1.0")
            connection.connectTimeout = 5000
            val response = connection.getInputStream().bufferedReader().readText()
            val jsonArray = JSONArray(response)
            if (jsonArray.length() > 0) {
                val obj = jsonArray.getJSONObject(0)
                GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"))
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateLocationFromCenter(center: GeoPoint): JournalLocation = withContext(Dispatchers.IO) {
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?lat=${center.latitude}&lon=${center.longitude}&format=json"
            val connection = URL(url).openConnection()
            connection.setRequestProperty("User-Agent", "JuneApp/1.0")
            connection.connectTimeout = 5000
            val response = connection.getInputStream().bufferedReader().readText()
            val json = JSONObject(response)
            val addressObj = json.optJSONObject("address")

            val road = addressObj?.optString("road")
            val neighbourhood = addressObj?.optString("neighbourhood")
            val suburb = addressObj?.optString("suburb")
            val city = addressObj?.optString("city")
            val town = addressObj?.optString("town")
            val village = addressObj?.optString("village")
            val state = addressObj?.optString("state")
            val country = addressObj?.optString("country")

            val primaryLocation = road ?: neighbourhood ?: suburb ?: "Selected Location"
            val locality = city ?: town ?: village ?: state
            val addressParts = listOfNotNull(road, neighbourhood ?: suburb, city ?: town ?: village, state, country).distinct()

            JournalLocation(
                latitude = center.latitude,
                longitude = center.longitude,
                name = primaryLocation,
                address = if (addressParts.isNotEmpty()) addressParts.joinToString(", ") else generateFallbackLabel(center),
                locality = locality
            )
        } catch (e: Exception) {
            JournalLocation(center.latitude, center.longitude, name = "Selected Location", address = generateFallbackLabel(center))
        }
    }

    private fun generateFallbackLabel(center: GeoPoint): String {
        return "Lat: ${"%.4f".format(Locale.US, center.latitude)}, Lon: ${"%.4f".format(Locale.US, center.longitude)}"
    }
}