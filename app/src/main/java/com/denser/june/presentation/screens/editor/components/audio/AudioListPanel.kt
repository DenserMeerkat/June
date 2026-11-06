package com.denser.june.presentation.screens.editor.components.audio

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.utils.toAudioTimestamp
import com.denser.june.presentation.components.JuneConnectedRowItem
import com.denser.june.presentation.components.JuneDropdownMenu
import com.denser.june.presentation.components.JuneDropdownMenuItem

@Composable
fun AudioListPanel(
    modifier: Modifier = Modifier,
    audios: List<JournalAudio>,
    activeAudioId: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectAudio: (JournalAudio) -> Unit,
    onReorderAudios: (List<JournalAudio>) -> Unit,
    onDeleteAudio: (String) -> Unit,
    onAudioInfo: (JournalAudio) -> Unit,
    processingAudioIds: Set<String> = emptySet(),
    maxListHeight: Dp = Dp.Unspecified
) {
    Column(
        modifier = modifier
    ) {
        val rotation by animateFloatAsState(
            targetValue = if (isExpanded) 0f else 180f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "chevronRotation"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onToggleExpand() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.playlist_play_24px),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${stringResource(R.string.audios)} (${audios.size})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                painter = painterResource(R.drawable.keyboard_arrow_down_24px),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotation },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (maxListHeight != Dp.Unspecified) {
                                Modifier.heightIn(max = maxListHeight)
                            } else {
                                Modifier.wrapContentHeight()
                            }
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(audios, key = { _, audio -> audio.id }) { index, audio ->
                        val isSelected = audio.id == activeAudioId
                        var showMenu by remember { mutableStateOf(false) }

                        val isProcessing = processingAudioIds.contains(audio.id)
                        val durationStr = audio.durationMs.toAudioTimestamp()
                        val displayTitle = if (isProcessing) {
                            stringResource(R.string.importing_audio)
                        } else {
                            audio.title.orEmpty().ifBlank { stringResource(R.string.untitled) }
                        }

                        JuneConnectedRowItem(
                            index = index,
                            count = audios.size,
                            selected = isSelected,
                            leadingSpacing = 2.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            leadingContent = {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(if (isSelected) R.drawable.mic_24px_fill else R.drawable.mic_24px),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            },
                            trailingContent = {
                                Box {
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert_24px),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }

                                    JuneDropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        if (index > 0) {
                                            JuneDropdownMenuItem(
                                                text = { Text(stringResource(R.string.move_up)) },
                                                onClick = {
                                                    showMenu = false
                                                    val updated = audios.toMutableList()
                                                    val item = updated.removeAt(index)
                                                    updated.add(index - 1, item)
                                                    onReorderAudios(updated)
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.arrow_upward_24px),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                             )
                                        }

                                        if (index < audios.size - 1) {
                                            JuneDropdownMenuItem(
                                                text = { Text(stringResource(R.string.move_down)) },
                                                onClick = {
                                                    showMenu = false
                                                    val updated = audios.toMutableList()
                                                    val item = updated.removeAt(index)
                                                    updated.add(index + 1, item)
                                                    onReorderAudios(updated)
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.arrow_downward_24px),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            )
                                        }

                                        JuneDropdownMenuItem(
                                            text = { Text("Audio Info") },
                                            onClick = {
                                                showMenu = false
                                                onAudioInfo(audio)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    painter = painterResource(R.drawable.info_24px),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        )

                                        JuneDropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_delete)) },
                                            onClick = {
                                                showMenu = false
                                                onDeleteAudio(audio.id)
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
                            },
                            onClick = {
                                onSelectAudio(audio)
                            }
                        ) {
                            if (!isProcessing) {
                                Text(
                                    text = durationStr,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1
                                )
                                Text(
                                    text = " • ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    }
                                )
                            }
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (isProcessing) FontWeight.Medium else FontWeight.Normal
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }
            }
        }
    }
}
