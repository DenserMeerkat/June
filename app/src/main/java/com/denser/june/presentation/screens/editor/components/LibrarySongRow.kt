package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.domain.model.SongSourceType
import com.denser.june.presentation.components.RestrictedAsyncImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibrarySongRow(
    song: SongDetails,
    isCurrentlyAttached: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Song?") },
            text = { Text("Are you sure you want to remove \"${song.title}\" from your library?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onRemove()
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(18.dp),
        color = if (isCurrentlyAttached) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp, bottom = 4.dp)
                    .wrapContentSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    RestrictedAsyncImage(
                        imageUrl = song.thumbnailUrl,
                        localPath = song.localThumbnailPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                val isLocal = song.sourceType == SongSourceType.LOCAL_FILE
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 6.dp)
                        .size(22.dp),
                    shape = CircleShape,
                    color = if (isLocal) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    shadowElevation = 2.dp,
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(
                                if (isLocal) R.drawable.folder_open_24px else R.drawable.link_24px
                            ),
                            contentDescription = if (isLocal) "Local file" else "Link",
                            modifier = Modifier.size(13.dp),
                            tint = if (isLocal) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = song.artistName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    val durationMs = remember(song) {
                        val localPath = song.localPreviewPath
                        if (localPath != null && java.io.File(localPath).exists()) {
                            try {
                                val retriever = android.media.MediaMetadataRetriever()
                                retriever.setDataSource(localPath)
                                val dur = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                                retriever.release()
                                dur
                            } catch (_: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }

                    if (durationMs != null && durationMs > 0L) {
                        val totalSeconds = kotlin.math.round(durationMs / 1000.0).toLong()
                        val minutes = totalSeconds / 60
                        val seconds = totalSeconds % 60
                        val formattedDuration = "%d:%02d".format(minutes, seconds)

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )

                        Text(
                            text = formattedDuration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert_24px),
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 200.dp)
                        .padding(horizontal = 8.dp),
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    offset = DpOffset(x = 0.dp, y = 4.dp)
                ) {
                    DropdownMenuItem(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                        text = { Text("Trim & Attach") },
                        onClick = {
                            showMenu = false
                            onClick()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.music_note_24px),
                                contentDescription = null,
                            )
                        }
                    )

                    DropdownMenuItem(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                        text = { Text("Edit Details") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.edit_24px),
                                contentDescription = null
                            )
                        }
                    )

                    DropdownMenuItem(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                        text = {
                            Text(
                                text = "Remove from Library",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            showDeleteConfirm = true
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.delete_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.music_note_24px),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "Your Library is Empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Add songs by pasting a link or choosing an audio file from your device using the buttons above.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
