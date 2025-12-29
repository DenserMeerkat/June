package com.example.june.core.presentation.screens.journal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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

sealed interface JournalPreviewItem {
    data class Images(val paths: List<String>) : JournalPreviewItem
    data class Song(val details: SongDetails) : JournalPreviewItem
}

data class MediaOperations(
    val onRemove: (String) -> Unit,
    val onMoveToFront: (String) -> Unit,
    val onMediaClick: ((String) -> Unit)? = null,
    val frontMediaPath: String?,
    val onRemoveSong: () -> Unit,
    val onEditSong: () -> Unit,
    val isEditMode: Boolean,
)

@Composable
fun JournalItemsPreview(
    mediaPaths: List<String>,
    songDetails: SongDetails?,
    mediaOperations: MediaOperations,
    onShowAllClick: () -> Unit = {}
) {
    val carouselItems = remember(mediaPaths, songDetails) {
        val items = mutableListOf<JournalPreviewItem>()
        if (songDetails != null) {
            items.add(JournalPreviewItem.Song(songDetails))
        }
        if (mediaPaths.isNotEmpty()) {
            items.add(JournalPreviewItem.Images(mediaPaths))
        }
        items
    }
    val isMultipleItems = carouselItems.size > 1
    val widthFraction = if (isMultipleItems) 0.9f else 1f
    Column(
        modifier = if (!isMultipleItems) Modifier.padding(horizontal = 16.dp) else Modifier
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isMultipleItems) {
                item { Spacer(Modifier.width(8.dp)) }
            }
            items(carouselItems) { item ->
                Box(modifier = Modifier.fillParentMaxWidth(widthFraction)) {
                    when (item) {
                        is JournalPreviewItem.Song -> {
                            JournalSongItem(
                                details = item.details,
                                isFetching = false,
                                onRemove = mediaOperations.onRemoveSong,
                                onEdit = mediaOperations.onEditSong,
                                isEditMode = mediaOperations.isEditMode
                            )
                        }

                        is JournalPreviewItem.Images -> {
                            val chunks = remember(item.paths) {
                                item.paths.reversed().chunked(3)
                            }
                            val heightFraction = if (chunks.size > 1) 0.9f else 1f
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(chunks) { chunk ->
                                    Box(modifier = Modifier.fillParentMaxHeight(heightFraction)) {
                                        JournalMosaicCard(
                                            modifier = Modifier.fillMaxSize(),
                                            mediaList = chunk,
                                            operations = mediaOperations,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (isMultipleItems) {
                item { Spacer(Modifier.width(8.dp)) }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            if (mediaPaths.isNotEmpty()) {
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
}