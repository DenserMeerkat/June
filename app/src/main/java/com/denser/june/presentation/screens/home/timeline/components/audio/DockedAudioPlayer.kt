package com.denser.june.presentation.screens.home.timeline.components.audio

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.utils.toAudioTimestamp
import com.denser.june.presentation.utils.formatRemainingTimestamp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DockedAudioPlayer(
    audio: JournalAudio,
    journalTitle: String?,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onRewind5s: () -> Unit,
    onForward10s: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSources = remember { List(3) { MutableInteractionSource() } }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = audio.title?.ifBlank { stringResource(R.string.audio_notes) } ?: stringResource(R.string.audio_notes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentSeconds = ((progress * audio.durationMs) / 1000L).toLong() * 1000L
                val remainingSeconds = (audio.durationMs - currentSeconds).coerceAtLeast(0L)

                Text(
                    text = currentSeconds.toAudioTimestamp(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )

                ButtonGroup(
                    overflowIndicator = {},
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .width(176.dp)
                        .height(44.dp)
                ) {
                    customItem(
                        buttonGroupContent = {
                            FilledTonalButton(
                                onClick = onRewind5s,
                                interactionSource = interactionSources[0],
                                shape = CircleShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .animateWidth(interactionSources[0])
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.replay_5_24px),
                                    contentDescription = "Rewind 5s",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        menuContent = {}
                    )

                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = onPlayPause,
                                interactionSource = interactionSources[1],
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .fillMaxHeight()
                                    .animateWidth(interactionSources[1])
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isPlaying) R.drawable.pause_24px else R.drawable.play_arrow_24px
                                    ),
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        menuContent = {}
                    )

                    customItem(
                        buttonGroupContent = {
                            FilledTonalButton(
                                onClick = onForward10s,
                                interactionSource = interactionSources[2],
                                shape = CircleShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .animateWidth(interactionSources[2])
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.forward_10_24px),
                                    contentDescription = "Forward 10s",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        menuContent = {}
                    )
                }

                Text(
                    text = formatRemainingTimestamp(remainingSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }
    }
}

