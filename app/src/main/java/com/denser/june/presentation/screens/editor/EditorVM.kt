package com.denser.june.presentation.screens.editor

import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.repository.SongRepository
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.utils.FileUtils
import com.denser.june.core.utils.getTodayAtMidnight
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class EditorVM(
    savedStateHandle: SavedStateHandle,
    private val journalRepo: JournalRepository,
    private val journalPrefs: JournalPreferences,
    private val songRepo: SongRepository,
    private val navigator: AppNavigator
) : ViewModel() {
    private val editorRoute = savedStateHandle.tryRoute<Route.Editor>()
    private val journalId = editorRoute?.journalId
        ?: savedStateHandle.tryRoute<Route.JournalMedia>()?.journalId
        ?: savedStateHandle.tryRoute<Route.JournalMediaDetail>()?.journalId

    private var existingJournal: Journal? = null

    private val _state = MutableStateFlow(
        run {
            val routeDate = editorRoute?.initialDate
            EditorState(
                dateTime = routeDate ?: getTodayAtMidnight(),
                tags = editorRoute?.initialTags ?: emptyList(),
                isDraft = true,
                isDirty = true
            )
        }
    )
    val state = _state.asStateFlow()

    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val _saveTrigger = MutableSharedFlow<EditorState>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        viewModelScope.launch {
            journalPrefs.startOfWeek().collect { startDay ->
                updateState { it.copy(startOfWeek = startDay) }
            }
        }

        viewModelScope.launch {
            _saveTrigger
                .debounce(5000L)
                .collect { stateToSave ->
                    saveDraft(stateToSave)
                }
        }

        if (journalId != null) {
            loadJournal(journalId)
        }
    }


    private inline fun <reified T : Any> SavedStateHandle.tryRoute(): T? {
        return try {
            toRoute<T>()
        } catch (e: Exception) {
            null
        }
    }

    fun onAction(action: EditorAction) {
        when (action) {
            is EditorAction.ChangeTitle -> updateState { it.copy(title = action.title) }
            is EditorAction.ChangeContent -> updateState { it.copy(content = action.content) }
            is EditorAction.ChangeDateTime -> updateState { it.copy(dateTime = action.dateTime) }
            is EditorAction.ChangeEmoji -> updateState { it.copy(emoji = action.emoji) }

            is EditorAction.AddImage -> updateState { it.copy(images = it.images + action.uri) }
            is EditorAction.AddImages -> updateState { it.copy(images = it.images + action.uris) }

            is EditorAction.RemoveImage -> {
                updateState { it.copy(images = it.images - action.uri) }
            }
            is EditorAction.MoveImageToFront -> {
                val currentImages = _state.value.images.toMutableList()
                if (currentImages.remove(action.uri)) {
                    currentImages.add(action.uri)
                    updateState { it.copy(images = currentImages) }
                }
            }

            is EditorAction.UpdateTags -> updateState { it.copy(tags = action.tags) }
            is EditorAction.SearchTags -> {
                viewModelScope.launch {
                    journalRepo.getTagSuggestions(action.query).collect { suggestions ->
                        _state.update { it.copy(tagSuggestions = suggestions) }
                    }
                }
            }

            is EditorAction.FetchSong -> fetchSongDetails(action.url)
            is EditorAction.RemoveSong -> updateState { it.copy(songDetails = null) }

            is EditorAction.SetLocation -> updateState { it.copy(location = action.location) }
            is EditorAction.RemoveLocation -> updateState { it.copy(location = null) }

            is EditorAction.ToggleBookmark -> toggleBookmark()
            is EditorAction.ToggleArchive -> toggleArchive()
            is EditorAction.SaveJournal -> saveJournal()
            is EditorAction.NavigateBack -> {
                if (_state.value.isDirty) {
                    saveDraft(_state.value)
                }
                navigator.navigateBack()
            }
            is EditorAction.DeleteJournal -> deleteJournal()
            is EditorAction.RestoreJournal -> restoreJournal()
        }
    }

    private fun updateState(update: (EditorState) -> EditorState) {
        _state.update { currentState ->
            val newState = update(currentState)
            if (isDirtyCheck(newState)) {
                _saveTrigger.tryEmit(newState)
            }
            newState.copy(isDirty = isDirtyCheck(newState))
        }
    }

    private fun isDirtyCheck(currentState: EditorState): Boolean {
        val original = existingJournal ?: return true

        return original.title != currentState.title ||
                original.content != currentState.content ||
                original.tags != currentState.tags ||
                original.emoji != currentState.emoji ||
                original.images != currentState.images ||
                original.location != currentState.location ||
                original.dateTime != currentState.dateTime ||
                original.songDetails != currentState.songDetails
    }

    private fun toggleBookmark() {
        viewModelScope.launch {
            val currentState = _state.value
            val newBookmarkState = !currentState.isBookmarked
            _state.update { it.copy(isBookmarked = newBookmarkState) }

            existingJournal?.let {
                val updatedJournal = it.copy(
                    isBookmarked = newBookmarkState,
                    updatedAt = System.currentTimeMillis()
                )
                journalRepo.updateJournal(updatedJournal)
                existingJournal = updatedJournal
            }
        }
    }

    private fun toggleArchive() {
        viewModelScope.launch {
            val currentState = _state.value
            val newArchiveState = !currentState.isArchived
            _state.update { it.copy(isArchived = newArchiveState) }

            existingJournal?.let {
                val updatedJournal = it.copy(
                    isArchived = newArchiveState,
                    updatedAt = System.currentTimeMillis()
                )
                journalRepo.updateJournal(updatedJournal)
                existingJournal = updatedJournal
                if (newArchiveState) navigator.navigateBack()
            }
        }
    }

    private fun loadJournal(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val journal = journalRepo.getJournalById(id)

            if (journal != null) {
                existingJournal = journal
                _state.update {
                    it.copy(
                        journalId = journal.id,
                        title = journal.title,
                        content = journal.content,
                        emoji = journal.emoji,
                        images = journal.images,
                        location = journal.location,
                        songDetails = journal.songDetails,
                        tags = journal.tags,
                        createdAt = journal.createdAt,
                        updatedAt = journal.updatedAt,
                        dateTime = journal.dateTime,
                        isBookmarked = journal.isBookmarked,
                        isArchived = journal.isArchived,
                        isDraft = journal.isDraft,
                        deletedAt = journal.deletedAt,
                        syncedAt = journal.syncedAt,
                        cloudId = journal.cloudId,
                        isLoading = false,
                        isDirty = false
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun saveDraft(currentState: EditorState) {
        viewModelScope.launch {
            if (existingJournal != null && !existingJournal!!.isDraft) return@launch

            if (currentState.title.isBlank() &&
                currentState.content.isBlank() &&
                currentState.emoji == null &&
                currentState.images.isEmpty() &&
                currentState.songDetails == null &&
                currentState.location == null &&
                currentState.tags.isEmpty()
            ) return@launch

            val currentTime = System.currentTimeMillis()
            val isNewEntry = existingJournal == null

            val journalToSave = Journal(
                id = existingJournal?.id ?: "",
                title = currentState.title,
                content = currentState.content,
                emoji = currentState.emoji,
                images = currentState.images,
                location = currentState.location,
                songDetails = currentState.songDetails,
                tags = currentState.tags,
                createdAt = existingJournal?.createdAt ?: currentTime,
                updatedAt = currentTime,
                dateTime = currentState.dateTime,
                isBookmarked = currentState.isBookmarked,
                isArchived = currentState.isArchived,
                isDraft = true
            )

            if (isNewEntry) {
                val newId = journalRepo.insertJournal(journalToSave)
                val savedDraft = journalToSave.copy(id = newId)
                existingJournal = savedDraft
                _state.update { it.copy(journalId = newId, isDirty = false, isDraft = true) }
            } else {
                val imagesToDelete = existingJournal?.images.orEmpty().toSet() - currentState.images.toSet()
                imagesToDelete.forEach { FileUtils.deleteMedia(it) }
                journalRepo.updateJournal(journalToSave)
                existingJournal = journalToSave
                _state.update { it.copy(isDirty = false) }
            }
        }
    }

    private fun saveJournal() {
        viewModelScope.launch {
            val currentState = _state.value
            val currentTime = System.currentTimeMillis()

            val journalToSave = Journal(
                id = existingJournal?.id ?: "",
                title = currentState.title,
                content = currentState.content,
                emoji = currentState.emoji,
                images = currentState.images,
                location = currentState.location,
                songDetails = currentState.songDetails,
                tags = currentState.tags,
                createdAt = existingJournal?.createdAt ?: currentTime,
                updatedAt = currentTime,
                dateTime = currentState.dateTime,
                isBookmarked = currentState.isBookmarked,
                isArchived = currentState.isArchived,
                isDraft = false
            )

            if (existingJournal != null) {
                journalRepo.updateJournal(journalToSave)
                existingJournal = journalToSave
            } else {
                val newId = journalRepo.insertJournal(journalToSave)
                existingJournal = journalToSave.copy(id = newId)
                _state.update { it.copy(journalId = newId) }
            }

            _state.update { it.copy(isDirty = false, isDraft = false) }
        }
    }

    private fun deleteJournal() {
        viewModelScope.launch {
            existingJournal?.let { journal ->
                journalRepo.softDeleteJournal(journal.id)
            }
            navigator.navigateBack()
        }
    }

    private fun restoreJournal() {
        viewModelScope.launch {
            existingJournal?.let { journal ->
                journalRepo.restoreJournal(journal.id)
            }
            navigator.navigateBack()
        }
    }

    fun fetchSongDetails(url: String) {
        viewModelScope.launch {
            val trimmedUrl = url.trim()
            if (trimmedUrl.isBlank()) {
                _uiEvent.send("Link cannot be empty")
                return@launch
            }
            if (!Patterns.WEB_URL.matcher(trimmedUrl).matches()) {
                _uiEvent.send("Invalid URL format")
                return@launch
            }
            _state.update { it.copy(isFetchingSong = true) }
            songRepo.fetchSongDetails(trimmedUrl)
                .onSuccess { details ->
                    updateState { it.copy(songDetails = details, isFetchingSong = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isFetchingSong = false) }
                    error.printStackTrace()
                    _uiEvent.send("Failed to fetch song details")
                }
        }
    }
}