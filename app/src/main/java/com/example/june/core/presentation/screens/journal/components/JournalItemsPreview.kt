package com.example.june.core.presentation.screens.journal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.june.core.domain.data_classes.JournalLocation
import com.example.june.core.domain.data_classes.SongDetails

sealed interface JournalPreviewItem {
    data class Images(val paths: List<String>) : JournalPreviewItem
    data class Song(val details: SongDetails) : JournalPreviewItem
    data class Map(val location: JournalLocation) : JournalPreviewItem
}

data class MediaOperations(
    val onRemoveMedia: (String) -> Unit,
    val onMoveToFront: (String) -> Unit,
    val onMediaClick: ((String) -> Unit)? = null,
    val frontMediaPath: String?,
    val onRemoveSong: () -> Unit,
    val onEditSong: () -> Unit,
    val onRemoveLocation: () -> Unit,
    val onLocationClick: () -> Unit,
    val isEditMode: Boolean,
)

@Composable
fun JournalItemsPreview(
    mediaPaths: List<String>,
    songDetails: SongDetails?,
    location: JournalLocation?,
    mediaOperations: MediaOperations,
    onShowAllClick: () -> Unit = {}
) {
    val verticalSlides = remember(mediaPaths, songDetails, location) {
        val list = mutableListOf<JournalPreviewItem>()
        if (songDetails != null) list.add(JournalPreviewItem.Song(songDetails))
        if (mediaPaths.isNotEmpty()) list.add(JournalPreviewItem.Images(mediaPaths))
        if (location != null) list.add(JournalPreviewItem.Map(location))
        list
    }

    if (verticalSlides.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { verticalSlides.size })

    Column {
        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            pageSpacing = 8.dp,
            beyondViewportPageCount = 2
        ) { pageIndex ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                when (val slide = verticalSlides[pageIndex]) {
                    is JournalPreviewItem.Song -> {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            JournalSongItem(
                                details = slide.details,
                                isFetching = false,
                                onRemove = mediaOperations.onRemoveSong,
                                onEdit = mediaOperations.onEditSong,
                                isEditMode = mediaOperations.isEditMode
                            )
                        }
                    }

                    is JournalPreviewItem.Map -> {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            JournalMapItem(
                                location = slide.location,
                                onMapClick = mediaOperations.onLocationClick,
                                onRemove = mediaOperations.onRemoveLocation,
                                isEditMode = mediaOperations.isEditMode
                            )
                        }
                    }

                    is JournalPreviewItem.Images -> {
                        val chunks = remember(slide.paths) {
                            slide.paths.reversed().chunked(3)
                        }
                        val widthFraction = if (chunks.size > 1) 0.95f else 1f

                        LazyRow(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (chunks.size == 1) 16.dp else 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (chunks.size > 1) {
                                item { Spacer(modifier = Modifier.width(0.dp)) }
                            }
                            items(chunks) { chunk ->
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxWidth(widthFraction)
                                        .fillMaxHeight()
                                ) {
                                    JournalMosaicCard(
                                        modifier = Modifier.fillMaxSize(),
                                        mediaList = chunk,
                                        operations = mediaOperations,
                                    )
                                }
                            }
                            if (chunks.size > 1) {
                                item { Spacer(modifier = Modifier.width(0.dp)) }
                            }
                        }
                    }
                }
            }
        }
        if (mediaPaths.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onShowAllClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                ) {
                    Text(text = "Show all", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}