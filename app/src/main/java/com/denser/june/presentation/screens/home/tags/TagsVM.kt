package com.denser.june.presentation.screens.home.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.enums.TagCategory
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.presentation.utils.TagUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TagsVM(
    private val repository: JournalRepository,
    private val journalPrefs: JournalPreferences
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(TagCategory.Spaces)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedPrimaryTag = MutableStateFlow<String?>(null)
    val selectedPrimaryTag = _selectedPrimaryTag.asStateFlow()

    private val _selectedFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedFilters = _selectedFilters.asStateFlow()

    val timeFormat: StateFlow<TimeFormat> = journalPrefs.timeFormat()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TimeFormat.TWELVE_HOUR
        )

    val allUniqueTags: StateFlow<List<String>?> = repository.getUniqueTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val primaryTags: StateFlow<List<String>?> =
        combine(allUniqueTags, _selectedCategory) { tags, category ->
            tags?.let { TagUtils.filterTagsByCategory(it, category).sorted() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            primaryTags.collectLatest { tags ->
                if (tags != null && _selectedPrimaryTag.value !in tags) {
                    _selectedPrimaryTag.value = tags.firstOrNull()
                }
            }
        }
    }

    val tagCounts: StateFlow<Map<String, Int>> = repository.getTagCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val journals: StateFlow<List<Journal>?> = combine(
        _selectedPrimaryTag, _selectedFilters
    ) { primaryTag, filters ->
        if (primaryTag == null) return@combine null
        val queryTags = mutableListOf(primaryTag)
        queryTags.addAll(filters)
        queryTags
    }.flatMapLatest { tags ->
        if (tags == null) flowOf(null)
        else if (tags.isEmpty()) flowOf(emptyList())
        else repository.getJournalsByMultipleTags(tags)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableFilters: StateFlow<List<String>> = journals.map { currentJournals ->
        currentJournals?.flatMap { it.tags }
            ?.distinct()
            ?.filter { it != _selectedPrimaryTag.value }
            ?.sorted() ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: TagCategory) {
        _selectedCategory.value = category
        _selectedFilters.value = emptySet()
    }

    fun selectPrimaryTag(tag: String) {
        _selectedPrimaryTag.value = tag
        _selectedFilters.value = emptySet()
    }

    fun renameCurrentTag(newNameInput: String) {
        val oldTag = _selectedPrimaryTag.value ?: return
        val trimmedInput = newNameInput.trim()
        val targetCategory = TagUtils.getCategoryForTag(trimmedInput)
        val cleanName = TagUtils.getCleanTagName(trimmedInput)
        val newTag = TagUtils.appendPrefix(cleanName, targetCategory)

        if (oldTag == newTag) return

        viewModelScope.launch {
            repository.renameTag(oldTag, newTag)
            _selectedCategory.value = targetCategory
            _selectedPrimaryTag.value = newTag
            _selectedFilters.value = emptySet()
        }
    }

    fun deleteCurrentTag() {
        val currentTag = _selectedPrimaryTag.value ?: return
        val currentList = primaryTags.value ?: return

        val nextTag = if (currentList.size > 1) {
            val currentIndex = currentList.indexOf(currentTag)
            if (currentIndex == currentList.lastIndex) {
                currentList.getOrNull(currentIndex - 1)
            } else {
                currentList.getOrNull(currentIndex + 1)
            }
        } else {
            null
        }

        viewModelScope.launch {
            _selectedPrimaryTag.value = nextTag
            repository.deleteTag(currentTag)
        }
    }

    fun toggleFilter(tag: String) {
        val current = _selectedFilters.value
        _selectedFilters.value = if (current.contains(tag)) current - tag else current + tag
    }

    fun clearFilters() {
        _selectedFilters.value = emptySet()
    }

    fun toggleBookmark(id: String) {
        viewModelScope.launch { repository.toggleBookmark(id) }
    }

    fun softDelete(id: String) {
        viewModelScope.launch { repository.softDeleteJournal(id) }
    }

    fun restoreJournal(id: String) {
        viewModelScope.launch { repository.restoreJournal(id) }
    }
}