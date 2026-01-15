package com.denser.june.core.presentation.components

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
import com.denser.june.R


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MapControlColumn(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    isFetchingLocation: Boolean,
    onMyLocationClick: (() -> Unit)?,
    isTerrainMode: Boolean,
    onToggleTerrain: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledIconButton(
            onClick = onToggleDarkMode,
            shape = RoundedCornerShape(16.dp),
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

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledIconButton(
                onClick = onZoomIn,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(painter = painterResource(R.drawable.add_24px), contentDescription = "Zoom In", modifier = Modifier.size(24.dp))
            }

            FilledIconButton(
                onClick = onZoomOut,
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(painter = painterResource(R.drawable.remove_24px), contentDescription = "Zoom Out", modifier = Modifier.size(24.dp))
            }
        }

        if (onMyLocationClick != null) {
            FilledIconButton(
                onClick = onMyLocationClick,
                enabled = !isFetchingLocation,
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.size(56.dp)
            ) {
                if (isFetchingLocation) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(painter = painterResource(R.drawable.my_location_24px_fill), contentDescription = "Current Location", modifier = Modifier.size(24.dp))
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
fun MapTilerAttribution(isDarkMode: Boolean, modifier: Modifier = Modifier) {
    val contentColor = if (isDarkMode)
        Color.White.copy(alpha = 0.7f)
    else
        Color.Black.copy(alpha = 0.6f)

    Row(
        modifier = modifier.wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.maptiler_logo),
            contentDescription = "MapTiler Logo",
            tint = contentColor,
            modifier = Modifier.height(20.dp)
        )
    }
}