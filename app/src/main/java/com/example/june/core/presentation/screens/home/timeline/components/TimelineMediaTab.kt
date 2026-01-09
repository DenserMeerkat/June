package com.example.june.core.presentation.screens.home.timeline.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.june.core.domain.data_classes.Journal
import com.example.june.core.domain.utils.toFullDate
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.Route
import com.example.june.core.presentation.screens.journal.components.JournalMediaItem
import com.example.june.core.presentation.screens.journal.components.MediaOperations
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineMediaTab(
    journals: List<Journal>,
    bottomPadding: Dp
) {
    val navigator = koinInject<AppNavigator>()
    val allMedia = remember(journals) { journals.flatMap { it.images } }
    val groupedMedia = remember(journals) {
        journals.filter { it.images.isNotEmpty() }
            .groupBy { it.dateTime.toFullDate() }
    }

    if (allMedia.isEmpty()) {
        EmptyStateMessage("No media this month.")
    } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val minSize = 100.dp
            val spacing = 4.dp

            val density = LocalDensity.current
            val columns = remember(screenWidth) {
                val availableWidth = screenWidth - 32.dp
                val minSizePx = with(density) { minSize.toPx() }
                val spacingPx = with(density) { spacing.toPx() }
                val availableWidthPx = with(density) { availableWidth.toPx() }

                maxOf(1, (availableWidthPx / (minSizePx + spacingPx)).toInt())
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
            ) {
                groupedMedia.forEach { (date, journalsInDate) ->
                    val dayImages = journalsInDate.flatMap { it.images }

                    stickyHeader {
                        MediaDateHeader(dateString = date)
                    }
                    val rows = dayImages.chunked(columns)
                    items(rows) { rowImages ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowImages.forEach { mediaPath ->
                                Box(modifier = Modifier.weight(1f)) {
                                    JournalMediaItem(
                                        path = mediaPath,
                                        modifier = Modifier
                                            .aspectRatio(1f),
                                        enablePlayback = false,
                                        isLargeItem = true,
                                        operations = MediaOperations(
                                            onMediaClick = {
                                                val globalIndex = allMedia.indexOf(mediaPath)
                                                navigator.navigateTo(
                                                    Route.MediaViewerRoute(
                                                        allMedia,
                                                        globalIndex.coerceAtLeast(0)
                                                    ),
                                                    isSingleTop = true,
                                                )
                                            },
                                            isEditMode = false
                                        ),
                                    )
                                }
                            }
                            val emptySlots = columns - rowImages.size
                            repeat(emptySlots) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
fun MediaDateHeader(dateString: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = dateString,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}