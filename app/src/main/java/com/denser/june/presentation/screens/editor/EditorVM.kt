package com.denser.june.presentation.screens.editor

import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.repository.SongRepository
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.domain.model.SongFetchEvent
import com.denser.june.core.domain.model.SongFetchProgress
import com.denser.june.core.utils.FileUtils
import com.denser.june.core.utils.getTodayAtMidnight
import com.denser.hyphen.model.TriggerConfig
import com.denser.hyphen.state.HyphenTextState
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

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

    val hyphenState = HyphenTextState(
        initialText = editorRoute?.initialContent ?: "",
        initialTriggerConfigs = listOf(
            TriggerConfig(trigger = "@", scheme = "person"),
            TriggerConfig(trigger = "#", scheme = "topic")
        )
    )

    private var existingJournal: Journal? = null

    private var searchTagsJob: Job? = null

    private val _state = MutableStateFlow(
        run {
            val routeDate = editorRoute?.initialDate
            val initialTitle = editorRoute?.initialTitle ?: ""
            val initialContent = editorRoute?.initialContent ?: ""
            val initialEmoji = editorRoute?.initialEmoji
            EditorState(
                journalId = journalId,
                title = initialTitle,
                content = initialContent,
                emoji = initialEmoji,
                dateTime = routeDate ?: getTodayAtMidnight(),
                tags = editorRoute?.initialTags ?: emptyList(),
                isDraft = true,
                isLoading = journalId != null,
                isDirty = initialTitle.isNotBlank() || initialContent.isNotBlank() || initialEmoji != null
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
                _state.update { it.copy(startOfWeek = startDay) }
            }
        }

        viewModelScope.launch {
            journalPrefs.timeFormat().collect { format ->
                _state.update { it.copy(timeFormat = format) }
            }
        }

        viewModelScope.launch {
            combine(
                songRepo.getLibrarySongs(),
                journalRepo.getJournals()
            ) { libSongs, journals ->
                val libKeys = libSongs.map { "${it.title.trim().lowercase()}_${it.artistName.trim().lowercase()}" }.toSet()
                val distinctUnimported = journals
                    .mapNotNull { it.songDetails }
                    .filter { song ->
                        val key = "${song.title.trim().lowercase()}_${song.artistName.trim().lowercase()}"
                        key !in libKeys && (song.title.isNotBlank() || song.artistName.isNotBlank())
                    }
                    .distinctBy { "${it.title.trim().lowercase()}_${it.artistName.trim().lowercase()}" }
                libSongs to distinctUnimported
            }.collect { (libSongs, unimported) ->
                _state.update { current ->
                    var activeDetails = current.songDetails
                    val currentPath = activeDetails?.localPreviewPath
                    if (activeDetails != null && (currentPath == null || !File(currentPath).exists())) {
                        val match = libSongs.firstOrNull { libSong ->
                            libSong.title.trim().equals(activeDetails.title.trim(), ignoreCase = true) &&
                            libSong.artistName.trim().equals(activeDetails.artistName.trim(), ignoreCase = true) &&
                            libSong.localPreviewPath?.let { File(it).exists() } == true
                        }
                        if (match != null) {
                            activeDetails = activeDetails.copy(
                                localPreviewPath = match.localPreviewPath,
                                localThumbnailPath = activeDetails.localThumbnailPath ?: match.localThumbnailPath
                            )
                        }
                    }
                    current.copy(librarySongs = libSongs, unimportedJournalSongs = unimported, songDetails = activeDetails)
                }
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
            is EditorAction.SnapContentBaseline -> {
                _state.update { it.copy(content = action.content) }
                existingJournal = existingJournal?.copy(content = action.content)
            }
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
                searchTagsJob?.cancel()
                searchTagsJob = viewModelScope.launch {
                    journalRepo.getTagSuggestions(action.query).collect { suggestions ->
                        _state.update { it.copy(tagSuggestions = suggestions) }
                    }
                }
            }

            is EditorAction.FetchSong -> fetchSongDetails(action.url)
            is EditorAction.AttachLocalSong -> attachLocalSong(action.uri)
            is EditorAction.SelectLibrarySong -> {
                _state.update { it.copy(clipTrimmerSong = action.song, pendingStagedSong = action.song) }
            }
            is EditorAction.RemoveLibrarySong -> {
                viewModelScope.launch {
                    songRepo.removeFromLibrary(action.song)
                }
            }
            is EditorAction.AddSongToLibrary -> {
                viewModelScope.launch {
                    val songKey = "${action.song.title.trim().lowercase()}_${action.song.artistName.trim().lowercase()}"
                    _state.update { it.copy(importingSongKeys = it.importingSongKeys + songKey) }
                    try {
                        val result = songRepo.addToLibrary(action.song)
                        if (result.isSuccess) {
                            val updated = result.getOrThrow()
                            if (_state.value.songDetails?.title == updated.title && _state.value.songDetails?.artistName == updated.artistName) {
                                updateState { it.copy(songDetails = updated) }
                            }
                            _uiEvent.send("Added \"${updated.title}\" to library")
                        } else {
                            _uiEvent.send(result.exceptionOrNull()?.message ?: "Failed to add to library")
                        }
                    } finally {
                        _state.update { it.copy(importingSongKeys = it.importingSongKeys - songKey) }
                    }
                }
            }
            is EditorAction.AddAllSongsToLibrary -> {
                viewModelScope.launch {
                    val allKeys = action.songs.map { "${it.title.trim().lowercase()}_${it.artistName.trim().lowercase()}" }.toSet()
                    _state.update { it.copy(importingSongKeys = it.importingSongKeys + allKeys) }
                    try {
                        var successCount = 0
                        var failureCount = 0
                        for (song in action.songs) {
                            val songKey = "${song.title.trim().lowercase()}_${song.artistName.trim().lowercase()}"
                            try {
                                val result = songRepo.addToLibrary(song)
                                if (result.isSuccess) {
                                    successCount++
                                    val updated = result.getOrThrow()
                                    if (_state.value.songDetails?.title == updated.title && _state.value.songDetails?.artistName == updated.artistName) {
                                        updateState { it.copy(songDetails = updated) }
                                    }
                                } else {
                                    failureCount++
                                }
                            } finally {
                                _state.update { it.copy(importingSongKeys = it.importingSongKeys - songKey) }
                            }
                        }
                        if (successCount > 0 && failureCount == 0) {
                            _uiEvent.send("Added $successCount ${if (successCount == 1) "song" else "songs"} to library")
                        } else if (successCount > 0 && failureCount > 0) {
                            _uiEvent.send("Added $successCount ${if (successCount == 1) "song" else "songs"}, $failureCount failed")
                        } else if (failureCount > 0) {
                            _uiEvent.send("Failed to add songs: no working preview found")
                        }
                    } finally {
                        _state.update { it.copy(importingSongKeys = it.importingSongKeys - allKeys) }
                    }
                }
            }
            is EditorAction.EditLibrarySong -> Unit
            is EditorAction.SaveLibrarySongMeta -> {
                viewModelScope.launch {
                    songRepo.updateLibrarySongMeta(action.original, action.updated)
                    val currentSong = _state.value.songDetails
                    if (currentSong?.localPreviewPath != null && currentSong.localPreviewPath == action.original.localPreviewPath) {
                        updateState { state ->
                            val existing = state.songDetails ?: return@updateState state
                            state.copy(
                                songDetails = existing.copy(
                                    title = if (existing.title == action.original.title) action.updated.title else existing.title,
                                    artistName = if (existing.artistName == action.original.artistName) action.updated.artistName else existing.artistName,
                                    albumName = if (existing.albumName == action.original.albumName) action.updated.albumName else existing.albumName,
                                    genre = if (existing.genre == action.original.genre) action.updated.genre else existing.genre,
                                    localThumbnailPath = if (existing.localThumbnailPath == action.original.localThumbnailPath) action.updated.localThumbnailPath else existing.localThumbnailPath,
                                    thumbnailUrl = if (existing.thumbnailUrl == action.original.thumbnailUrl) action.updated.thumbnailUrl else existing.thumbnailUrl
                                )
                            )
                        }
                    }
                }
            }
            is EditorAction.OpenClipTrimmer -> _state.update { it.copy(clipTrimmerSong = action.songDetails, pendingStagedSong = action.songDetails) }
            is EditorAction.DismissClipTrimmer -> _state.update { it.copy(clipTrimmerSong = null, pendingStagedSong = null) }
            is EditorAction.OpenJournalSongEdit -> _state.update { it.copy(journalEditSongPending = true) }
            is EditorAction.DismissJournalSongEdit -> _state.update { it.copy(journalEditSongPending = false) }
            is EditorAction.SaveJournalSongMeta -> updateState { it.copy(songDetails = action.updated) }
            is EditorAction.SaveClip -> saveClip(action.startMs, action.endMs, action.songDetails)
            is EditorAction.RemoveSong -> updateState { it.copy(songDetails = null) }

            is EditorAction.SetLocation -> updateState { it.copy(location = action.location) }
            is EditorAction.RemoveLocation -> updateState { it.copy(location = null) }

            is EditorAction.ToggleBookmark -> toggleBookmark()
            is EditorAction.ToggleArchive -> toggleArchive()
            is EditorAction.SaveJournal -> saveJournal()
            is EditorAction.NavigateBack -> {
                if (_state.value.isDraft && _state.value.isDirty) {
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
            val dirty = isDirtyCheck(newState)
            if (dirty && newState.isDraft) {
                _saveTrigger.tryEmit(newState)
            }
            newState.copy(isDirty = dirty)
        }
    }

    private fun isDirtyCheck(currentState: EditorState): Boolean {
        val original = existingJournal ?: return currentState.title.isNotBlank() ||
                currentState.content.isNotBlank() ||
                currentState.images.isNotEmpty() ||
                currentState.emoji != null ||
                currentState.tags.isNotEmpty() ||
                currentState.songDetails != null ||
                currentState.location != null

        return original.title.trim() != currentState.title.trim() ||
                original.content.trim() != currentState.content.trim() ||
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
                var details = journal.songDetails
                val detailPath = details?.localPreviewPath
                if (details != null && (detailPath == null || !File(detailPath).exists())) {
                    val match = _state.value.librarySongs.firstOrNull { libSong ->
                        libSong.title.trim().equals(details.title.trim(), ignoreCase = true) &&
                        libSong.artistName.trim().equals(details.artistName.trim(), ignoreCase = true) &&
                        libSong.localPreviewPath?.let { File(it).exists() } == true
                    }
                    if (match != null) {
                        val healed = details.copy(
                            localPreviewPath = match.localPreviewPath,
                            localThumbnailPath = details.localThumbnailPath ?: match.localThumbnailPath
                        )
                        details = healed
                        val updatedJournal = journal.copy(songDetails = healed, updatedAt = System.currentTimeMillis())
                        journalRepo.updateJournal(updatedJournal)
                    }
                }
                existingJournal = journal.copy(songDetails = details)
                if (journal.content.isNotBlank()) {
                    hyphenState.setMarkdownAsync(journal.content)
                }
                _state.update {
                    it.copy(
                        journalId = journal.id,
                        title = journal.title,
                        content = journal.content,
                        emoji = journal.emoji,
                        images = journal.images,
                        location = journal.location,
                        songDetails = details,
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

            val currentMarkdown = if (hyphenState.text.isNotEmpty()) hyphenState.toMarkdown() else currentState.content

            if (currentState.title.isBlank() &&
                currentMarkdown.isBlank() &&
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
                content = currentMarkdown,
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
                _state.update { it.copy(journalId = newId, content = currentMarkdown, isDirty = false, isDraft = true) }
            } else {
                val imagesToDelete = existingJournal?.images.orEmpty().toSet() - currentState.images.toSet()
                imagesToDelete.forEach { FileUtils.deleteMedia(it) }
                journalRepo.updateJournal(journalToSave)
                existingJournal = journalToSave
                _state.update { it.copy(content = currentMarkdown, isDirty = false) }
            }
        }
    }

    private fun saveJournal() {
        viewModelScope.launch {
            val currentState = _state.value
            val currentTime = System.currentTimeMillis()
            val currentMarkdown = if (hyphenState.text.isNotEmpty()) hyphenState.toMarkdown() else currentState.content

            val journalToSave = Journal(
                id = existingJournal?.id ?: "",
                title = currentState.title,
                content = currentMarkdown,
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
            _state.update { it.copy(isFetchingSong = true, songFetchProgress = SongFetchProgress(1, 4, "Resolving song link…")) }
            songRepo.fetchSongDetailsWithProgress(trimmedUrl).collect { event ->
                when (event) {
                    is SongFetchEvent.Progress -> {
                        _state.update { it.copy(songFetchProgress = SongFetchProgress(event.step, event.totalSteps, event.label)) }
                    }
                    is SongFetchEvent.Success -> {
                        _state.update {
                            it.copy(
                                isFetchingSong = false,
                                songFetchProgress = null,
                                pendingStagedSong = event.details,
                                clipTrimmerSong = event.details
                            )
                        }
                    }
                    is SongFetchEvent.Error -> {
                        val error = event.exception
                        val msg = if (error.message?.contains("restricted") == true) {
                            "Internet access is restricted in settings"
                        } else {
                            "Failed to fetch song details"
                        }
                        _state.update {
                            it.copy(
                                isFetchingSong = false,
                                songFetchProgress = SongFetchProgress(0, 4, msg, isError = true, errorMessage = msg)
                            )
                        }
                        _uiEvent.send(msg)
                    }
                }
            }
        }
    }

    fun attachLocalSong(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isFetchingSong = true, songFetchProgress = SongFetchProgress(1, 2, "Importing audio file…")) }
            songRepo.attachLocalAudioWithProgress(uri).collect { event ->
                when (event) {
                    is SongFetchEvent.Progress -> {
                        _state.update { it.copy(songFetchProgress = SongFetchProgress(event.step, event.totalSteps, event.label)) }
                    }
                    is SongFetchEvent.Success -> {
                        _state.update {
                            it.copy(
                                isFetchingSong = false,
                                songFetchProgress = null,
                                pendingStagedSong = event.details,
                                clipTrimmerSong = event.details
                            )
                        }
                    }
                    is SongFetchEvent.Error -> {
                        val msg = "Failed to attach audio: ${event.exception.message ?: "Unknown error"}"
                        _state.update {
                            it.copy(
                                isFetchingSong = false,
                                songFetchProgress = SongFetchProgress(0, 2, msg, isError = true, errorMessage = msg)
                            )
                        }
                        _uiEvent.send(msg)
                    }
                }
            }
        }
    }

    private fun saveClip(startMs: Long, endMs: Long?, songDetails: SongDetails? = null) {
        val currentSong = songDetails ?: _state.value.clipTrimmerSong ?: _state.value.pendingStagedSong ?: _state.value.songDetails ?: return
        val updatedSong = currentSong.copy(clipStartMs = startMs, clipEndMs = endMs)
        updateState { it.copy(songDetails = updatedSong, clipTrimmerSong = null, pendingStagedSong = null) }
    }
}