package com.example.june.core.presentation.screens.journal

import com.example.june.core.domain.data_classes.JournalLocation

sealed interface JournalAction {
    data class ChangeTitle(val title: String) : JournalAction
    data class ChangeContent(val content: String) : JournalAction
    data class ChangeEmoji(val emoji: String?) : JournalAction
    data class ChangeDateTime(val dateTime: Long) : JournalAction

    data class AddImage(val uri: String) : JournalAction
    data class AddImages(val uris: List<String>) : JournalAction
    data class RemoveImage(val uri: String) : JournalAction
    data class MoveImageToFront(val uri: String) : JournalAction

    data class FetchSong(val url: String) : JournalAction
    data object RemoveSong : JournalAction

    data class SetLocation(val location: JournalLocation) : JournalAction
    data object RemoveLocation : JournalAction

    data class SetEditMode(val isEdit: Boolean) : JournalAction
    data object ToggleBookmark : JournalAction
    data object ToggleArchive : JournalAction

    data object SaveJournal : JournalAction
    data object NavigateBack : JournalAction
    data object DeleteJournal : JournalAction
}