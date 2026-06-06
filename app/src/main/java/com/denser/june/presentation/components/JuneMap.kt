package com.denser.june.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.MapStyleProvider
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.core.domain.model.enums.MapTheme
import com.denser.june.presentation.theme.LocalAppTheme
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView


@Composable
fun MapControlColumn(
    isFetchingLocation: Boolean = false,
    onMyLocationClick: (() -> Unit)? = null,
    isMapExpanded: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    onToggleDarkMode: (() -> Unit)? = null,
    onZoomIn: (() -> Unit)? = null,
    onZoomOut: (() -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (onToggleFullscreen != null) {
            FilledIconButton(
                onClick = onToggleFullscreen,
                shape = RoundedCornerShape(18.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(if (isMapExpanded) R.drawable.fullscreen_exit_24px else R.drawable.fullscreen_24px),
                    contentDescription = if (isMapExpanded) "Collapse Map" else "Expand Map",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (onToggleDarkMode != null) {
            FilledIconButton(
                onClick = onToggleDarkMode,
                shape = RoundedCornerShape(18.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(if (isDarkMode) R.drawable.light_mode_24px else R.drawable.dark_mode_24px),
                    contentDescription = "Toggle Dark Mode",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (onZoomIn != null) {
            FilledIconButton(
                onClick = onZoomIn,
                shape = RoundedCornerShape(18.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_24px),
                    contentDescription = "Zoom In",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (onZoomOut != null) {
            FilledIconButton(
                onClick = onZoomOut,
                shape = RoundedCornerShape(18.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.remove_24px),
                    contentDescription = "Zoom Out",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (onMyLocationClick != null) {
            FilledIconButton(
                onClick = onMyLocationClick,
                enabled = !isFetchingLocation,
                shape = RoundedCornerShape(20.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.size(56.dp)
            ) {
                if (isFetchingLocation) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.my_location_24px_fill),
                        contentDescription = "Current Location",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MapLocationPin(
    modifier: Modifier = Modifier,
    isMoving: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PinAnimation")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isMoving) -10f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BounceOffset"
    )

    val shadowAlpha = if (isMoving) 0.1f else 0.3f
    val shadowScale = if (isMoving) 0.6f else 1.0f

    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(width = 16.dp, height = 8.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-20).dp)
                .scale(shadowScale)
        ) {
            drawOval(color = Color.Black.copy(alpha = shadowAlpha))
        }
        Icon(
            painter = painterResource(R.drawable.location_on_24px_fill),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                .offset(y = (-20).dp + bounce.dp)
        )
    }
}


@Composable
fun MapAttributions(
    provider: MapStyleProvider,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isDarkMode)
        Color.White.copy(alpha = 0.7f)
    else
        Color.Black.copy(alpha = 0.6f)

    Row(
        modifier = modifier.wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (provider) {
            MapStyleProvider.MAPTILER -> {
                Text(
                    text = "© MapTiler © OpenStreetMap",
                    color = contentColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            MapStyleProvider.STADIA -> {
                Text(
                    text = "© Stadia Maps © OpenStreetMap",
                    color = contentColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            MapStyleProvider.CARTO -> {
                Text(
                    text = "© CARTO © OpenStreetMap",
                    color = contentColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            MapStyleProvider.MAPBOX -> {
                Text(
                    text = "© Mapbox © OpenStreetMap",
                    color = contentColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun MapLibreInitializer(isInternetAllowed: Boolean) {
    val context = LocalContext.current
    remember(isInternetAllowed) {
        if (isInternetAllowed) MapLibre.getInstance(context) else null
    }
}

@Composable
fun rememberMapDarkMode(savedMapTheme: MapTheme): MutableState<Boolean> {
    val currentTheme = LocalAppTheme.current.themeMode
    val systemDark = isSystemInDarkTheme()
    val resolvedDarkMode = remember(savedMapTheme, currentTheme, systemDark) {
        when (savedMapTheme) {
            MapTheme.APP -> when (currentTheme) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            MapTheme.DARK -> true
            MapTheme.LIGHT -> false
        }
    }
    val state = remember { mutableStateOf(resolvedDarkMode) }
    LaunchedEffect(resolvedDarkMode) {
        state.value = resolvedDarkMode
    }
    return state
}

@Composable
fun MapViewLifecycleEffect(mapView: MapView) {
    val lifecycleOwner = LocalLifecycleOwner.current
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
}