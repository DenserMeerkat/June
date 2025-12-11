package com.example.june.core.presentation.screens.journal

sealed interface JournalAction {
    data class ChangeTitle(val title: String) : JournalAction
    data class ChangeContent(val content: String) : JournalAction
    data class ChangeDateTime(val dateTime: Long?) : JournalAction
    data class ChangeCoverImageUri(val uri: String?) : JournalAction
    data object SaveJournal : JournalAction
    data object NavigateBack : JournalAction
    data object DeleteJournal : JournalAction
}