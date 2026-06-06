package com.denser.june.presentation.screens.home.timeline.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.enums.MapStyleProvider
import com.denser.june.core.domain.model.enums.MapTheme
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.presentation.components.InternetRestrictedIndicator
import com.denser.june.presentation.components.MapAttributions
import com.denser.june.presentation.components.MapControlColumn
import com.denser.june.presentation.components.MapLibreInitializer
import com.denser.june.presentation.components.rememberMapDarkMode
import com.denser.june.presentation.components.MapViewLifecycleEffect
import com.denser.june.presentation.screens.home.timeline.TimelineVM
import com.denser.june.presentation.theme.LocalInternetAllowed
import com.denser.june.presentation.utils.MapProviderUtils
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import com.denser.june.core.utils.toDayOfMonth
import com.denser.june.core.utils.toShortMonth
import com.denser.june.presentation.utils.TimelineMapUtils

@Composable
fun TimelineMapTab(
    journals: List<Journal>,
    bottomPadding: Dp,
    viewModel: TimelineVM = koinViewModel()
) {
    val isInternetAllowed = LocalInternetAllowed.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isCalendarExpanded by viewModel.isCalendarExpanded.collectAsStateWithLifecycle()
    val isMapExpanded = !isCalendarExpanded

    val journalPreferences = koinInject<JournalPreferences>()
    val savedMapTheme by journalPreferences.mapTheme()
        .collectAsStateWithLifecycle(initialValue = MapTheme.APP)
    val mapStyleProvider by journalPreferences.mapStyleProvider()
        .collectAsStateWithLifecycle(initialValue = MapStyleProvider.MAPTILER)

    MapLibreInitializer(isInternetAllowed)

    val validPoints = remember(journals) {
        journals.filter { it.location?.let { loc -> loc.latitude != 0.0 && loc.longitude != 0.0 } == true }
    }

    val sortedPoints = remember(validPoints) {
        validPoints.sortedBy { it.dateTime }
    }

    val locationGroups = remember(sortedPoints) {
        TimelineMapUtils.buildLocationGroups(sortedPoints)
    }

    var selectedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(sortedPoints) {
        selectedIndex = 0
    }

    var isDarkMap by rememberMapDarkMode(savedMapTheme)

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()
    val mutedLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f).toArgb()

    val styleUrl by produceState(initialValue = "", mapStyleProvider, isDarkMap) {
        value = MapProviderUtils.getStyleUrl(mapStyleProvider, isDarkMap)
    }
    val mapView = remember { MapView(context).apply { isClickable = true; isFocusable = true } }

    val dateBubbleCache = remember { mutableMapOf<String, org.maplibre.android.annotations.Icon>() }

    fun getMarkerIcon(text: String): org.maplibre.android.annotations.Icon {
        return dateBubbleCache.getOrPut(text) {
            val bitmap = TimelineMapUtils.createMarkerBitmap(context, text, primaryColor, onPrimaryColor)
            IconFactory.getInstance(context).fromBitmap(bitmap)
        }
    }

    LaunchedEffect(selectedIndex, sortedPoints) {
        val target = sortedPoints.getOrNull(selectedIndex)?.location ?: return@LaunchedEffect
        mapView.getMapAsync { map ->
            map.animateCamera(
                org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(target.latitude, target.longitude))
                        .zoom(15.0)
                        .build()
                ),
                800
            )
        }
    }

    fun renderMapContent(mapboxMap: MapLibreMap, style: Style) {
        mapboxMap.clear()
        val pathPoints = locationGroups.map { LatLng(it.location.latitude, it.location.longitude) }

        val lines = if (pathPoints.size > 1) {
            (0 until pathPoints.size - 1).map { i ->
                val arcPoints = TimelineMapUtils.createArchedPoints(pathPoints[i], pathPoints[i + 1])
                LineString.fromLngLats(arcPoints.map { Point.fromLngLat(it.longitude, it.latitude) })
            }
        } else emptyList()

        val featureCollection = FeatureCollection.fromFeatures(lines.map { Feature.fromGeometry(it) })
        val sourceId = "timeline-source"
        val layerId = "timeline-layer"

        val existingSource = style.getSource(sourceId) as? GeoJsonSource
        if (existingSource != null) {
            existingSource.setGeoJson(featureCollection)
        } else {
            style.addSource(GeoJsonSource(sourceId, featureCollection))
        }

        if (style.getLayer(layerId) == null) {
            style.addLayer(
                LineLayer(layerId, sourceId).apply {
                    setProperties(
                        PropertyFactory.lineColor(mutedLineColor),
                        PropertyFactory.lineWidth(1.8f),
                        PropertyFactory.lineDasharray(arrayOf(2f, 2f))
                    )
                }
            )
        } else {
            style.getLayer(layerId)?.setProperties(PropertyFactory.lineColor(mutedLineColor))
        }

        locationGroups.forEach { group ->
            val locationName = group.location.name?.ifBlank { "Location" } ?: "Location"
            mapboxMap.addMarker(
                MarkerOptions()
                    .position(LatLng(group.location.latitude, group.location.longitude))
                    .icon(getMarkerIcon(locationName))
            )
        }

        if (pathPoints.size == 1) {
            mapboxMap.animateCamera(
                org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(pathPoints.first()).zoom(14.0).build()
                ),
                800
            )
        } else if (pathPoints.size > 1) {
            val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
            pathPoints.forEach { boundsBuilder.include(it) }
            val padding = (64 * context.resources.displayMetrics.density).toInt()
            try {
                mapboxMap.animateCamera(
                    org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), padding),
                    1000
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (validPoints.isEmpty()) {
        EmptyStateMessage("No locations added for this month.")
        return
    }

    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
        if (isInternetAllowed) {
            MapViewLifecycleEffect(mapView)

            LaunchedEffect(styleUrl) {
                if (styleUrl.isBlank()) return@LaunchedEffect
                mapView.getMapAsync { mapboxMap ->
                    mapboxMap.uiSettings.isAttributionEnabled = false
                    mapboxMap.uiSettings.isLogoEnabled = false
                    mapboxMap.uiSettings.isCompassEnabled = false
                    mapboxMap.setStyle(styleUrl) { style ->
                        renderMapContent(mapboxMap, style)
                    }
                }
            }

            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 12.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapAttributions(
                    provider = mapStyleProvider,
                    isDarkMode = isDarkMap
                )

                val currentJournal = sortedPoints.getOrNull(selectedIndex)
                val dayText = remember(currentJournal) { currentJournal?.dateTime?.toDayOfMonth() ?: "" }
                val monthText = remember(currentJournal) { currentJournal?.dateTime?.toShortMonth()?.uppercase() ?: "" }

                MapNavigationPill(
                    totalCount = sortedPoints.size,
                    dayText = dayText,
                    monthText = monthText,
                    onPrevious = {
                        selectedIndex = if (selectedIndex > 0) selectedIndex - 1 else sortedPoints.size - 1
                    },
                    onNext = {
                        selectedIndex = (selectedIndex + 1) % sortedPoints.size
                    }
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 36.dp, end = 16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapControlColumn(
                    isMapExpanded = isMapExpanded,
                    isDarkMode = isDarkMap,
                    onToggleDarkMode = { isDarkMap = !isDarkMap },
                    onToggleFullscreen = { viewModel.setCalendarExpanded(isMapExpanded) }
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                InternetRestrictedIndicator(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = bottomPadding),
                    description = "Maps require internet access to load tiles and display locations."
                )
            }
        }
    }
}

@Composable
fun MapNavigationPill(
    totalCount: Int,
    dayText: String,
    monthText: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(min = 120.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = totalCount > 1
            ) {
                Icon(
                    painter = painterResource(R.drawable.chevron_left_24px),
                    contentDescription = "Previous",
                    tint = if (totalCount > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = dayText.ifBlank { "--" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = monthText.ifBlank { "---" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            IconButton(
                onClick = onNext,
                enabled = totalCount > 1
            ) {
                Icon(
                    painter = painterResource(R.drawable.chevron_right_24px),
                    contentDescription = "Next",
                    tint = if (totalCount > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}