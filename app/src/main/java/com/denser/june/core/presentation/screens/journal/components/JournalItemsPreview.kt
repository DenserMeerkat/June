package com.denser.june.core.presentation.screens.journal.components

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
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.june.R
import com.denser.june.core.domain.data_classes.JournalLocation
import com.denser.june.core.domain.data_classes.SongDetails
import kotlinx.coroutines.launch

sealed interface JournalPreviewItem {
    data class Images(val paths: List<String>) : JournalPreviewItem
    data class Song(val details: SongDetails) : JournalPreviewItem
    data class Map(val location: JournalLocation) : JournalPreviewItem
}

data class MediaOperations(
    val onItemSheetToggle: (Boolean) -> Unit = {},
    val onRemoveMedia: (String) -> Unit = {},
    val onMoveToFront: (String) -> Unit = {},
    val onMediaClick: ((String) -> Unit)? = {},
    val frontMediaPath: String? = null,
    val onRemoveSong: () -> Unit = {},
    val onSongSheetToggle: (Boolean) -> Unit = {},
    val onRemoveLocation: () -> Unit = {},
    val onLocationDialogToggle: (Boolean) -> Unit = {},
    val isEditMode: Boolean,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JournalItemsPreview(
    mediaPaths: List<String>,
    songDetails: SongDetails?,
    location: JournalLocation?,
    mediaOperations: MediaOperations,
    onShowAllClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    val verticalSlides = remember(mediaPaths, songDetails, location) {
        val list = mutableListOf<JournalPreviewItem>()
        if (mediaPaths.isNotEmpty()) list.add(JournalPreviewItem.Images(mediaPaths))
        if (songDetails != null) list.add(JournalPreviewItem.Song(songDetails))
        if (location != null) list.add(JournalPreviewItem.Map(location))
        list
    }

    val songIndex = verticalSlides.indexOfFirst { it is JournalPreviewItem.Song }
    val imagesIndex = verticalSlides.indexOfFirst { it is JournalPreviewItem.Images }
    val mapIndex = verticalSlides.indexOfFirst { it is JournalPreviewItem.Map }

    val pagerState = rememberPagerState(pageCount = { verticalSlides.size })

    val currentItem =
        if (verticalSlides.isNotEmpty()) verticalSlides.getOrNull(pagerState.currentPage) else null

    val onAddSong = { mediaOperations.onSongSheetToggle(true) }
    val onAddMedia = { mediaOperations.onItemSheetToggle(true) }
    val onAddLocation = { mediaOperations.onLocationDialogToggle(true) }

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
                modifier = Modifier.fillMaxSize()
            ) {
                when (val slide = verticalSlides[pageIndex]) {
                    is JournalPreviewItem.Song -> {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            JournalSongItem(
                                details = slide.details,
                                isFetching = false,
                                onRemove = mediaOperations.onRemoveSong,
                                onEdit = { mediaOperations.onSongSheetToggle(true) },
                                isEditMode = mediaOperations.isEditMode
                            )
                        }
                    }

                    is JournalPreviewItem.Map -> {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            JournalMapItem(
                                location = slide.location,
                                onMapClick = { mediaOperations.onLocationDialogToggle(true) },
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
                                .padding(horizontal = if (chunks.size == 1) 16.dp else 0.dp),
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

        if (mediaPaths.isNotEmpty() || songDetails != null || location != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    val isImagesSelected = currentItem is JournalPreviewItem.Images
                    val imagesExist = imagesIndex != -1
                    ToggleButton(
                        checked = isImagesSelected,
                        enabled = imagesIndex != -1 || mediaOperations.isEditMode,
                        onCheckedChange = {
                            if (imagesExist) {
                                scope.launch { pagerState.animateScrollToPage(imagesIndex) }
                            } else {
                                onAddMedia()
                            }
                        },
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            checkedContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ) {
                        Icon(
                            painter = painterResource(if (isImagesSelected) R.drawable.art_track_24px_fill else R.drawable.art_track_24px),
                            contentDescription = "Images",
                        )
                    }
                    val isSongSelected = currentItem is JournalPreviewItem.Song
                    val songExists = songIndex != -1
                    ToggleButton(
                        checked = isSongSelected,
                        enabled = songExists || mediaOperations.isEditMode,
                        onCheckedChange = {
                            if (songExists) {
                                scope.launch { pagerState.animateScrollToPage(songIndex) }
                            } else {
                                onAddSong()
                            }
                        },
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            checkedContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                    ) {
                        Icon(
                            painter = painterResource(if (isSongSelected) R.drawable.music_video_24px_fill else R.drawable.music_video_24px),
                            contentDescription = "Song",
                        )
                    }
                    val isMapSelected = currentItem is JournalPreviewItem.Map
                    val mapExists = mapIndex != -1
                    ToggleButton(
                        checked = isMapSelected,
                        enabled = mapExists || mediaOperations.isEditMode,
                        onCheckedChange = {
                            if (mapExists) {
                                scope.launch { pagerState.animateScrollToPage(mapIndex) }
                            } else {
                                onAddLocation()
                            }
                        },
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            checkedContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ) {
                        Icon(
                            painter = painterResource(if (isMapSelected) R.drawable.location_chip_24px_fill else R.drawable.location_chip_24px),
                            contentDescription = "Location",
                        )
                    }
                }
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