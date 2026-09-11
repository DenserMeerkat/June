package com.denser.june.presentation.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import java.time.LocalDate

@Composable
fun DayJournalGroup(
    modifier: Modifier = Modifier,
    date: LocalDate,
    journals: List<Journal>,
    is24Hour: Boolean,
    recentJournalId: String? = null,
    onToggleBookmark: (String) -> Unit,
    onJournalClick: ((Journal) -> Unit)? = null,
    onLongClick: (Journal) -> Unit,
) {

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        journals.forEachIndexed { index, journal ->
            val shape = when (index) {
                0 -> RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                )
                journals.lastIndex -> RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp,
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp
                )
                else -> RoundedCornerShape(8.dp)
            }

            if (journal.id == recentJournalId) {
                RecentJournalCard(
                    journal = journal,
                    shape = shape,
                    is24Hour = is24Hour,
                    onToggleBookmark = { onToggleBookmark(journal.id) },
                    onJournalClick = onJournalClick?.let { { it(journal) } },
                    onLongClick = { onLongClick(journal) }
                )
            } else {
                JournalCard(
                    journal = journal,
                    shape = shape,
                    is24Hour = is24Hour,
                    showDate = true,
                    onToggleBookmark = { onToggleBookmark(journal.id) },
                    onJournalClick = onJournalClick?.let { { it(journal) } },
                    onLongClick = { onLongClick(journal) }
                )
            }
        }
    }
}
