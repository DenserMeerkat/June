package com.denser.june.core.presentation.screens.home.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.june.core.domain.data_classes.Journal
import com.denser.june.core.domain.utils.getDaysInMonthGrid
import com.denser.june.core.presentation.components.DaysOfWeekHeader
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun TimelineCalendarPage(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    journals: List<Journal>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = remember(yearMonth) { yearMonth.getDaysInMonthGrid() }
    val weeks = remember(daysInMonth) { daysInMonth.chunked(7) }

    val journalsByDate = remember(journals) {
        journals.groupBy {
            java.time.Instant.ofEpochMilli(it.dateTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    fun hasEntry(date: LocalDate?): Boolean {
        if (date == null) return false
        return journalsByDate[date]?.isNotEmpty() == true
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        DaysOfWeekHeader(
            modifier = Modifier.padding(bottom = 4.dp),
            textStyle = MaterialTheme.typography.labelSmall
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (i in 0 until 7) {
                        val date = week.getOrNull(i)

                        if (date != null) {
                            val dayJournals = journalsByDate[date]
                            val count = dayJournals?.size ?: 0
                            val hasSelf = count > 0
                            val emoji = dayJournals?.firstNotNullOfOrNull { it.emoji }

                            val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY
                            val connectLeft = hasSelf && !isSunday && hasEntry(date.minusDays(1))

                            val isSaturday = date.dayOfWeek == DayOfWeek.SATURDAY
                            val connectRight = hasSelf && !isSaturday && hasEntry(date.plusDays(1))

                            val startRadius = if (connectLeft) 8.dp else 16.dp
                            val endRadius = if (connectRight) 8.dp else 16.dp

                            val dynamicShape = RoundedCornerShape(
                                topStart = startRadius,
                                bottomStart = startRadius,
                                topEnd = endRadius,
                                bottomEnd = endRadius
                            )

                            Box(modifier = Modifier.weight(1f)) {
                                CalendarDayTile(
                                    date = date,
                                    entryCount = count,
                                    emoji = emoji,
                                    shape = dynamicShape,
                                    onClick = { onDateSelected(date) }
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayTile(
    date: LocalDate,
    entryCount: Int,
    emoji: String?,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit
) {
    val isToday = date == LocalDate.now()
    val hasJournals = entryCount > 0

    val backgroundColor = when {
        hasJournals -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val textColor = when {
        hasJournals -> MaterialTheme.colorScheme.onTertiaryContainer
        isToday -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (emoji != null) {
            Text(
                text = emoji,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (hasJournals || isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}