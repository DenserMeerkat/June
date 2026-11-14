package com.denser.june.presentation.screens.editor.components.audio

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.utils.AudioPlayerState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AudioControlBar(
    playerState: AudioPlayerState,
    hasMultipleAudios: Boolean = false,
    hasPreviousAudio: Boolean = false,
    hasNextAudio: Boolean = false,
    onPreviousAudio: (() -> Unit)? = null,
    onNextAudio: (() -> Unit)? = null,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val count = if (hasMultipleAudios) 5 else 3
    val interactionSources = remember(hasMultipleAudios) { List(count) { MutableInteractionSource() } }

    ButtonGroup(
        overflowIndicator = {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        if (hasMultipleAudios) {
            customItem(
                buttonGroupContent = {
                    FilledTonalButton(
                        onClick = { onPreviousAudio?.invoke() },
                        enabled = !isProcessing && hasPreviousAudio,
                        interactionSource = interactionSources[0],
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .animateWidth(interactionSources[0])
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chevron_left_24px),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                menuContent = {}
            )
        }

        val rewindIdx = if (hasMultipleAudios) 1 else 0
        customItem(
            buttonGroupContent = {
                FilledTonalButton(
                    onClick = { if (!isProcessing) playerState.onRewind5s() },
                    enabled = !isProcessing,
                    interactionSource = interactionSources[rewindIdx],
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .animateWidth(interactionSources[rewindIdx])
                ) {
                    Icon(
                        painter = painterResource(R.drawable.replay_5_24px),
                        contentDescription = "Rewind 5s",
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            menuContent = {}
        )

        val playIdx = if (hasMultipleAudios) 2 else 1
        customItem(
            buttonGroupContent = {
                Button(
                    onClick = { if (!isProcessing) playerState.onPlayPause() },
                    enabled = !isProcessing,
                    interactionSource = interactionSources[playIdx],
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(if (hasMultipleAudios) 1.6f else 1.8f)
                        .fillMaxHeight()
                        .animateWidth(interactionSources[playIdx])
                ) {
                    Icon(
                        painter = painterResource(
                            if (playerState.isPlaying) R.drawable.pause_24px else R.drawable.play_arrow_24px
                        ),
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            menuContent = {}
        )

        val fwdIdx = if (hasMultipleAudios) 3 else 2
        customItem(
            buttonGroupContent = {
                FilledTonalButton(
                    onClick = { if (!isProcessing) playerState.onForward10s() },
                    enabled = !isProcessing,
                    interactionSource = interactionSources[fwdIdx],
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .animateWidth(interactionSources[fwdIdx])
                ) {
                    Icon(
                        painter = painterResource(R.drawable.forward_10_24px),
                        contentDescription = "Forward 10s",
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            menuContent = {}
        )

        if (hasMultipleAudios) {
            customItem(
                buttonGroupContent = {
                    FilledTonalButton(
                        onClick = { onNextAudio?.invoke() },
                        enabled = !isProcessing && hasNextAudio,
                        interactionSource = interactionSources[4],
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .animateWidth(interactionSources[4])
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chevron_right_24px),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                menuContent = {}
            )
        }
    }
}
