package com.example.june.core.presentation.screens.home.timeline.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.june.R
import com.example.june.core.domain.data_classes.Journal
import com.example.june.core.domain.utils.toFullDate
import com.example.june.core.presentation.utils.rememberManagedOsmMapView
import com.example.june.viewmodels.TimelineVM
import org.koin.compose.viewmodel.koinViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

@Composable
fun TimelineMapTab(
    journals: List<Journal>,
    bottomPadding: Dp,
    viewModel: TimelineVM = koinViewModel()
) {
    val context = LocalContext.current
    val isCalendarExpanded by viewModel.isCalendarExpanded.collectAsStateWithLifecycle()
    val isMapExpanded = !isCalendarExpanded

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f).toArgb()

    val validPoints = remember(journals) {
        journals.filter {
            it.location != null && it.location.latitude != 0.0 && it.location.longitude != 0.0
        }
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    var zoomLevel by remember { mutableDoubleStateOf(18.0) }
    var isDarkMap by remember { mutableStateOf(false) }

    fun resizeDrawable(resId: Int, sizeDp: Int): Drawable? {
        val drawable = context.getDrawable(resId) ?: return null
        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return BitmapDrawable(context.resources, bitmap)
    }

    if (validPoints.isEmpty()) {
        EmptyStateMessage("No locations added for this month.")
    } else {
        val mapView = rememberManagedOsmMapView(
            context = context,
            debounceTime = 500L
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView },
                update = { map ->
                    if (isDarkMap) {
                        map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
                    } else {
                        map.overlayManager.tilesOverlay.setColorFilter(null)
                    }

                    map.overlays.clear()
                    validPoints.forEachIndexed { index, journal ->
                        val point =
                            GeoPoint(journal.location!!.latitude, journal.location.longitude)
                        val isSelected = index == selectedIndex

                        val marker = Marker(map).apply {
                            position = point
                            title = journal.location.name ?: journal.location.address
                            snippet = journal.dateTime.toFullDate()

                            val size = if (isSelected) 40 else 36
                            val resizedDrawable = resizeDrawable(R.drawable.location_on_24px_fill, size)?.mutate()

                            resizedDrawable?.setTint(if (isSelected) primaryColor else secondaryColor)
                            icon = resizedDrawable

                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                            setOnMarkerClickListener { m, _ ->
                                selectedIndex = index
                                m.showInfoWindow()
                                true
                            }
                        }
                        map.overlays.add(marker)
                    }
                    map.invalidate()

                    if (validPoints.isNotEmpty()) {
                        val targetJournal = validPoints[selectedIndex]
                        val targetPoint = GeoPoint(
                            targetJournal.location!!.latitude,
                            targetJournal.location.longitude
                        )
                        map.controller.setZoom(zoomLevel)
                        map.controller.animateTo(targetPoint)
                    }
                }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SmallFloatingActionButton(
                    onClick = { viewModel.setCalendarExpanded(isMapExpanded) },
                    containerColor = if (isMapExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (isMapExpanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(
                        painter = painterResource(
                            if (isMapExpanded) R.drawable.fullscreen_exit_24px else R.drawable.fullscreen_24px
                        ),
                        contentDescription = if (isMapExpanded) "Collapse Map" else "Expand Map"
                    )
                }
                SmallFloatingActionButton(
                    onClick = { isDarkMap = !isDarkMap },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(
                        painter = painterResource(if (isDarkMap) R.drawable.light_mode_24px else R.drawable.dark_mode_24px),
                        contentDescription = "Toggle Theme"
                    )
                }
                Column {
                    SmallFloatingActionButton(
                        onClick = { zoomLevel = (zoomLevel + 1).coerceAtMost(21.0) },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add_24px),
                            contentDescription = "Zoom In"
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = { zoomLevel = (zoomLevel - 1).coerceAtLeast(3.0) },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.remove_24px),
                            contentDescription = "Zoom Out"
                        )
                    }
                }
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
            .width(300.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 4.dp)
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