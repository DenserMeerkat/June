package com.example.june.core.presentation.screens.journal.components

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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.june.R
import com.example.june.core.domain.data_classes.SongDetails
import com.example.june.core.presentation.components.JuneSongPlayerCard
import com.example.june.core.presentation.utils.rememberSongPlayerState
@Composable
fun JournalSongItem(
    details: SongDetails?,
    isFetching: Boolean,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    isEditMode: Boolean
) {
    val playerState = rememberSongPlayerState(previewUrl = details?.previewUrl)

    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }

    when {
        isFetching -> {
            SongCardPlaceholder(isLoading = true)
        }

        details != null -> {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .indication(interactionSource, LocalIndication.current)
                        .then(
                            if (isEditMode) {
                                Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { onEdit() },
                                        onLongPress = { offset ->
                                            showMenu = true
                                            pressOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                                        },
                                        onPress = { offset ->
                                            val press = PressInteraction.Press(offset)
                                            interactionSource.emit(press)
                                            tryAwaitRelease()
                                            interactionSource.emit(PressInteraction.Release(press))
                                        },
                                    )
                                }
                            } else Modifier
                        )
                ) {
                    JuneSongPlayerCard(
                        details = details,
                        isPlaying = playerState.isPlaying,
                        sliderValue = playerState.sliderValue,
                        onPlayPause = playerState.onPlayPause,
                        onSeek = playerState.onSeek,
                        onSeekFinished = playerState.onSeekFinished,
                    )

                    if (isEditMode && showMenu) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize() 
                                .wrapContentSize(align = Alignment.TopStart) 
                                .offset(x = pressOffset.x, y = pressOffset.y) 
                                .size(1.dp) 
                        ) {
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
                                    text = { Text("Edit Song") },
                                    onClick = {
                                        showMenu = false
                                        onEdit()
                                    },
                                    leadingIcon = {
                                        Icon(painterResource(R.drawable.edit_24px), null)
                                    }
                                )
                                DropdownMenuItem(
                                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                                    text = { Text("Remove") },
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
            }
        }

        else -> {
            SongCardPlaceholder(isLoading = false)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongCardPlaceholder(
    isLoading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
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