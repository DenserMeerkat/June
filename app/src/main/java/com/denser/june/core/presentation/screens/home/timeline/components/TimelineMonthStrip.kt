package com.denser.june.core.presentation.screens.home.timeline.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import com.denser.june.R
import com.denser.june.core.presentation.components.InfiniteMonthStrip
import com.denser.june.core.presentation.components.YearHeader

@Composable
fun TimelineMonthStrip(
    currentMonth: YearMonth,
    isExpanded: Boolean,
    onMonthSelect: (YearMonth) -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfiniteMonthStrip(
        currentMonth = currentMonth,
        modifier = modifier.padding(vertical = 12.dp),
        yearContent = { year -> YearHeader(year) },
        monthContent = { ym, isSelected ->
            MonthStripItem(
                label = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                isSelected = isSelected,
                isExpanded = isSelected && isExpanded,
                onClick = {
                    if (isSelected) {
                        onToggleExpand()
                    } else {
                        onMonthSelect(ym)
                    }
                }
            )
        }
    )
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
        targetValue = if (isExpanded) -180f else 0f,
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
                modifier = Modifier.height(20.dp),
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