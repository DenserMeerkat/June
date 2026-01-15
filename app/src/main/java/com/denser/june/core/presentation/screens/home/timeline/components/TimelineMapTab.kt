package com.denser.june.core.presentation.screens.home.timeline.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.LocalAppTheme
import com.denser.june.R
import com.denser.june.core.domain.data_classes.Journal
import com.denser.june.core.domain.enums.AppTheme
import com.denser.june.core.presentation.components.MapControlColumn
import com.denser.june.core.presentation.components.MapAttributions
import com.denser.june.core.presentation.utils.MapTilerUtils
import com.denser.june.viewmodels.TimelineVM
import org.koin.compose.viewmodel.koinViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

@Composable
fun TimelineMapTab(
    journals: List<Journal>,
    bottomPadding: Dp,
    viewModel: TimelineVM = koinViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isCalendarExpanded by viewModel.isCalendarExpanded.collectAsStateWithLifecycle()
    val isMapExpanded = !isCalendarExpanded
    remember { MapLibre.getInstance(context) }

    val validPoints = remember(journals) {
        journals.filter {
            it.location != null && it.location.latitude != 0.0 && it.location.longitude != 0.0
        }
    }
    var selectedIndex by remember { mutableIntStateOf(0) }

    val currentTheme = LocalAppTheme.current.appTheme
    val systemDark = isSystemInDarkTheme()
    val initialMapTheme = remember(currentTheme, systemDark) {
        when (currentTheme) {
            AppTheme.SYSTEM -> systemDark
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
        }
    }
    var isDarkMap by remember { mutableStateOf(initialMapTheme) }
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    fun getMarkerIcon(resId: Int, color: Int): org.maplibre.android.annotations.Icon {
        val drawable = context.getDrawable(resId)?.mutate()
        drawable?.setTint(color)
        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    if (validPoints.isEmpty()) {
        EmptyStateMessage("No locations added for this month.")
    } else {
        val mapView = remember {
            MapView(context).apply {
                isClickable = true
                isFocusable = true
            }
        }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { map ->
                    val styleUrl =
                        if (isDarkMap) MapTilerUtils.STYLE_DARK else MapTilerUtils.STYLE_LIGHT
                    map.getMapAsync { mapboxMap ->
                        mapboxMap.uiSettings.isAttributionEnabled = false
                        mapboxMap.uiSettings.isLogoEnabled = false
                        mapboxMap.uiSettings.isCompassEnabled = false
                        mapboxMap.setStyle(styleUrl) { style ->
                            mapboxMap.clear()

                            validPoints.forEachIndexed { index, journal ->
                                val loc = journal.location!!
                                val markerOptions = MarkerOptions()
                                    .position(LatLng(loc.latitude, loc.longitude))
                                    .title(loc.name ?: "Location")
                                    .icon(
                                        getMarkerIcon(
                                            R.drawable.location_on_24px_fill,
                                            primaryColor
                                        )
                                    )
                                mapboxMap.addMarker(markerOptions)
                            }
                        }
                    }
                }
            )
            LaunchedEffect(selectedIndex) {
                if (validPoints.isNotEmpty()) {
                    val target = validPoints[selectedIndex].location!!
                    mapView.getMapAsync { map ->
                        map.animateCamera(
                            org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(LatLng(target.latitude, target.longitude))
                                    .zoom(16.0)
                                    .build()
                            ),
                            800
                        )
                    }
                }
            }
            MapControlColumn(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 16.dp),
                isDarkMode = isDarkMap,
                onToggleDarkMode = { isDarkMap = !isDarkMap },
                isMapExpanded = isMapExpanded,
                onToggleFullscreen = { viewModel.setCalendarExpanded(isMapExpanded) },
                isFetchingLocation = false,
                onZoomIn = {
                    mapView.getMapAsync { it.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.zoomIn()) }
                },
                onZoomOut = {
                    mapView.getMapAsync { it.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.zoomOut()) }
                }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 12.dp)
            ) {
                MapAttributions(isDarkMode = isDarkMap)
            }
            MapNavigationPill(
                currentIndex = selectedIndex,
                totalCount = validPoints.size,
                currentLocationName = validPoints[selectedIndex].location?.name ?: "Unknown",
                onPrevious = { if (selectedIndex > 0) selectedIndex-- },
                onNext = { if (selectedIndex < validPoints.size - 1) selectedIndex++ },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding + 12.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}

@Composable
fun MapNavigationPill(
    currentIndex: Int,
    totalCount: Int,
    currentLocationName: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .widthIn(max = 320.dp)
            .fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = currentIndex > 0
            ) {
                Icon(
                    painter = painterResource(R.drawable.chevron_left_24px),
                    contentDescription = "Previous",
                    tint = if (currentIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.38f
                    )
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentLocationName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${currentIndex + 1} of $totalCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            IconButton(
                onClick = onNext,
                enabled = currentIndex < totalCount - 1
            ) {
                Icon(
                    painter = painterResource(R.drawable.chevron_right_24px),
                    contentDescription = "Next",
                    tint = if (currentIndex < totalCount - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.38f
                    )
                )
            }
        }
    }
}