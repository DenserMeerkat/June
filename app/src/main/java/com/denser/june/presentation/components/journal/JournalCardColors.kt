package com.denser.june.presentation.components.journal

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.denser.june.core.domain.model.Journal

data class JournalCardColors(
    val containerColor: Color,
    val innerContainerColor: Color,
    val dateColor: Color
)

@Composable
fun rememberJournalCardColors(journal: Journal): JournalCardColors {
    val containerColor = when {
        journal.isDraft -> MaterialTheme.colorScheme.surfaceContainerLow
        journal.isBookmarked -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val innerContainerColor = if (journal.isBookmarked) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val dateColor = when {
        journal.isDeleted -> MaterialTheme.colorScheme.error
        journal.isDraft -> MaterialTheme.colorScheme.tertiary
        journal.isBookmarked -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    }
    return remember(journal.isDraft, journal.isBookmarked, journal.isDeleted) {
        JournalCardColors(containerColor, innerContainerColor, dateColor)
    }
}
