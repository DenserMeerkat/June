package com.denser.june.presentation.components.sheets

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.domain.model.SongSourceType
import com.denser.june.core.media.exif.AudioMetadataParser
import com.denser.june.core.media.storage.SongMediaStorage
import com.denser.june.core.utils.formatBytes
import com.denser.june.core.utils.toAudioTimestamp
import com.denser.june.core.utils.toExifDateTimeDisplay
import com.denser.june.core.utils.toSongDurationString
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneConnectedList
import com.denser.june.presentation.components.JuneConnectedRowItem
import com.denser.june.presentation.components.RestrictedAsyncImage
import com.denser.june.presentation.components.getPlatformIcon
import com.denser.june.presentation.components.toAvailableLinks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoSheet(
    song: SongDetails,
    journal: Journal,
    onDismiss: () -> Unit,
    onOpenJournal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    val dateDisplay = remember(journal) {
        journal.dateTime.takeIf { it > 946684800000L }?.toExifDateTimeDisplay()
            ?: System.currentTimeMillis().toExifDateTimeDisplay()
    }

    val availableLinks = remember(song.links) {
        song.links.toAvailableLinks()
    }

    val artFile = remember(song.localThumbnailPath) {
        SongMediaStorage.resolveSongMedia(context, song.localThumbnailPath, "art")
    }
    val artFileSize = remember(artFile) {
        artFile?.length()?.takeIf { it > 0L }?.formatBytes()
    }

    val audioFile = remember(song.localPreviewPath) {
        SongMediaStorage.resolveSongMedia(context, song.localPreviewPath, "library")
    }
    val audioFileSize = remember(audioFile) {
        audioFile?.length()?.takeIf { it > 0L }?.formatBytes()
    }
    val audioDurationMs = remember(audioFile) {
        audioFile?.let {
            try {
                AudioMetadataParser.parseAudioMetadata(context, it).durationMs
            } catch (_: Exception) {
                0L
            }
        } ?: 0L
    }

    val isLocallyCached = audioFile != null && audioFile.exists()

    ItemInfoSheet(
        onDismissRequest = onDismiss,
        dateDisplay = dateDisplay,
        parentJournal = journal,
        onOpenJournal = {
            onDismiss()
            onOpenJournal()
        },
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Details",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            JuneConnectedList(modifier = Modifier.fillMaxWidth()) {
                JuneConnectedItem(
                    title = if (isLocallyCached) "Backed up" else "Not backed up",
                    subtitle = if (isLocallyCached) {
                        audioFileSize?.let { "Stored locally ($it)" } ?: "Stored locally"
                    } else {
                        song.previewUrlProvider?.let { "Online stream ($it)" } ?: "Streaming track"
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(
                                if (isLocallyCached) R.drawable.cloud_done_24px
                                else R.drawable.cloud_off_24px
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )

                SongDetailItem(
                    text = song.title,
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.music_note_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )

                SongDetailItem(
                    text = song.artistName,
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.artist_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )

                val album = song.albumName
                if (!album.isNullOrBlank()) {
                    SongDetailItem(
                        text = album,
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.album_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    )
                }

                val genre = song.genre
                if (!genre.isNullOrBlank()) {
                    SongDetailItem(
                        text = genre,
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.category_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    )
                }

                SongDetailItem(
                    text = artFileSize ?: if (song.thumbnailUrl != null) "Remote Artwork" else "No Artwork",
                    leadingContent = {
                        RestrictedAsyncImage(
                            imageUrl = song.thumbnailUrl,
                            localPath = song.localThumbnailPath,
                            iconSize = 18.dp,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                )

                val clipEnd = song.clipEndMs
                val hasClip = clipEnd != null && clipEnd > 0L
                if (hasClip) {
                    val clipDurationMs = (clipEnd - song.clipStartMs).coerceAtLeast(0L)
                    val specs = buildList {
                        add(song.clipStartMs.toAudioTimestamp())
                        add(clipEnd.toAudioTimestamp())
                        add(clipDurationMs.toSongDurationString())
                        if (audioDurationMs > 0L) {
                            add(audioDurationMs.toSongDurationString())
                        }
                    }
                    ItemSpecsRow(specs = specs)
                } else if (audioDurationMs > 0L) {
                    SongDetailItem(
                        text = audioDurationMs.toSongDurationString(),
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.schedule_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    )
                }

                SongDetailItem(
                    text = if (song.sourceType == SongSourceType.LOCAL_FILE) "Local File" else (song.previewUrlProvider ?: "Streaming Link"),
                    leadingContent = {
                        Icon(
                            painter = painterResource(if (song.sourceType == SongSourceType.LOCAL_FILE) R.drawable.folder_open_24px else R.drawable.link_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )
            }
        }

        if (availableLinks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Listen",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            JuneConnectedList(
                items = availableLinks,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) { index, count, (platform, url) ->
                val openAction = {
                    try {
                        uriHandler.openUri(url)
                    } catch (_: Exception) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }

                val copyAction = {
                    clipboardManager.setText(AnnotatedString(url))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(context.applicationContext, "Link copied!", Toast.LENGTH_SHORT).show()
                }

                JuneConnectedItem(
                    title = platform,
                    index = index,
                    count = count,
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 10.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    onClick = openAction,
                    leadingContent = {
                        Icon(
                            painter = painterResource(getPlatformIcon(platform)),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = copyAction,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.content_copy_24px),
                                    contentDescription = "Copy Link",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = openAction,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.open_in_new_24px),
                                    contentDescription = "Open Link",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SongDetailItem(
    text: String,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null
) {
    JuneConnectedRowItem(
        modifier = modifier,
        leadingContent = leadingContent,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
