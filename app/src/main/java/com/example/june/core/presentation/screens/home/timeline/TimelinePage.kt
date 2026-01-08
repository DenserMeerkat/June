package com.example.june.core.presentation.screens.home.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.june.core.domain.data_classes.Journal
import com.example.june.core.presentation.screens.home.journals.components.JournalCard
import com.example.june.core.presentation.screens.home.timeline.components.TimelineCalendarPage
import com.example.june.core.presentation.screens.home.timeline.components.TimelineMonthStrip
import com.example.june.viewmodels.TimelineVM
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate

@Composable
fun TimelinePage(
    viewModel: TimelineVM = koinViewModel()
) {
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val journalsInMonth by viewModel.journalsInMonth.collectAsStateWithLifecycle()
    val sortedJournals = remember(journalsInMonth) {
        journalsInMonth.sortedByDescending { it.dateTime }
    }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val pagerState = rememberPagerState(
        initialPage = viewModel.initialPage,
        pageCount = { Int.MAX_VALUE }
    )

    LaunchedEffect(pagerState.currentPage) {
        val newMonth = viewModel.getMonthForPage(pagerState.currentPage)
        if (newMonth != currentMonth) viewModel.onMonthChange(newMonth)
    }

    LaunchedEffect(currentMonth) {
        val targetPage = viewModel.getPageForMonth(currentMonth)
        if (targetPage != pagerState.currentPage) pagerState.animateScrollToPage(targetPage)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TimelineMonthStrip(
            currentMonth = currentMonth,
            onMonthSelect = { month ->
                viewModel.onMonthChange(month)
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) { page ->
                val monthForPage = viewModel.getMonthForPage(page)
                if (monthForPage == currentMonth || kotlin.math.abs(pagerState.currentPage - page) <= 1) {
                    val pageJournals =
                        if (monthForPage == currentMonth) journalsInMonth else emptyList()

                    TimelineCalendarPage(
                        yearMonth = monthForPage,
                        selectedDate = selectedDate,
                        journals = pageJournals,
                        onDateSelected = { selectedDate = it }
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 2.dp,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        )
        JournalListSection(
            journals = sortedJournals,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun JournalListSection(journals: List<Journal>, modifier: Modifier = Modifier) {
    if (journals.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No journals this month.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp),
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