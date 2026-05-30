package com.denser.june.presentation.components

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
import androidx.compose.foundation.rememberScrollState
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
import androidx.activity.compose.BackHandler
import java.time.LocalDate
import java.time.LocalTime
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
import com.denser.june.core.R
import com.denser.june.core.utils.getDaysInMonthGrid
import com.denser.june.core.utils.toLocalDate
import com.denser.june.core.utils.toLocalTime
import com.denser.june.core.utils.combineDateAndTime
import com.denser.june.core.utils.toFullDate
import com.denser.june.core.utils.toFullTime
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

enum class JuneDateTimePickerMode {
    DATE_ONLY,
    TIME_ONLY,
    BOTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuneDateTimePicker(
    initialDateTimeMillis: Long,
    mode: JuneDateTimePickerMode = JuneDateTimePickerMode.BOTH,
    startOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    is24Hour: Boolean = false,
    initialTab: Int = 0,
    onDateTimeSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val scope = rememberCoroutineScope()
    val initialDate = remember(initialDateTimeMillis) { initialDateTimeMillis.toLocalDate() }
    val initialTime = remember(initialDateTimeMillis) { initialDateTimeMillis.toLocalTime() }
    val anchorMonth = remember { YearMonth.from(initialDate) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var includeTime by remember {
        mutableStateOf(
            when (mode) {
                JuneDateTimePickerMode.DATE_ONLY -> false
                JuneDateTimePickerMode.TIME_ONLY -> true
                JuneDateTimePickerMode.BOTH -> (initialTime != LocalTime.MIDNIGHT || initialTab == 1)
            }
        )
    }
    
    var selectedTab by remember {
        mutableIntStateOf(
            when (mode) {
                JuneDateTimePickerMode.DATE_ONLY -> 0
                JuneDateTimePickerMode.TIME_ONLY -> 1
                JuneDateTimePickerMode.BOTH -> initialTab
            }
        )
    }
    
    var showDialMode by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = is24Hour
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
            .background(Color.Black.copy(alpha = 0.32f))
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
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(top = 10.dp, start = 4.dp, end = 4.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (mode == JuneDateTimePickerMode.BOTH) {
                Row(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = CircleShape
                        )
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val segments = listOf("Date", "Time")
                    segments.forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        val bgContainerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(bgContainerColor)
                                .clickable {
                                    selectedTab = index
                                    if (index == 1) {
                                        includeTime = true
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (index == 0) R.drawable.today_24px else R.drawable.schedule_24px
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = contentColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .animateContentSize()
            ) {
                Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                val selectedTime = remember(timePickerState.hour, timePickerState.minute) {
                    LocalTime.of(timePickerState.hour, timePickerState.minute)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) selectedDate.toFullDate() else selectedTime.toFullTime(is24Hour),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedTab == 0) {
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
                    } else {
                        Button(
                            onClick = {
                                val now = LocalTime.now()
                                timePickerState.hour = now.hour
                                timePickerState.minute = now.minute
                                includeTime = true
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
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
                    Spacer(modifier = Modifier.height(8.dp))
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
                            startOfWeek = startOfWeek,
                            onDateClick = { selectedDate = it }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showDialMode) {
                            TimePicker(state = timePickerState)
                        } else {
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
                    if (selectedTab == 1) {
                        Switch(
                            checked = showDialMode,
                            onCheckedChange = { showDialMode = it },
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        if (showDialMode) R.drawable.schedule_24px else R.drawable.edit_24px
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                onDateTimeSelected(millis)
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
}

@Composable
fun CalendarPage(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    startOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    onDateClick: (LocalDate) -> Unit
) {
    val daysInMonth = remember(yearMonth, startOfWeek) { yearMonth.getDaysInMonthGrid(startOfWeek) }
    val weeks = remember(daysInMonth) { daysInMonth.chunked(7) }
    val cellHeight = 36.dp
    val cellShape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        DaysOfWeekHeader(
            modifier = Modifier.padding(bottom = 8.dp),
            startOfWeek = startOfWeek
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
            val label = ym.month.getDisplayName(TextStyle.SHORT, LocalLocale.current.platformLocale)

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
