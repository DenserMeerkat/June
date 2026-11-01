package com.denser.june.presentation.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.presentation.components.journal.JournalCard
import com.denser.june.presentation.components.journal.HighlightJournalCard
import com.denser.june.presentation.components.journal.rememberJournalGroupShape
import java.time.LocalDate

@Composable
fun DayJournalGroup(
    modifier: Modifier = Modifier,
    date: LocalDate,
    journals: List<Journal>,
    is24Hour: Boolean,
    recentJournalId: String? = null,
    allowMediaHighlight: Boolean = false,
    actionIcon: Int? = null,
    onActionClick: ((Journal) -> Unit)? = null,
    onToggleBookmark: ((String) -> Unit)? = null,
    onJournalClick: ((Journal) -> Unit)? = null,
    onLongClick: (Journal) -> Unit,
) {

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        journals.forEachIndexed { index, journal ->
            val shape = rememberJournalGroupShape(
                index = index,
                totalCount = journals.size
            )

            val isHighlight = journal.id == recentJournalId || allowMediaHighlight

            if (isHighlight) {
                HighlightJournalCard(
                    journal = journal,
                    shape = shape,
                    is24Hour = is24Hour,
                    actionIcon = actionIcon,
                    onActionClick = onActionClick?.let { { it(journal) } },
                    onToggleBookmark = onToggleBookmark?.let { { it(journal.id) } },
                    onJournalClick = onJournalClick?.let { { it(journal) } },
                    onLongClick = { onLongClick(journal) }
                )
            } else {
                JournalCard(
                    journal = journal,
                    shape = shape,
                    is24Hour = is24Hour,
                    showDate = true,
                    actionIcon = actionIcon,
                    onActionClick = onActionClick?.let { { it(journal) } },
                    onToggleBookmark = onToggleBookmark?.let { { it(journal.id) } },
                    onJournalClick = onJournalClick?.let { { it(journal) } },
                    onLongClick = { onLongClick(journal) }
                )
            }
        }
    }
}
