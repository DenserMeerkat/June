package com.example.june.core.presentation.screens.home.timeline

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.june.R
import com.example.june.core.domain.data_classes.Journal
import com.example.june.core.presentation.screens.home.journals.components.JournalCard
import com.example.june.core.presentation.screens.home.timeline.components.TimelineCalendarPage
import com.example.june.core.presentation.screens.home.timeline.components.TimelineMonthStrip
import com.example.june.core.presentation.screens.journal.components.JournalMosaicCard
import com.example.june.core.presentation.screens.journal.components.MediaOperations
import com.example.june.viewmodels.TimelineVM
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale

enum class TimelineTab(val label: String, val iconRes: Int) {
    Journals("Journals", R.drawable.list_24px),
    Media("Media", R.drawable.art_track_24px),
    Map("Map", R.drawable.location_on_24px),
    Music("Music", R.drawable.music_note_24px)
}

@Composable
fun TimelinePage(
    viewModel: TimelineVM = koinViewModel()
) {
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val journalsInMonth by viewModel.journalsInMonth.collectAsStateWithLifecycle()
    val sortedJournals = remember(journalsInMonth) {
        journalsInMonth.sortedByDescending { it.dateTime }
    }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTab by remember { mutableStateOf(TimelineTab.Journals) }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val weeksInMonth = remember(currentMonth) { getWeeksInMonth(currentMonth) }
    val maxCalendarHeight = remember(weeksInMonth) {
        (weeksInMonth * 44).dp + 24.dp
    }
    val maxCalendarHeightPx = with(density) { maxCalendarHeight.toPx() }
    val minCalendarHeightPx = 0f
    var calendarHeightPx by remember { mutableFloatStateOf(maxCalendarHeightPx) }
    val heightAnimatable = remember { Animatable(maxCalendarHeightPx) }

    LaunchedEffect(maxCalendarHeightPx) {
        if (calendarHeightPx > 0) {
            heightAnimatable.animateTo(maxCalendarHeightPx, tween(300)) {
                calendarHeightPx = value
            }
        }
    }

    val nestedScrollConnection = remember(maxCalendarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0) {
                    val newHeight = calendarHeightPx + available.y
                    val coercedHeight = newHeight.coerceIn(minCalendarHeightPx, maxCalendarHeightPx)

                    if (coercedHeight != calendarHeightPx) {
                        calendarHeightPx = coercedHeight
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0) {
                    val newHeight = calendarHeightPx + available.y
                    val coercedHeight = newHeight.coerceIn(minCalendarHeightPx, maxCalendarHeightPx)

                    if (coercedHeight != calendarHeightPx) {
                        calendarHeightPx = coercedHeight
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val targetHeight =
                    if (calendarHeightPx > maxCalendarHeightPx / 2) maxCalendarHeightPx else minCalendarHeightPx

                heightAnimatable.snapTo(calendarHeightPx)
                heightAnimatable.animateTo(
                    targetValue = targetHeight,
                    animationSpec = tween(durationMillis = 300)
                ) {
                    calendarHeightPx = value
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    fun toggleCalendar() {
        scope.launch {
            val target = if (calendarHeightPx > 0) minCalendarHeightPx else maxCalendarHeightPx
            heightAnimatable.snapTo(calendarHeightPx)
            heightAnimatable.animateTo(target, tween(300)) {
                calendarHeightPx = value
            }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = viewModel.initialPage,
        pageCount = { Int.MAX_VALUE }
    )

    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        if (!isProgrammaticScroll) {
            val newMonth = viewModel.getMonthForPage(pagerState.currentPage)
            if (newMonth != currentMonth) viewModel.onMonthChange(newMonth)
        }
    }

    LaunchedEffect(currentMonth) {
        val targetPage = viewModel.getPageForMonth(currentMonth)
        if (targetPage != pagerState.currentPage) {
            isProgrammaticScroll = true
            pagerState.animateScrollToPage(targetPage)
            isProgrammaticScroll = false
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
            onToggleExpand = { toggleCalendar() }
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
                if (monthForPage == currentMonth || kotlin.math.abs(pagerState.currentPage - page) <= 1) {
                    val pageJournals =
                        if (monthForPage == currentMonth) journalsInMonth else emptyList()
                    TimelineCalendarPage(
                        yearMonth = monthForPage,
                        selectedDate = selectedDate,
                        journals = pageJournals,
                        onDateSelected = { selectedDate = it }
                    )
                }
            }
        }

        PrimaryTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh) }
        ) {
            TimelineTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label) },
                    icon = {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                TimelineTab.Journals -> JournalListSection(journals = sortedJournals)
                TimelineTab.Media -> MonthMediaGrid(journals = sortedJournals)
                TimelineTab.Map -> MonthMapPlaceholder(journals = sortedJournals)
                TimelineTab.Music -> MonthMusicList(journals = sortedJournals)
            }
        }
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

@Composable
fun JournalListSection(journals: List<Journal>) {
    if (journals.isEmpty()) {
        EmptyStateMessage("No journals this month.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(journals, key = { it.id }) { journal ->
                JournalCard(
                    journal = journal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun MonthMediaGrid(journals: List<Journal>) {
    val allImages = remember(journals) { journals.flatMap { it.images } }

    if (allImages.isEmpty()) {
        EmptyStateMessage("No media this month.")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(allImages) { media ->
                JournalMosaicCard(
                    mediaList = listOf(media),
                    enablePlayback = false,
                    operations = MediaOperations(isEditMode = false),
                    roundedCornerShape = RoundedCornerShape(8.dp),
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }
    }
}

@Composable
fun MonthMapPlaceholder(journals: List<Journal>) {
    val journalsWithLocation = remember(journals) { journals.filter { it.location != null } }

    if (journalsWithLocation.isEmpty()) {
        EmptyStateMessage("No locations visited.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(journalsWithLocation) { journal ->
                ListItem(
                    headlineContent = { Text(journal.location?.address ?: "Unknown") },
                    supportingContent = { Text(journal.dateTime.toString()) },
                    leadingContent = { Icon(painterResource(R.drawable.location_on_24px), null) }
                )
            }
        }
    }
}

@Composable
fun MonthMusicList(journals: List<Journal>) {
    EmptyStateMessage("Music tracking coming soon.")
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}