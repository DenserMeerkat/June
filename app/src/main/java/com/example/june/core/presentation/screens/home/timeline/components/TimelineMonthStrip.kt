package com.example.june.core.presentation.screens.home.timeline.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.june.R
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineMonthStrip(
    currentMonth: YearMonth,
    isExpanded: Boolean,
    onMonthSelect: (YearMonth) -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val startYear = remember(currentMonth) { currentMonth.year - 5 }
    val endYear = remember(currentMonth) { currentMonth.year + 5 }
    val yearRange = remember(startYear, endYear) { startYear..endYear }

    LaunchedEffect(currentMonth) {
        val yearDiff = currentMonth.year - startYear
        if (yearDiff >= 0) {
            val index = (yearDiff * 13) + currentMonth.monthValue
            listState.animateScrollToItem(index, scrollOffset = -150)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        yearRange.forEach { year ->
            stickyHeader(key = "Y-$year") {
                YearStripItem(year = year.toString())
            }

            val months = (1..12).map { YearMonth.of(year, it) }
            items(items = months, key = { it.toString() }) { ym ->
                val isSelected = ym == currentMonth
                val label = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

                MonthStripItem(
                    label = label,
                    isSelected = isSelected,
                    isExpanded = isExpanded,
                    onClick = {
                        if (isSelected) {
                            onToggleExpand()
                        } else {
                            onMonthSelect(ym)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MonthStripItem(
    label: String,
    isSelected: Boolean,
    isExpanded: Boolean,
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

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "Chevron Rotation"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(36.dp)
            .widthIn(min = 48.dp)
            .clip(shape)
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(start = 16.dp, end = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
        if (isSelected) {
            Spacer(Modifier.width(12.dp))
            VerticalDivider(
                modifier = Modifier
                    .height(20.dp),
                color = textColor.copy(alpha = 0.3f)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.keyboard_arrow_down_24px),
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = textColor,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
fun YearStripItem(year: String) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val backgroundColor = MaterialTheme.colorScheme.surface

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(36.dp)
            .background(backgroundColor)
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = year,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}