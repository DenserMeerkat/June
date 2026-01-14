package com.denser.june.core.presentation.screens.journal.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
import com.denser.june.R
import com.denser.june.core.domain.utils.getDaysInMonthGrid
import com.denser.june.core.domain.utils.toLocalDate
import com.denser.june.core.domain.utils.toLocalTime
import com.denser.june.core.domain.utils.combineDateAndTime
import com.denser.june.core.domain.utils.toFullDate
import com.denser.june.core.domain.utils.toFullTime
import com.denser.june.core.presentation.components.DaysOfWeekHeader
import com.denser.june.core.presentation.components.InfiniteMonthStrip
import com.denser.june.core.presentation.components.YearHeader
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalDatePickerDialog(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val initialDate = remember(initialDateMillis) { initialDateMillis.toLocalDate() }
    val initialTime = remember(initialDateMillis) { initialDateMillis.toLocalTime() }
    val anchorMonth = remember { YearMonth.from(initialDate) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var includeTime by remember { mutableStateOf(initialTime != LocalTime.MIDNIGHT) }
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute
    )
    val initialPageIndex = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = initialPageIndex) { Int.MAX_VALUE }
    val currentMonth by remember {
        derivedStateOf {
            val monthsToAdd = pagerState.currentPage - initialPageIndex
            anchorMonth.plusMonths(monthsToAdd.toLong())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
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
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDate.toFullDate(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            val today = LocalDate.now()
                            selectedDate = today
                            val monthsDiff = ChronoUnit.MONTHS.between(anchorMonth, YearMonth.from(today))
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
                        Text("Today", style = MaterialTheme.typography.labelMedium)
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
                AnimatedVisibility(
                    visible = includeTime,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selectedTime = remember(timePickerState.hour, timePickerState.minute) {
                                LocalTime.of(timePickerState.hour, timePickerState.minute)
                            }
                            Text(
                                text = selectedTime.toFullTime(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    val now = LocalTime.now()
                                    timePickerState.hour = now.hour
                                    timePickerState.minute = now.minute
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.schedule_24px),
                                    contentDescription = "Jump to Now",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Now", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TimeInput(state = timePickerState)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = includeTime,
                        onCheckedChange = { includeTime = it },
                        thumbContent = {
                            Icon(
                                painter = painterResource(R.drawable.schedule_24px),
                                contentDescription = "Set time",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    Row {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                val time = if (includeTime) {
                                    LocalTime.of(timePickerState.hour, timePickerState.minute)
                                } else null

                                val millis = combineDateAndTime(selectedDate, time)
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