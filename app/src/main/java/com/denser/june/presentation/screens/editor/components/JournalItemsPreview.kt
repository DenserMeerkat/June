package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.JournalLocation
import com.denser.june.core.domain.model.SongDetails
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

    val pagerState = rememberPagerState(pageCount = { verticalSlides.size })
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
                else mediaOperations.onItemSheetToggle(true)
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
                else mediaOperations.onSongSheetToggle(true)
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
                else mediaOperations.onLocationDialogToggle(true)
            }
        )
    )

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
                            )
                        }
                    }
                    is JournalPreviewItem.Map -> {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            JournalMapItem(
                                location = slide.location,
                                onMapClick = { mediaOperations.onLocationDialogToggle(true) },
                                onRemove = mediaOperations.onRemoveLocation,
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
                    .padding(horizontal = 16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    buttons.forEachIndexed { index, config ->
                        val shape = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            buttons.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }

                        ToggleButton(
                            checked = config.isSelected,
                            onCheckedChange = { config.onClick() },
                            enabled = config.exists,
                            shapes = shape,
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkedContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.tertiaryContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            contentPadding = PaddingValues(6.dp),
                            modifier = Modifier.height(36.dp).width(48 .dp)
                        ) {
                            Icon(
                                painter = painterResource(if (config.isSelected) config.filledIconRes else config.iconRes),
                                contentDescription = config.type,
                                modifier = Modifier.size(28.dp)
                            )
                        }
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