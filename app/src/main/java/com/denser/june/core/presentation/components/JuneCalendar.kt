package com.denser.june.core.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.YearMonth
import java.util.Locale

private val YearHeaderWidth = 60.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfiniteMonthStrip(
    currentMonth: YearMonth,
    modifier: Modifier = Modifier,
    yearContent: @Composable (String) -> Unit = { year -> YearHeader(year) },
    monthContent: @Composable (YearMonth, Boolean) -> Unit
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    val startYear = remember(currentMonth) { currentMonth.year - 50 }
    val endYear = remember(currentMonth) { currentMonth.year + 50 }
    val yearRange = remember(startYear, endYear) { startYear..endYear }

    LaunchedEffect(currentMonth) {
        val yearDiff = currentMonth.year - startYear
        val index = (yearDiff * 13) + currentMonth.monthValue
        val offsetInPx = with(density) { -((YearHeaderWidth - 16.dp).toPx()).toInt() }
        listState.animateScrollToItem(index.coerceAtLeast(0), scrollOffset = offsetInPx)
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        yearRange.forEach { year ->
            stickyHeader(key = "Y-$year") {
                yearContent(year.toString())
            }

            val months = (1..12).map { YearMonth.of(year, it) }
            items(items = months, key = { it.toString() }) { ym ->
                val isSelected = ym == currentMonth
                monthContent(ym, isSelected)
            }
        }
    }
}

@Composable
fun DaysOfWeekHeader(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
) {
    Row(modifier = modifier.fillMaxWidth()) {
        val days = listOf(
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )
        days.forEach { day ->
            Text(
                text = day.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault()),
                style = textStyle,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun YearHeader(
    year: String,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(36.dp)
            .width(YearHeaderWidth)
            .background(containerColor)
    ) {
        Text(
            text = year,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}