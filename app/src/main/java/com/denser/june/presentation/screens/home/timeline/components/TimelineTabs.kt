package com.denser.june.presentation.screens.home.timeline.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.presentation.screens.home.timeline.TimelineTab
import com.denser.june.presentation.components.JunePlaceholderPage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineTabs(
    selectedTab: TimelineTab,
    journals: List<Journal>,
    onTabSelected: (TimelineTab) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    onLongClickJournal: ((Journal) -> Unit)? = null
) {
    CompositionLocalProvider(
        LocalOverscrollFactory provides null
    ) {
        Column(modifier = modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                TimelineTab.entries.forEach { tab ->
                    Tab(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    TimelineTab.Journals -> TimelineJournalTab(
                        journals = journals,
                        bottomPadding = bottomPadding,
                        onLongClick = onLongClickJournal
                    )

                    TimelineTab.Media -> TimelineMediaTab(
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


@Composable
fun EmptyStateMessage(message: String) {
    JunePlaceholderPage(
        modifier = Modifier.padding(bottom = 100.dp),
        subtitle = message
    )
}