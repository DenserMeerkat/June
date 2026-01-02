package com.example.june.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.june.core.domain.JournalRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class SearchVM(
    private val repo: JournalRepo
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

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults = combine(
        _searchQuery.debounce(300),
        _isBookmarked,
        _isDraft,
        _hasLocation,
        _hasSong
    ) { query, bookmarked, draft, loc, song ->
        SearchState(query, bookmarked, draft, loc, song)
    }.flatMapLatest { state ->
        val bookmarkFilter = if (state.bookmarked) true else null
        val draftFilter = if (state.draft) true else null
        val locationFilter = if (state.hasLocation) true else null
        val songFilter = if (state.hasSong) true else null

        repo.getFilteredJournals(
            query = state.query,
            isBookmarked = bookmarkFilter,
            isDraft = draftFilter,
            hasLocation = locationFilter,
            hasSong = songFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun toggleBookmarkFilter() { _isBookmarked.value = !_isBookmarked.value }
    fun toggleDraftFilter() { _isDraft.value = !_isDraft.value }
    fun toggleLocationFilter() { _hasLocation.value = !_hasLocation.value }
    fun toggleSongFilter() { _hasSong.value = !_hasSong.value }
}

data class SearchState(
    val query: String,
    val bookmarked: Boolean,
    val draft: Boolean,
    val hasLocation: Boolean,
    val hasSong: Boolean
)