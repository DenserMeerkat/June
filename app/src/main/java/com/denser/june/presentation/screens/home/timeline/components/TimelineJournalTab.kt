package com.denser.june.presentation.screens.home.timeline.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

import com.denser.june.core.R
import com.denser.june.core.utils.toLocalDate
import com.denser.june.presentation.components.DayJournalGroupData
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineJournalTab(
    journals: List<Journal>,
    bottomPadding: Dp,
    is24Hour: Boolean = false,
    targetScrollDate: LocalDate? = null,
    onScrollConsumed: (() -> Unit)? = null,
    onToggleBookmark: ((String) -> Unit)? = null,
    onLongClick: ((Journal) -> Unit)? = null
) {
    val navigator = koinInject<AppNavigator>()
    val listState = rememberLazyListState()
    var highlightedDate by remember { mutableStateOf<LocalDate?>(null) }

    val dayGroups = remember(journals) {
        journals
            .groupBy { it.dateTime.toLocalDate() }
            .map { (date, journalsOnDay) -> DayJournalGroupData(date, journalsOnDay) }
    }

    LaunchedEffect(targetScrollDate) {
        if (targetScrollDate != null) {
            val index = dayGroups.indexOfFirst { it.date == targetScrollDate }
            if (index != -1) {
                highlightedDate = targetScrollDate
                listState.animateScrollToItem(index)
                delay(1500.milliseconds)
                highlightedDate = null
            }
            onScrollConsumed?.invoke()
        }
    }

    if (journals.isEmpty()) {
        EmptyStateMessage(stringResource(R.string.no_journals_this_month))
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dayGroups.forEach { dayGroup ->
                if (dayGroup.journals.size > 1) {
                    item(key = "day_group_${dayGroup.date}") {
                        TimelineDayGroup(
                            date = dayGroup.date,
                            journals = dayGroup.journals,
                            is24Hour = is24Hour,
                            isHighlighted = dayGroup.date == highlightedDate,
                            onToggleBookmark = { id -> onToggleBookmark?.invoke(id) },
                            onJournalClick = { journal ->
                                navigator.navigateTo(Route.Editor(journal.id), isSingleTop = true)
                            },
                            onLongClick = { journal -> onLongClick?.invoke(journal) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem()
                        )
                    }
                } else {
                    val singleJournal = dayGroup.journals.first()
                    item(key = "single_journal_${singleJournal.id}") {
                        TimelineJournalCard(
                            journal = singleJournal,
                            is24Hour = is24Hour,
                            isHighlighted = dayGroup.date == highlightedDate,
                            onToggleBookmark = { onToggleBookmark?.invoke(singleJournal.id) },
                            onJournalClick = {
                                navigator.navigateTo(Route.Editor(singleJournal.id), isSingleTop = true)
                            },
                            onLongClick = { onLongClick?.invoke(singleJournal) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem()
                        )
                    }
                }
            }
        }
    }
}
