package com.example.june.core.presentation.screens.journal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.june.core.domain.data_classes.SongDetails
import com.example.june.core.presentation.components.JuneSongPlayerCard
import com.example.june.core.presentation.utils.rememberSongPlayerState

sealed interface JournalPreviewItem {
    data class ImageChunk(val paths: List<String>) : JournalPreviewItem
    data class Song(val details: SongDetails) : JournalPreviewItem
}

data class MediaOperations(
    val onRemove: (String) -> Unit,
    val onMoveToFront: (String) -> Unit,
    val onMediaClick: ((String) -> Unit)? = null,
    val isEditMode: Boolean,
    val frontMediaPath: String?
)

@Composable
fun JournalItemsPreview(
    modifier: Modifier = Modifier,
    mediaPaths: List<String>,
    songDetails: SongDetails?,
    mediaOperations: MediaOperations,
    onShowAllClick: () -> Unit = {}
) {
    val chunks = remember(mediaPaths) {
        mediaPaths.reversed().chunked(3)
    }

    val carouselItems = remember(mediaPaths, songDetails) {
        val items = mutableListOf<JournalPreviewItem>()
        if (songDetails != null) {
            items.add(JournalPreviewItem.Song(songDetails))
        }
        if (mediaPaths.isNotEmpty()) {
            val chunks = mediaPaths.reversed().chunked(3)
            chunks.mapTo(items) { JournalPreviewItem.ImageChunk(it) }
        }
        items
    }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(carouselItems) { item ->
                val widthFraction = if (carouselItems.size == 1) 1f else 0.9f

                when (item) {
                    is JournalPreviewItem.ImageChunk -> {
                        JournalMosaicCard(
                            modifier = Modifier.fillParentMaxWidth(widthFraction),
                            mediaList = item.paths,
                            operations = mediaOperations,
                        )
                    }
                    is JournalPreviewItem.Song -> {
                        Box(modifier = Modifier.fillParentMaxWidth(widthFraction)) {
                            SongPlayerCardWrapper(item.details)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onShowAllClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            ) {
                Text(
                    text = "Show all",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun SongPlayerCardWrapper(
    songDetails: SongDetails,
) {
    val playerState = rememberSongPlayerState(previewUrl = songDetails.previewUrl)

    JuneSongPlayerCard(
        details = songDetails,
        isPlaying = playerState.isPlaying,
        sliderValue = playerState.sliderValue,
        onPlayPause = playerState.onPlayPause,
        onSeek = playerState.onSeek,
        onSeekFinished = playerState.onSeekFinished,
    )
}