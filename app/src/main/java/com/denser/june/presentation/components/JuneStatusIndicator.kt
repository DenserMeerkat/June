package com.denser.june.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JuneStatusIndicator(
    iconRes: Int,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
    isRunning: Boolean = false,
    progress: Float = 0f,
    size: Dp = 24.dp,
    progressTrackColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    progressColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    val containerSize = size + 12.dp

    val animatedIconSize by animateDpAsState(
        targetValue = if (isRunning) size * 0.75f else size,
        animationSpec = tween(durationMillis = 300),
        label = "status_indicator_icon_size"
    )

    Box(
        modifier = modifier
            .size(containerSize)
            .let { m ->
                if (onClick != null) {
                    m.clip(CircleShape)
                        .clickable(onClick = onClick)
                } else m
            },
        contentAlignment = Alignment.Center
    ) {
        if (isRunning) {
            if (progress > 0f) {
                CircularWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = progressColor,
                    trackColor = progressTrackColor
                )
            } else {
                CircularWavyProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = progressColor,
                    trackColor = progressTrackColor
                )
            }
        }

        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(animatedIconSize),
            tint = iconTint
        )
    }
}
