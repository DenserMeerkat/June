package com.denser.june.presentation.screens.editor.components.audio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.utils.toAudioTimestamp
import com.denser.june.presentation.utils.AudioPlayerState
import com.denser.june.presentation.utils.formatRemainingTimestamp

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AudioSpeedTimeRow(
    playerState: AudioPlayerState,
    modifier: Modifier = Modifier,
    isProcessing: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isProcessing) "--:--" else playerState.currentPositionMs.toAudioTimestamp(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        val speedText = when (playerState.playbackSpeed) {
            1.5f -> "1.5x"
            2.0f -> "2.0x"
            else -> "1.0x"
        }

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            ToggleButton(
                checked = playerState.playbackSpeed > 1.0f,
                onCheckedChange = { if (!isProcessing) playerState.onToggleSpeed() },
                enabled = !isProcessing,
                shapes = ToggleButtonDefaults.shapes(
                    shape = RoundedCornerShape(10.dp),
                    pressedShape = RoundedCornerShape(18.dp),
                    checkedShape = RoundedCornerShape(10.dp)
                ),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .height(30.dp)
                    .width(58.dp)
            ) {
                Text(
                    text = speedText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (playerState.playbackSpeed > 1.0f) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Text(
            text = if (isProcessing) "--:--" else formatRemainingTimestamp(playerState.remainingMs),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
