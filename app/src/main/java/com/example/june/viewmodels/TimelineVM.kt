package com.example.june.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.june.R
import com.example.june.core.domain.JournalRepo
import com.example.june.core.domain.data_classes.Journal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.YearMonth
import java.time.ZoneId

enum class TimelineTab(val label: String, val iconRes: Int) {
    Journals("Journals", R.drawable.list_alt_24px),
    Media("Media", R.drawable.art_track_24px),
    Music("Music", R.drawable.music_note_24px),
    Map("Map", R.drawable.location_on_24px)
}

class TimelineVM(
    private val repo: JournalRepo
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth = _currentMonth.asStateFlow()

    private val _selectedTab = MutableStateFlow(TimelineTab.Journals)
    val selectedTab = _selectedTab.asStateFlow()

    private val _isCalendarExpanded = MutableStateFlow(true)
    val isCalendarExpanded = _isCalendarExpanded.asStateFlow()

    val initialPage = Int.MAX_VALUE / 2

    @OptIn(ExperimentalCoroutinesApi::class)
    val journalsInMonth: StateFlow<List<Journal>> = _currentMonth.flatMapLatest { month ->
        val start = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = month.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
            .toEpochMilli()
        repo.getJournalsByDateRange(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onMonthChange(newMonth: YearMonth) {
        _currentMonth.value = newMonth
    }

    fun onTabChange(tab: TimelineTab) {
        _selectedTab.value = tab
    }

    fun setCalendarExpanded(expanded: Boolean) {
        _isCalendarExpanded.value = expanded
    }

    fun getMonthForPage(page: Int): YearMonth {
        val diff = page - initialPage
        return YearMonth.now().plusMonths(diff.toLong())
    }

    fun getPageForMonth(month: YearMonth): Int {
        val now = YearMonth.now()
        val diff = (month.year - now.year) * 12 + (month.monthValue - now.monthValue)
        return initialPage + diff
    }
}