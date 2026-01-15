package com.denser.june.core.presentation.screens.journal.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.denser.june.R
import com.denser.june.core.domain.data_classes.JournalLocation
import com.denser.june.core.presentation.components.MapLocationPin
import com.denser.june.core.presentation.components.MapTilerAttribution
import com.denser.june.core.presentation.utils.MapTilerUtils
import org.maplibre.android.MapLibre
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

@Composable
fun JournalMapItem(
    location: JournalLocation,
    onMapClick: () -> Unit,
    onRemove: () -> Unit,
    isEditMode: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }
    val context = LocalContext.current

    remember { MapLibre.getInstance(context) }

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(location.longitude, location.latitude),
            zoom = 15.0
        )
    )

    val isDarkMode = isSystemInDarkTheme()
    val mapStyle = remember(isDarkMode) {
        if (isDarkMode) MapTilerUtils.STYLE_DARK else MapTilerUtils.STYLE_LIGHT
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                baseStyle = BaseStyle.Uri(mapStyle),
                cameraState = cameraState,
                options = MapOptions(
                    gestureOptions = GestureOptions(
                        isTiltEnabled = false,
                        isZoomEnabled = false,
                        isRotateEnabled = false,
                        isScrollEnabled = false,
                    ),
                    ornamentOptions = OrnamentOptions(
                        isLogoEnabled = false,
                        isAttributionEnabled = false,
                        isCompassEnabled = false,
                        isScaleBarEnabled = false
                    )
                )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
            ) {
                MapTilerAttribution(isDarkMode = isDarkMode)
            }
            Surface(
                onClick = onMapClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.location_on_24px),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = location.name ?: "Pinned Location",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp))
                    .indication(interactionSource, LocalIndication.current)
                    .pointerInput(isEditMode) {
                        detectTapGestures(
                            onTap = { onMapClick() },
                            onLongPress = { offset ->
                                if (isEditMode) {
                                    showMenu = true
                                    pressOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                                }
                            },
                            onPress = { offset ->
                                val press = PressInteraction.Press(offset)
                                interactionSource.emit(press)
                                tryAwaitRelease()
                                interactionSource.emit(PressInteraction.Release(press))
                            }
                        )
                    }
            )
            Box(modifier = Modifier.align(Alignment.Center)) {
                MapLocationPin()
            }
            if (isEditMode && showMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(align = Alignment.TopStart)
                        .offset(x = pressOffset.x, y = pressOffset.y)
                        .size(1.dp)
                ) {
                    DropdownMenu(
                        modifier = Modifier.defaultMinSize(minWidth = 200.dp).padding(horizontal = 8.dp),
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        DropdownMenuItem(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                            text = { Text("Change Location") },
                            onClick = {
                                showMenu = false
                                onMapClick()
                            },
                            leadingIcon = { Icon(painterResource(R.drawable.edit_24px), null) }
                        )
                        DropdownMenuItem(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                            text = { Text("Remove") },
                            onClick = {
                                showMenu = false
                                onRemove()
                            },
                            leadingIcon = { Icon(painterResource(R.drawable.delete_24px), null) }
                        )
                    }
                }
            }
        }
    }
}