package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.utils.toSongDurationString
import com.denser.june.core.utils.toSongTimestamp
import com.denser.june.presentation.components.RestrictedAsyncImage
import com.denser.june.presentation.components.SmallPlayPauseButton
import com.denser.june.presentation.utils.rememberSongPlayerState

private data class ActiveBarAction(
    val iconRes: Int,
    val contentDescription: String,
    val onClick: () -> Unit,
    val isActive: Boolean = false,
    val isDestructive: Boolean = false
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActiveJournalSongBar(
    song: SongDetails,
    onEdit: () -> Unit,
    onTrim: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRemoveConfirm by remember { mutableStateOf(false) }
    val playerState = rememberSongPlayerState(
        previewUrl = song.previewUrl,
        localPreviewPath = song.localPreviewPath,
        clipStartMs = song.clipStartMs,
        clipEndMs = song.clipEndMs
    )

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove from journal?") },
            text = { Text("This will detach the song from this journal entry. The library copy stays unchanged.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveConfirm = false
                        onRemove()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Remove") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveConfirm = false }) { Text("Cancel") }
            }
        )
    }

    val totalDuration = playerState.exoPlayer?.duration?.takeIf { it > 0L }
    val effectiveEnd = song.clipEndMs ?: totalDuration
    val startStr = song.clipStartMs.toSongTimestamp()
    val endStr = effectiveEnd?.toSongTimestamp() ?: "Full"
    val durationMs = effectiveEnd?.let { (it - song.clipStartMs).coerceAtLeast(0L) }
    val durationStr = durationMs?.toSongDurationString()

    val actions = remember(onEdit, onTrim) {
        listOf(
            ActiveBarAction(
                iconRes = R.drawable.edit_24px,
                contentDescription = "Edit Details",
                onClick = onEdit
            ),
            ActiveBarAction(
                iconRes = R.drawable.more_time_24px,
                contentDescription = "Trim Clip",
                onClick = onTrim
            ),
            ActiveBarAction(
                iconRes = R.drawable.delete_24px,
                contentDescription = "Remove from journal",
                onClick = { showRemoveConfirm = true },
                isDestructive = true
            )
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            LinearProgressIndicator(
                progress = { playerState.sliderValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    RestrictedAsyncImage(
                        imageUrl = song.thumbnailUrl,
                        localPath = song.localThumbnailPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        iconSize = 20.dp,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artistName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                SmallPlayPauseButton(
                    isPlaying = playerState.isPlaying,
                    isLoading = playerState.isLoading,
                    enabled = true,
                    onClick = playerState.onPlayPause,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                ) {
                    Text(
                        text = startStr,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = endStr,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (durationStr != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = durationStr,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions.forEachIndexed { index, action ->
                        val shape = when {
                            actions.size == 1 -> ToggleButtonDefaults.shapes()
                            index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            index == actions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                        val contentColor = when {
                            action.isDestructive -> MaterialTheme.colorScheme.error
                            action.isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val buttonModifier = when {
                            actions.size == 1 -> Modifier
                                .height(34.dp)
                                .width(44.dp)

                            index == 0 || index == actions.lastIndex -> Modifier
                                .height(34.dp)
                                .width(40.dp)

                            else -> Modifier
                                .height(34.dp)
                                .width(36.dp)
                        }

                        ToggleButton(
                            checked = action.isActive,
                            onCheckedChange = { action.onClick() },
                            shapes = shape,
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = contentColor,
                                checkedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                checkedContentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                            modifier = buttonModifier
                        ) {
                            Icon(
                                painter = painterResource(action.iconRes),
                                contentDescription = action.contentDescription,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
