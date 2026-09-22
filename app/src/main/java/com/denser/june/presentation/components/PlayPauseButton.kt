package com.denser.june.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayPauseButton(
    modifier: Modifier = Modifier.size(width = 64.dp, height = 48.dp),
    isPlaying: Boolean,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    val buttonScale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    FilledIconToggleButton(
        checked = isPlaying,
        onCheckedChange = {
            if (!isLoading) {
                coroutineScope.launch {
                    buttonScale.animateTo(1.05f, tween(300))
                    buttonScale.animateTo(
                        1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                }
                onClick()
            }
        },
        enabled = enabled,
        modifier = modifier.scale(buttonScale.value),
        shapes = IconButtonDefaults.toggleableShapes(
            checkedShape = RoundedCornerShape(16.dp)
        ),
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            checkedContainerColor = containerColor,
            checkedContentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularWavyProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = contentColor,
            )
        } else {
            Icon(
                painter = painterResource(
                    if (isPlaying) R.drawable.pause_24px else R.drawable.play_arrow_24px
                ),
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmallPlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    FilledIconToggleButton(
        checked = isPlaying,
        onCheckedChange = { if (!isLoading) onClick() },
        enabled = enabled,
        modifier = modifier.size(width = 52.dp, height = 40.dp),
        shapes = IconButtonDefaults.toggleableShapes(),
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            checkedContainerColor = containerColor,
            checkedContentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularWavyProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = contentColor,
            )
        } else {
            Icon(
                painter = painterResource(
                    if (isPlaying) R.drawable.pause_24px else R.drawable.play_arrow_24px
                ),
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
