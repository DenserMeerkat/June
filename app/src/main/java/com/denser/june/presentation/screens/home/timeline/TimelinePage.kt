package com.denser.june.presentation.screens.home.timeline

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.utils.*
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.components.journal.ExportJournalSheet
import com.denser.june.presentation.components.journal.JournalOptionsSheet
import com.denser.june.presentation.screens.home.timeline.components.common.TimelineCalendarPage
import androidx.compose.ui.platform.LocalLocale
import java.time.format.TextStyle
import com.denser.june.presentation.components.InfiniteMonthStrip
import com.denser.june.presentation.components.YearHeader
import com.denser.june.presentation.screens.home.timeline.components.common.MonthStripItem
import com.denser.june.presentation.screens.home.timeline.components.common.TimelineTabs
import com.denser.june.presentation.utils.UiUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelinePage(
    viewModel: TimelineVM = koinViewModel()
) {
    val navigator = koinInject<AppNavigator>()
    val timeFormat by viewModel.timeFormat.collectAsStateWithLifecycle()
    val is24Hour = timeFormat == TimeFormat.TWENTY_FOUR_HOUR
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val journalsInMonth by viewModel.journalsInMonth.collectAsStateWithLifecycle()
    val isCalendarExpanded by viewModel.isCalendarExpanded.collectAsStateWithLifecycle()
    val startOfWeek by viewModel.startOfWeek.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var selectedJournalForOptions by remember { mutableStateOf<Journal?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var journalToExport by remember { mutableStateOf<Journal?>(null) }

    ExportJournalSheet(
        journal = journalToExport,
        onDismiss = { journalToExport = null }
    )

    val currentJournalForOptions = remember(selectedJournalForOptions, journalsInMonth) {
        val id = selectedJournalForOptions?.id ?: return@remember null
        journalsInMonth.find { it.id == id }
    }

    if (currentJournalForOptions != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedJournalForOptions = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            JournalOptionsSheet(
                journal = currentJournalForOptions,
                is24Hour = is24Hour,
                onDismiss = { selectedJournalForOptions = null },
                onToggleBookmark = {
                    viewModel.toggleBookmark(currentJournalForOptions.id)
                },
                onToggleLock = {
                    viewModel.toggleLock(currentJournalForOptions.id)
                },
                onExportMarkdown = {
                    journalToExport = currentJournalForOptions
                    selectedJournalForOptions = null
                },
                onDeleteOrRestore = {
                    if (currentJournalForOptions.isDeleted) {
                        viewModel.restoreJournal(currentJournalForOptions.id)
                    } else {
                        viewModel.deleteJournal(currentJournalForOptions.id)
                    }
                }
            )
        }
    }

    val nonDraftJournalsInMonth = journalsInMonth.filter { !it.isDraft }
    val weeksInMonth = remember(currentMonth, startOfWeek) { getWeeksInMonth(currentMonth, startOfWeek) }
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

    val haptic = LocalHapticFeedback.current
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
            if (!isProgrammaticScroll) {
                val newMonth = viewModel.getMonthForPage(page)
                if (newMonth != currentMonth) {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
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

    var targetScrollDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        val platformLocale = LocalLocale.current.platformLocale
        InfiniteMonthStrip(
            currentMonth = currentMonth,
            modifier = Modifier.padding(vertical = 12.dp),
            yearContent = { year -> YearHeader(year) },
            monthContent = { ym, isSelected ->
                MonthStripItem(
                    label = ym.month.getDisplayName(TextStyle.SHORT, platformLocale),
                    isSelected = isSelected,
                    isExpanded = isSelected && calendarHeightPx > 0,
                    onClick = {
                        if (isSelected) {
                            viewModel.setCalendarExpanded(!isCalendarExpanded)
                        } else {
                            viewModel.onMonthChange(ym)
                        }
                    }
                )
            }
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
                    val pageJournals = if (monthForPage == currentMonth) nonDraftJournalsInMonth else emptyList()
                    TimelineCalendarPage(
                        yearMonth = monthForPage,
                        selectedDate = selectedDate,
                        journals = pageJournals,
                        startOfWeek = startOfWeek,
                        onDateClick = { clickedDate, dayJournals ->
                            selectedDate = clickedDate
                            if (dayJournals.isEmpty()) {
                                val dateMillis = combineDateAndTime(clickedDate, null)
                                navigator.navigateTo(Route.Editor(initialDate = dateMillis))
                            } else {
                                viewModel.onTabChange(TimelineTab.Journals)
                                targetScrollDate = clickedDate
                            }
                        },
                        onDateLongClick = { clickedDate ->
                            selectedDate = clickedDate
                            val dateMillis = combineDateAndTime(clickedDate, null)
                            navigator.navigateTo(Route.Editor(initialDate = dateMillis))
                        }
                    )
                }
            }
        }

        TimelineTabs(
            selectedTab = selectedTab,
            journals = nonDraftJournalsInMonth,
            onTabSelected = { viewModel.onTabChange(it) },
            modifier = Modifier.weight(1f),
            bottomPadding = UiUtils.BOTTOM_BAR_PADDING,
            is24Hour = is24Hour,
            targetScrollDate = targetScrollDate,
            onScrollConsumed = { targetScrollDate = null },
            onToggleBookmark = { id -> viewModel.toggleBookmark(id) },
            onLongClickJournal = { journal -> selectedJournalForOptions = journal }
        )
    }
}