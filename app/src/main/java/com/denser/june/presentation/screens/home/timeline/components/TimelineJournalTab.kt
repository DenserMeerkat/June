package com.denser.june.presentation.screens.home.timeline.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import org.koin.compose.koinInject

import com.denser.june.core.R
import com.denser.june.core.utils.toLocalDate
import com.denser.june.presentation.components.DayJournalGroupData

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineJournalTab(
    journals: List<Journal>,
    bottomPadding: Dp,
    is24Hour: Boolean = false,
    onToggleBookmark: ((String) -> Unit)? = null,
    onLongClick: ((Journal) -> Unit)? = null
) {
    val navigator = koinInject<AppNavigator>()

    val dayGroups = remember(journals) {
        journals
            .groupBy { it.dateTime.toLocalDate() }
            .map { (date, journalsOnDay) -> DayJournalGroupData(date, journalsOnDay) }
    }

    if (journals.isEmpty()) {
        EmptyStateMessage(androidx.compose.ui.res.stringResource(R.string.no_journals_this_month))
    } else {
        LazyColumn(
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