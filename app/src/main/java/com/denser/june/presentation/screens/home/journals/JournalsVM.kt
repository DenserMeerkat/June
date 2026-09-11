package com.denser.june.presentation.screens.home.journals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.domain.model.enums.TimeFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class JournalsFeedUiState(
    val isLoading: Boolean = false,
    val journals: List<Journal> = emptyList()
)

private data class InternalFeedFilterState(
    val query: String,
    val isDebouncing: Boolean,
    val bookmarked: Boolean,
    val draft: Boolean,
    val hasLocation: Boolean,
    val hasSong: Boolean,
    val hasMedia: Boolean
)

class JournalsVM(
    private val journalRepo: JournalRepository,
    private val journalPrefs: JournalPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked = _isBookmarked.asStateFlow()

    private val _isDraft = MutableStateFlow(false)
    val isDraft = _isDraft.asStateFlow()

    private val _hasLocation = MutableStateFlow(false)
    val hasLocation = _hasLocation.asStateFlow()

    private val _hasSong = MutableStateFlow(false)
    val hasSong = _hasSong.asStateFlow()

    private val _hasMedia = MutableStateFlow(false)
    val hasMedia = _hasMedia.asStateFlow()

    val hasActiveFilters: StateFlow<Boolean> = combine(
        listOf<Flow<Any>>(_searchQuery, _isBookmarked, _isDraft, _hasLocation, _hasSong, _hasMedia)
    ) { values ->
        val q = values[0] as String
        val bm = values[1] as Boolean
        val dr = values[2] as Boolean
        val loc = values[3] as Boolean
        val sng = values[4] as Boolean
        val med = values[5] as Boolean
        q.isNotEmpty() || bm || dr || loc || sng || med
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val timeFormat: StateFlow<TimeFormat> = journalPrefs.timeFormat()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TimeFormat.TWELVE_HOUR
        )

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val feedUiState: StateFlow<JournalsFeedUiState> = combine(
        listOf(
            _searchQuery,
            _searchQuery.debounce(300),
            _isBookmarked,
            _isDraft,
            _hasLocation,
            _hasSong,
            _hasMedia
        )
    ) { array: Array<Any> ->
        val immediateQuery = array[0] as String
        val debouncedQuery = array[1] as String

        InternalFeedFilterState(
            query = debouncedQuery,
            isDebouncing = immediateQuery != debouncedQuery,
            bookmarked = array[2] as Boolean,
            draft = array[3] as Boolean,
            hasLocation = array[4] as Boolean,
            hasSong = array[5] as Boolean,
            hasMedia = array[6] as Boolean
        )
    }.flatMapLatest { state ->
        if (state.isDebouncing) {
            flowOf(JournalsFeedUiState(isLoading = true, journals = emptyList()))
        } else {
            val isDraftParam: Boolean? = if (state.draft) true else null

            journalRepo.getJournals(
                query = state.query.ifEmpty { null },
                isBookmarked = if (state.bookmarked) true else null,
                isDraft = isDraftParam,
                hasLocation = if (state.hasLocation) true else null,
                hasSong = if (state.hasSong) true else null,
                hasMedia = if (state.hasMedia) true else null
            ).map { journals ->
                JournalsFeedUiState(isLoading = false, journals = journals)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JournalsFeedUiState(isLoading = true, journals = emptyList())
    )

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun resetAllFilters() {
        _searchQuery.value = ""
        _isBookmarked.value = false
        _isDraft.value = false
        _hasLocation.value = false
        _hasSong.value = false
        _hasMedia.value = false
    }

    fun toggleBookmarkFilter() { _isBookmarked.value = !_isBookmarked.value }
    fun toggleDraftFilter() { _isDraft.value = !_isDraft.value }
    fun toggleLocationFilter() { _hasLocation.value = !_hasLocation.value }
    fun toggleSongFilter() { _hasSong.value = !_hasSong.value }
    fun toggleMediaFilter() { _hasMedia.value = !_hasMedia.value }

    fun deleteJournal(id: String) {
        viewModelScope.launch { journalRepo.softDeleteJournal(id) }
    }

    fun toggleBookmark(id: String) {
        viewModelScope.launch { journalRepo.toggleBookmark(id) }
    }

    fun restoreJournal(id: String) {
        viewModelScope.launch { journalRepo.restoreJournal(id) }
    }
}