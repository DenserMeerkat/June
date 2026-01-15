package com.denser.june.core.presentation.screens.home.timeline

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.presentation.screens.home.timeline.components.TimelineCalendarPage
import com.denser.june.core.presentation.screens.home.timeline.components.TimelineMonthStrip
import com.denser.june.core.presentation.screens.home.timeline.components.TimelineTabs
import com.denser.june.viewmodels.TimelineVM
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs

@Composable
fun TimelinePage(
    viewModel: TimelineVM = koinViewModel()
) {
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val journalsInMonth by viewModel.journalsInMonth.collectAsStateWithLifecycle()
    val isCalendarExpanded by viewModel.isCalendarExpanded.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val weeksInMonth = remember(currentMonth) { getWeeksInMonth(currentMonth) }
    val maxCalendarHeight = remember(weeksInMonth) { (weeksInMonth * 44).dp + 24.dp }
    val maxCalendarHeightPx = with(density) { maxCalendarHeight.toPx() }
    val minCalendarHeightPx = 0f

    val initialHeight = if (isCalendarExpanded) maxCalendarHeightPx else minCalendarHeightPx
    var calendarHeightPx by remember { mutableFloatStateOf(initialHeight) }
    val heightAnimatable = remember { Animatable(initialHeight) }

    LaunchedEffect(isCalendarExpanded, maxCalendarHeightPx) {
        val targetHeight = if (isCalendarExpanded) maxCalendarHeightPx else minCalendarHeightPx
        if (calendarHeightPx != targetHeight) {
            heightAnimatable.animateTo(
                targetValue = targetHeight,
                animationSpec = tween(durationMillis = 300)
            ) {
                calendarHeightPx = value
            }
        }
    }

    val nestedScrollConnection = remember(maxCalendarHeightPx, isCalendarExpanded) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0 && calendarHeightPx > minCalendarHeightPx) {
                    val newHeight = calendarHeightPx + available.y
                    val coercedHeight = newHeight.coerceIn(minCalendarHeightPx, maxCalendarHeightPx)
                    if (coercedHeight != calendarHeightPx) {
                        calendarHeightPx = coercedHeight
                        scope.launch { heightAnimatable.snapTo(coercedHeight) }
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && calendarHeightPx < maxCalendarHeightPx) {
                    val newHeight = calendarHeightPx + available.y
                    val coercedHeight = newHeight.coerceIn(minCalendarHeightPx, maxCalendarHeightPx)
                    if (coercedHeight != calendarHeightPx) {
                        calendarHeightPx = coercedHeight
                        scope.launch { heightAnimatable.snapTo(coercedHeight) }
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val targetState = calendarHeightPx > maxCalendarHeightPx / 2
                val targetHeight = if (targetState) maxCalendarHeightPx else minCalendarHeightPx

                if (targetState != isCalendarExpanded) {
                    viewModel.setCalendarExpanded(targetState)
                } else {
                    scope.launch {
                        heightAnimatable.animateTo(
                            targetValue = targetHeight,
                            animationSpec = tween(durationMillis = 300)
                        ) {
                            calendarHeightPx = value
                        }
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = viewModel.initialPage,
        pageCount = { Int.MAX_VALUE }
    )

    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
            if (!isProgrammaticScroll) {
                val newMonth = viewModel.getMonthForPage(page)
                if (newMonth != currentMonth) {
                    viewModel.onMonthChange(newMonth)
                }
            }
        }
    }

    LaunchedEffect(currentMonth) {
        val targetPage = viewModel.getPageForMonth(currentMonth)
        if (targetPage != pagerState.currentPage) {
            isProgrammaticScroll = true
            try {
                pagerState.animateScrollToPage(targetPage)
            } finally {
                isProgrammaticScroll = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        TimelineMonthStrip(
            currentMonth = currentMonth,
            isExpanded = calendarHeightPx > 0,
            onMonthSelect = { month -> viewModel.onMonthChange(month) },
            onToggleExpand = { viewModel.setCalendarExpanded(!isCalendarExpanded) }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { calendarHeightPx.toDp() })
                .clipToBounds()
                .graphicsLayer {
                    alpha = (calendarHeightPx / maxCalendarHeightPx).coerceIn(0f, 1f)
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.Top
            ) { page ->
                val monthForPage = viewModel.getMonthForPage(page)
                if (abs(pagerState.currentPage - page) <= 1) {
                    val pageJournals = if (monthForPage == currentMonth) journalsInMonth else emptyList()
                    TimelineCalendarPage(
                        yearMonth = monthForPage,
                        selectedDate = selectedDate,
                        journals = pageJournals,
                        onDateSelected = { selectedDate = it }
                    )
                }
            }
        }

        TimelineTabs(
            selectedTab = selectedTab,
            journals = journalsInMonth,
            onTabSelected = { viewModel.onTabChange(it) },
            modifier = Modifier.weight(1f),
            bottomPadding = 80.dp
        )
    }
}

fun getWeeksInMonth(yearMonth: YearMonth): Int {
    val firstDay = yearMonth.atDay(1)
    val lastDay = yearMonth.atEndOfMonth()
    val weekFields = WeekFields.of(Locale.getDefault())

    val weekOne = firstDay.get(weekFields.weekOfWeekBasedYear())
    var weekLast = lastDay.get(weekFields.weekOfWeekBasedYear())

    if (weekLast < weekOne) {
        weekLast += 52
    }
    return (weekLast - weekOne) + 1
}