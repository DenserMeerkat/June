package com.denser.june.presentation.components.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import java.time.LocalDate

@Composable
fun rememberJournalDisplayTitle(journal: Journal): String {
    val defaultTitle = stringResource(R.string.untitled)
    return remember(journal.title, journal.content, journal.emoji, defaultTitle) {
        val rawTitle = journal.title.ifBlank {
            journal.content.lineSequence().firstOrNull { it.isNotBlank() } ?: defaultTitle
        }
        val cleanTitle = rawTitle.replace("#", "").trim()
        val emojiPrefix = journal.emoji?.let { "$it " } ?: ""
        emojiPrefix + cleanTitle
    }
}

@Composable
fun rememberCleanJournalContent(journal: Journal): String {
    return remember(journal.title, journal.content) {
        if (journal.title.isBlank()) {
            val lines = journal.content.lines()
            val firstNonBlankIndex = lines.indexOfFirst { it.isNotBlank() }
            if (firstNonBlankIndex != -1) {
                lines.drop(firstNonBlankIndex + 1).joinToString("\n").trim()
            } else ""
        } else {
            journal.content.trim()
        }
    }
}

fun rememberJournalGroupShape(index: Int, totalCount: Int): RoundedCornerShape {
    return when {
        totalCount <= 1 -> RoundedCornerShape(24.dp)
        index == 0 -> RoundedCornerShape(
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp
        )
        index == totalCount - 1 -> RoundedCornerShape(
            topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp
        )
        else -> RoundedCornerShape(8.dp)
    }
}

@Composable
fun JuneMetadataRow(
    iconRes: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.width(60.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

data class DayJournalGroupData(
    val date: LocalDate,
    val journals: List<Journal>
)
