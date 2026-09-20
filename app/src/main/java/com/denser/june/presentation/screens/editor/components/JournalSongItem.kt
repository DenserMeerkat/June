package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.presentation.components.JuneSongPlayerCard
import com.denser.june.presentation.utils.rememberSongPlayerState
import com.denser.june.core.R

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalSongItem(
    details: SongDetails?,
    isFetching: Boolean,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    onTrim: (() -> Unit)? = null,
    onAddToLibrary: (() -> Unit)? = null,
    isInLibrary: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(1.7f)
) {
    val playerState = rememberSongPlayerState(
        previewUrl = details?.previewUrl,
        localPreviewPath = details?.localPreviewPath,
        clipStartMs = details?.clipStartMs,
        clipEndMs = details?.clipEndMs
    )

    var showMenu by remember { mutableStateOf(false) }

    when {
        isFetching -> {
            SongCardPlaceholder(isLoading = true, modifier = modifier)
        }

        details != null -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(32.dp))
                    .combinedClickable(
                        onClick = { onEdit() },
                        onLongClick = { showMenu = true }
                    )
            ) {
                JuneSongPlayerCard(
                    details = details,
                    isPlaying = playerState.isPlaying,
                    isLoading = playerState.isLoading,
                    sliderValue = playerState.sliderValue,
                    isRepeatEnabled = playerState.isRepeatEnabled,
                    onPlayPause = playerState.onPlayPause,
                    onSeek = playerState.onSeek,
                    onSeekFinished = playerState.onSeekFinished,
                    onToggleRepeat = playerState.onToggleRepeat,
                    modifier = Modifier.fillMaxSize()
                )

                if (showMenu) {
                    DropdownMenu(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 200.dp)
                            .padding(horizontal = 8.dp),
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 3.dp,
                    ) {
                        DropdownMenuItem(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                            text = { Text(stringResource(R.string.edit_song)) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(painterResource(R.drawable.edit_24px), null)
                            }
                        )
                        if (onTrim != null && details.localPreviewPath?.let { java.io.File(it).exists() } == true) {
                            DropdownMenuItem(
                                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                                text = { Text("Trim Clip") },
                                onClick = {
                                    showMenu = false
                                    onTrim()
                                },
                                leadingIcon = {
                                    Icon(painterResource(R.drawable.more_time_24px), null)
                                }
                            )
                        }
                        if (onAddToLibrary != null && !isInLibrary) {
                            DropdownMenuItem(
                                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                                text = { Text("Add to Library") },
                                onClick = {
                                    showMenu = false
                                    onAddToLibrary()
                                },
                                leadingIcon = {
                                    Icon(painterResource(R.drawable.music_note_add_24px), null)
                                }
                            )
                        }
                        DropdownMenuItem(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                            text = { Text(stringResource(R.string.remove)) },
                            onClick = {
                                showMenu = false
                                onRemove()
                            },
                            leadingIcon = {
                                Icon(painterResource(R.drawable.delete_24px), null)
                            }
                        )
                    }
                }
            }
        }

        else -> {
            SongCardPlaceholder(isLoading = false, modifier = modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongCardPlaceholder(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(32.dp),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                ContainedLoadingIndicator(
                    modifier = Modifier.size(64.dp),
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.music_note_2_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No song attached",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}