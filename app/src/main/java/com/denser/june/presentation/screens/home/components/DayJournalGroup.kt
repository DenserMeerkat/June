package com.denser.june.presentation.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.presentation.components.journal.JournalCard
import com.denser.june.presentation.components.journal.HighlightJournalCard
import com.denser.june.presentation.components.journal.rememberJournalGroupShape
import java.time.LocalDate

fun LazyListScope.dayJournalGroup(
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
    modifier: Modifier = Modifier
) {
    val totalCount = journals.size
    itemsIndexed(
        items = journals,
        key = { _, journal -> journal.id }
    ) { index, journal ->
        val shape = rememberJournalGroupShape(
            index = index,
            totalCount = totalCount
        )

        val isHighlight = journal.id == recentJournalId || allowMediaHighlight
        val isLast = index == totalCount - 1
        val itemModifier = modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 6.dp else 0.dp)
            .animateItem()

        if (isHighlight) {
            HighlightJournalCard(
                journal = journal,
                shape = shape,
                is24Hour = is24Hour,
                actionIcon = actionIcon,
                onActionClick = onActionClick?.let { { it(journal) } },
                onToggleBookmark = onToggleBookmark?.let { { it(journal.id) } },
                onJournalClick = onJournalClick?.let { { it(journal) } },
                onLongClick = { onLongClick(journal) },
                modifier = itemModifier
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
                onLongClick = { onLongClick(journal) },
                modifier = itemModifier
            )
        }
    }
}
