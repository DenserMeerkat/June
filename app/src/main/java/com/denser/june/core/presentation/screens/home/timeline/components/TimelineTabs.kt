package com.denser.june.core.presentation.screens.home.timeline.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.data_classes.Journal
import com.denser.june.viewmodels.TimelineTab

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
                    TimelineTab.Journals -> TimelineJournalTab(
                        journals = journals,
                        bottomPadding = bottomPadding
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
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}