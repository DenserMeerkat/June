package com.denser.june.presentation.screens.editor

import com.denser.june.core.domain.model.JournalLocation

import android.net.Uri
import com.denser.june.core.domain.model.SongDetails

sealed interface EditorAction {
    data class ChangeTitle(val title: String) : EditorAction
    data class ChangeContent(val content: String) : EditorAction
    data class SnapContentBaseline(val content: String) : EditorAction
    data class ChangeEmoji(val emoji: String?) : EditorAction
    data class ChangeDateTime(val dateTime: Long) : EditorAction

    data class AddImage(val uri: String) : EditorAction
    data class AddImages(val uris: List<String>) : EditorAction
    data class RemoveImage(val uri: String) : EditorAction
    data class MoveImageToFront(val uri: String) : EditorAction

    data class UpdateTags(val tags: List<String>) : EditorAction
    data class SearchTags(val query: String) : EditorAction

    data class FetchSong(val url: String) : EditorAction
    data class AttachLocalSong(val uri: Uri) : EditorAction
    data class SelectLibrarySong(val song: SongDetails) : EditorAction
    data class AddSongToLibrary(val song: SongDetails) : EditorAction
    data class AddAllSongsToLibrary(val songs: List<SongDetails>) : EditorAction
    data class RemoveLibrarySong(val song: SongDetails) : EditorAction
    data class EditLibrarySong(val song: SongDetails) : EditorAction
    data class SaveLibrarySongMeta(val original: SongDetails, val updated: SongDetails) : EditorAction
    data class OpenClipTrimmer(val songDetails: SongDetails) : EditorAction
    data object DismissClipTrimmer : EditorAction
    data class SaveClip(val startMs: Long, val endMs: Long?, val songDetails: SongDetails? = null) : EditorAction
    data object RemoveSong : EditorAction

    data class SetLocation(val location: JournalLocation) : EditorAction
    data object RemoveLocation : EditorAction

    data object ToggleBookmark : EditorAction
    data object ToggleArchive : EditorAction

    data object SaveJournal : EditorAction
    data object NavigateBack : EditorAction
    data object DeleteJournal : EditorAction
    data object RestoreJournal : EditorAction
}