package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.JournalLocation
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.presentation.components.JuneConnectedButtonGroup
import com.denser.june.presentation.components.journal.JournalMosaicCard
import kotlinx.coroutines.launch

sealed interface JournalPreviewItem {
    data class Images(val paths: List<String>) : JournalPreviewItem
    data class Song(val details: SongDetails) : JournalPreviewItem
    data class Map(val location: JournalLocation) : JournalPreviewItem
}

typealias MediaOperations = com.denser.june.presentation.components.journal.MediaOperations

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JournalItemsPreview(
    mediaPaths: List<String>,
    songDetails: SongDetails?,
    location: JournalLocation?,
    mediaOperations: MediaOperations,
    processingMediaPaths: Set<String> = emptySet(),
    readOnly: Boolean = false,
    selectedSlideIndex: Int? = null,
    onSlideIndexChange: ((Int) -> Unit)? = null,
    onShowAllClick: () -> Unit = {},
    theme: com.denser.june.core.domain.model.JournalTheme? = null
) {
    val scope = rememberCoroutineScope()

    val verticalSlides = remember(mediaPaths, songDetails, location) {
        val list = mutableListOf<JournalPreviewItem>()
        if (mediaPaths.isNotEmpty()) list.add(JournalPreviewItem.Images(mediaPaths))
        if (songDetails != null) list.add(JournalPreviewItem.Song(songDetails))
        if (location != null) list.add(JournalPreviewItem.Map(location))
        list
    }

    var localSlideIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentTargetIndex = (selectedSlideIndex ?: localSlideIndex).coerceIn(0, (verticalSlides.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = currentTargetIndex, pageCount = { verticalSlides.size })

    LaunchedEffect(currentTargetIndex, verticalSlides.size) {
        if (verticalSlides.isNotEmpty() && pagerState.currentPage != currentTargetIndex) {
            pagerState.scrollToPage(currentTargetIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        localSlideIndex = pagerState.currentPage
        onSlideIndexChange?.invoke(pagerState.currentPage)
    }

    val currentSlide = if (verticalSlides.isNotEmpty()) verticalSlides.getOrNull(pagerState.currentPage) else null

    data class ButtonConfig(
        val type: String,
        val iconRes: Int,
        val filledIconRes: Int,
        val isSelected: Boolean,
        val exists: Boolean,
        val onClick: () -> Unit
    )

    val buttons = listOf(
        ButtonConfig(
            type = "Images",
            iconRes = R.drawable.art_track_24px,
            filledIconRes = R.drawable.art_track_24px_fill,
            isSelected = currentSlide is JournalPreviewItem.Images,
            exists = mediaPaths.isNotEmpty(),
            onClick = {
                val idx = verticalSlides.indexOfFirst { it is JournalPreviewItem.Images }
                if (idx != -1) scope.launch { pagerState.animateScrollToPage(idx) }
                else if (!readOnly) mediaOperations.onItemSheetToggle(true)
            }
        ),
        ButtonConfig(
            type = "Song",
            iconRes = R.drawable.music_video_24px,
            filledIconRes = R.drawable.music_video_24px_fill,
            isSelected = currentSlide is JournalPreviewItem.Song,
            exists = songDetails != null,
            onClick = {
                val idx = verticalSlides.indexOfFirst { it is JournalPreviewItem.Song }
                if (idx != -1) scope.launch { pagerState.animateScrollToPage(idx) }
                else if (!readOnly) mediaOperations.onSongSheetToggle(true)
            }
        ),
        ButtonConfig(
            type = "Map",
            iconRes = R.drawable.location_chip_24px,
            filledIconRes = R.drawable.location_chip_24px_fill,
            isSelected = currentSlide is JournalPreviewItem.Map,
            exists = location != null,
            onClick = {
                val idx = verticalSlides.indexOfFirst { it is JournalPreviewItem.Map }
                if (idx != -1) scope.launch { pagerState.animateScrollToPage(idx) }
                else if (!readOnly) mediaOperations.onLocationDialogToggle(true)
            }
        )
    )

    Column {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val pagerHeight = maxWidth / 1.7f
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pagerHeight),
                pageSpacing = 8.dp,
                beyondViewportPageCount = 2,
                key = { pageIndex ->
                    when (val slide = verticalSlides.getOrNull(pageIndex)) {
                        is JournalPreviewItem.Song -> "song_${slide.details.title}_${slide.details.artistName}"
                        is JournalPreviewItem.Map -> "map_${slide.location.latitude}_${slide.location.longitude}"
                        is JournalPreviewItem.Images -> "images_${slide.paths.hashCode()}"
                        null -> pageIndex
                    }
                }
            ) { pageIndex ->
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val slide = verticalSlides.getOrNull(pageIndex) ?: return@Box
                    when (slide) {
                        is JournalPreviewItem.Song -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                JournalSongItem(
                                    details = slide.details,
                                    isFetching = false,
                                    onRemove = if (!readOnly) mediaOperations.onRemoveSong else null,
                                    onEditJournal = if (!readOnly) mediaOperations.onEditJournalSong else null,
                                    onOpenLibrary = if (!readOnly) { { mediaOperations.onSongSheetToggle(true) } } else null,
                                    onTrim = if (!readOnly) mediaOperations.onTrimSong else null,
                                    theme = theme,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        is JournalPreviewItem.Map -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                JournalMapItem(
                                    location = slide.location,
                                    onMapClick = if (!readOnly) { { mediaOperations.onLocationDialogToggle(true) } } else null,
                                    onRemove = if (!readOnly) mediaOperations.onRemoveLocation else null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        is JournalPreviewItem.Images -> {
                            val chunks = remember(slide.paths) {
                                slide.paths.reversed().chunked(3)
                            }
                            val widthFraction = if (chunks.size > 1) 0.95f else 1f
                            val effectiveOperations = remember(mediaOperations, readOnly) {
                                if (readOnly) {
                                    mediaOperations.copy(
                                        onMediaClick = null,
                                        onItemSheetToggle = {},
                                        onRemoveMedia = {},
                                        onMoveToFront = {}
                                    )
                                } else {
                                    mediaOperations
                                }
                            }

                            LazyRow(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = if (chunks.size == 1) 16.dp else 0.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (chunks.size > 1) {
                                    item { Spacer(modifier = Modifier.width(0.dp)) }
                                }
                                items(
                                    items = chunks,
                                    key = { chunk -> chunk.joinToString(",") }
                                ) { chunk ->
                                    Box(
                                        modifier = Modifier
                                            .fillParentMaxWidth(widthFraction)
                                            .fillMaxHeight()
                                    ) {
                                        JournalMosaicCard(
                                            modifier = Modifier.fillMaxSize(),
                                            mediaList = chunk,
                                            operations = effectiveOperations,
                                            processingMediaPaths = processingMediaPaths,
                                            enableContextMenu = !readOnly,
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
        }

        if (mediaPaths.isNotEmpty() || songDetails != null || location != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                JuneConnectedButtonGroup(
                    options = buttons,
                    selected = { it.isSelected },
                    onSelect = { it.onClick() },
                    itemEnabled = { it.exists || !readOnly },
                    buttonModifier = Modifier
                        .height(32.dp)
                        .width(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                ) { config, isSelected ->
                    Icon(
                        painter = painterResource(if (isSelected) config.filledIconRes else config.iconRes),
                        contentDescription = config.type,
                        modifier = Modifier.size(20.dp)
                    )
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