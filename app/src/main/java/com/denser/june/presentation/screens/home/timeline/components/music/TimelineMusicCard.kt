package com.denser.june.presentation.screens.home.timeline.components.music

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.media.storage.SongMediaStorage
import com.denser.june.presentation.components.RestrictedAsyncImage
import com.denser.june.presentation.components.journal.rememberJournalGroupShape
import com.denser.june.presentation.screens.home.timeline.components.common.TimelineDateColumn
import com.denser.june.presentation.theme.LocalInternetAllowed

@Composable
fun TimelineMusicDayGroup(
    modifier: Modifier = Modifier,
    journals: List<Journal>,
    activeSong: SongDetails?,
    onSongClick: (SongDetails, String) -> Unit,
    onSongLongClick: ((SongDetails, String) -> Unit)? = null
) {
    val groupShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(groupShape),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        journals.forEachIndexed { index, journal ->
            val song = journal.songDetails ?: return@forEachIndexed
            val shape = rememberJournalGroupShape(index = index, totalCount = journals.size)
            val isSongActive = (activeSong?.localPreviewPath != null && activeSong.localPreviewPath == song.localPreviewPath) ||
                    (activeSong?.previewUrl != null && activeSong.previewUrl == song.previewUrl)

            TimelineMusicCard(
                journal = journal,
                song = song,
                shape = shape,
                isActive = isSongActive,
                onClick = { onSongClick(song, journal.id) },
                onLongClick = onSongLongClick?.let { { it(song, journal.id) } }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineMusicCard(
    journal: Journal,
    song: SongDetails,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    isActive: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isInternetAllowed = LocalInternetAllowed.current
    val hasLocalAudio = remember(song.localPreviewPath) {
        SongMediaStorage.resolveSongMedia(context, song.localPreviewPath, "library") != null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                enabled = isInternetAllowed || hasLocalAudio,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimelineDateColumn(dateTime = journal.dateTime)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                RestrictedAsyncImage(
                    imageUrl = song.thumbnailUrl,
                    localPath = song.localThumbnailPath,
                    iconSize = 20.dp,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
