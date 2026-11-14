package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.domain.model.JournalTheme
import com.denser.june.core.media.storage.AudioStorageManager
import com.denser.june.presentation.components.JuneDropdownMenu
import com.denser.june.presentation.components.JuneDropdownMenuItem
import com.denser.june.presentation.screens.editor.components.audio.AudioControlBar
import com.denser.june.presentation.screens.editor.components.audio.AudioSpeedTimeRow
import com.denser.june.presentation.screens.editor.components.audio.AudioWaveformScrubber
import com.denser.june.presentation.utils.rememberAudioPlayerState

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JournalAudioItem(
    audios: List<JournalAudio>,
    onOpenStudio: ((String) -> Unit)? = null,
    onRemove: ((String) -> Unit)? = null,
    theme: JournalTheme? = null,
    processingAudioIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(1.7f)
) {
    if (audios.isEmpty()) return

    val context = LocalContext.current
    var currentIndex by remember(audios) { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }

    val safeIndex = currentIndex.coerceIn(0, audios.lastIndex)
    val audio = audios[safeIndex]
    val isProcessing = processingAudioIds.contains(audio.id)

    val audioFile = remember(audio.path) {
        AudioStorageManager.resolveAudioFile(context, audio.path)
    }

    val playerState = rememberAudioPlayerState(
        file = audioFile,
        initialDurationMs = audio.durationMs
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp),
        onClick = { onOpenStudio?.invoke(audio.id) },
        enabled = onOpenStudio != null,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isProcessing) {
                            stringResource(R.string.importing_audio)
                        } else {
                            audio.title.orEmpty().ifBlank { stringResource(R.string.untitled) }
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    Box {
                        FilledIconButton(
                            onClick = { showMenu = true },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert_24px),
                                contentDescription = stringResource(R.string.cd_more_options),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        JuneDropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (onOpenStudio != null) {
                                JuneDropdownMenuItem(
                                    text = { Text(stringResource(R.string.audio_notes)) },
                                    onClick = {
                                        showMenu = false
                                        onOpenStudio(audio.id)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.playlist_play_24px),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }

                            if (onRemove != null) {
                                JuneDropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_delete)) },
                                    onClick = {
                                        showMenu = false
                                        if (currentIndex >= audios.lastIndex && currentIndex > 0) {
                                            currentIndex--
                                        }
                                        onRemove(audio.id)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.delete_24px),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                AudioWaveformScrubber(
                    waveform = audio.waveform,
                    sliderValue = playerState.sliderValue,
                    onSeek = playerState.onSeek,
                    onSeekFinished = playerState.onSeekFinished,
                    barWidth = 3.dp,
                    barSpacing = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )

                AudioSpeedTimeRow(
                    playerState = playerState,
                    isProcessing = isProcessing,
                    modifier = Modifier.fillMaxWidth()
                )

                AudioControlBar(
                    playerState = playerState,
                    hasMultipleAudios = audios.size > 1,
                    hasPreviousAudio = safeIndex > 0,
                    hasNextAudio = safeIndex < audios.size - 1,
                    onPreviousAudio = if (safeIndex > 0) {
                        { currentIndex = safeIndex - 1 }
                    } else null,
                    onNextAudio = if (safeIndex < audios.size - 1) {
                        { currentIndex = safeIndex + 1 }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator(
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
    }
}
