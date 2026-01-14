package com.denser.june.core.presentation.screens.journal.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.launch
import com.denser.june.R
import com.denser.june.core.domain.utils.getDaysInMonthGrid
import com.denser.june.core.presentation.components.DaysOfWeekHeader
import com.denser.june.core.presentation.components.InfiniteMonthStrip
import com.denser.june.core.presentation.components.YearHeader

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalDatePickerDialog(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val initialDate = remember(initialDateMillis) {
        Instant.ofEpochMilli(initialDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val anchorMonth = remember { YearMonth.from(initialDate) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    val initialPageIndex = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = initialPageIndex) { Int.MAX_VALUE }

    val currentMonth by remember {
        derivedStateOf {
            val monthsToAdd = pagerState.currentPage - initialPageIndex
            anchorMonth.plusMonths(monthsToAdd.toLong())
        }
    }
    val headerFormatter =
        remember { DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .clickable(enabled = false) {}
                .animateContentSize()
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = headerFormatter.format(selectedDate),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            val today = LocalDate.now()
                            selectedDate = today
                            val monthsDiff =
                                ChronoUnit.MONTHS.between(anchorMonth, YearMonth.from(today))
                            scope.launch {
                                pagerState.animateScrollToPage(initialPageIndex + monthsDiff.toInt())
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.today_24px),
                            contentDescription = "Jump to Today",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                ) {
                    DialogDateMonthStrip(
                        currentMonth = currentMonth,
                        onMonthSelect = { targetMonth ->
                            val monthsDiff = ChronoUnit.MONTHS.between(anchorMonth, targetMonth)
                            scope.launch {
                                pagerState.animateScrollToPage(initialPageIndex + monthsDiff.toInt())
                            }
                        }
                    )
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(8.dp, 12.dp),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    val monthForPage = remember(page) {
                        val monthsToAdd = page - initialPageIndex
                        anchorMonth.plusMonths(monthsToAdd.toLong())
                    }
                    CalendarPage(
                        yearMonth = monthForPage,
                        selectedDate = selectedDate,
                        onDateClick = { selectedDate = it }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            val millis = selectedDate.atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                            onDateSelected(millis)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarPage(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    onDateClick: (LocalDate) -> Unit
) {
    val daysInMonth = remember(yearMonth) { yearMonth.getDaysInMonthGrid() }
    val weeks = remember(daysInMonth) { daysInMonth.chunked(7) }

    val cellHeight = 36.dp
    val cellShape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        DaysOfWeekHeader(
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0 until 7) {
                        val date = week.getOrNull(i)
                        Box(modifier = Modifier.weight(1f)) {
                            if (date != null) {
                                val isSelected = date == selectedDate
                                val isToday = date == LocalDate.now()

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(cellHeight)
                                        .clip(cellShape)
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            when {
                                                isToday -> BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                )
                                                else -> BorderStroke(0.dp, Color.Transparent)
                                            },
                                            shape = cellShape
                                        )
                                        .clickable { onDateClick(date) }
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                                            isToday -> MaterialTheme.colorScheme.onTertiaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(cellHeight))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialogDateMonthStrip(
    currentMonth: YearMonth,
    onMonthSelect: (YearMonth) -> Unit
) {
    InfiniteMonthStrip(
        currentMonth = currentMonth,
        modifier = Modifier.height(56.dp),
        yearContent = { year ->
            YearHeader(
                year = year,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        monthContent = { ym, isSelected ->
            val label = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

            val backgroundColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }

            val textColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            val shape = if (isSelected) CircleShape else RoundedCornerShape(8.dp)

            Box(
                modifier = Modifier
                    .height(36.dp)
                    .widthIn(min = 48.dp)
                    .clip(shape)
                    .background(backgroundColor)
                    .clickable { onMonthSelect(ym) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    )
}