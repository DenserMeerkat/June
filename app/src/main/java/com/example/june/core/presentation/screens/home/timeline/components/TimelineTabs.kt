package com.example.june.core.presentation.screens.home.timeline.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.june.core.domain.data_classes.Journal
import com.example.june.core.presentation.screens.home.journals.components.JournalCard
import com.example.june.core.presentation.screens.journal.components.JournalMosaicCard
import com.example.june.core.presentation.screens.journal.components.MediaOperations
import com.example.june.viewmodels.TimelineTab

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineTabs(
    selectedTab: TimelineTab,
    journals: List<Journal>,
    onTabSelected: (TimelineTab) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    CompositionLocalProvider(
        LocalOverscrollFactory provides null
    ) {
        Column(modifier = modifier.fillMaxSize()) {
            TimelineTabSelector(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    TimelineTab.Journals -> JournalListSection(
                        journals = journals,
                        bottomPadding = bottomPadding
                    )

                    TimelineTab.Media -> MonthMediaGrid(
                        journals = journals,
                        bottomPadding = bottomPadding
                    )

                    TimelineTab.Map -> TimelineMapTab(
                        journals = journals,
                        bottomPadding = bottomPadding
                    )

                    TimelineTab.Music -> TimelineMusicTab(
                        journals = journals,
                        bottomPadding = bottomPadding
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineTabSelector(
    selectedTab: TimelineTab,
    onTabSelected: (TimelineTab) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
        }
    ) {
        TimelineTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun JournalListSection(journals: List<Journal>, bottomPadding: Dp) {
    if (journals.isEmpty()) {
        EmptyStateMessage("No journals this month.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(journals, key = { it.id }) { journal ->
                JournalCard(
                    journal = journal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun MonthMediaGrid(journals: List<Journal>, bottomPadding: Dp) {
    val allImages = remember(journals) { journals.flatMap { it.images } }

    if (allImages.isEmpty()) {
        EmptyStateMessage("No media this month.")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(
                bottom = bottomPadding + 16.dp,
                top = 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(allImages) { media ->
                JournalMosaicCard(
                    mediaList = listOf(media),
                    enablePlayback = false,
                    operations = MediaOperations(isEditMode = false),
                    roundedCornerShape = RoundedCornerShape(8.dp),
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }
    }
}

@Composable
fun MonthMusicList(journals: List<Journal>) {
    EmptyStateMessage("Music tracking coming soon.")
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}