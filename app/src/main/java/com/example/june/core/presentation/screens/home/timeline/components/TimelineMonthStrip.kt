package com.example.june.core.presentation.screens.home.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private sealed interface StripItem {
    data class Month(val yearMonth: YearMonth) : StripItem
    data class Year(val year: Int) : StripItem
}

@Composable
fun TimelineMonthStrip(
    currentMonth: YearMonth,
    onMonthSelect: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val stripItems = remember(currentMonth) {
        val rawRange = (-24..24).map { currentMonth.plusMonths(it.toLong()) }

        buildList {
            rawRange.forEach { ym ->
                // Whenever we hit January, insert the Year item first
                if (ym.monthValue == 1) {
                    add(StripItem.Year(ym.year))
                }
                add(StripItem.Month(ym))
            }
        }
    }

    LaunchedEffect(currentMonth) {
        val index = stripItems.indexOf(StripItem.Month(currentMonth))
        if (index >= 0) {
            listState.animateScrollToItem(index, scrollOffset = -150)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = stripItems,
            key = { item ->
                when (item) {
                    is StripItem.Month -> item.yearMonth.toString()
                    is StripItem.Year -> "Y-${item.year}"
                }
            }
        ) { item ->
            when (item) {
                is StripItem.Month -> {
                    val isSelected = item.yearMonth == currentMonth
                    val label = item.yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

                    MonthStripItem(
                        label = label,
                        isSelected = isSelected,
                        onClick = { onMonthSelect(item.yearMonth) }
                    )
                }
                is StripItem.Year -> {
                    YearStripItem(year = item.year.toString())
                }
            }
        }
    }
}

@Composable
fun MonthStripItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val shape = if (isSelected) CircleShape else RoundedCornerShape(8.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(36.dp)
            .widthIn(min = 48.dp)
            .clip(shape)
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
fun YearStripItem(
    year: String
) {
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(36.dp)
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = year,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}