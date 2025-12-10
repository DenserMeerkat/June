package com.example.june.core.presentation.screens.note

sealed interface NoteAction {
    data class ChangeTitle(val title: String) : NoteAction
    data class ChangeContent(val content: String) : NoteAction
    data class ChangeDateTime(val dateTime: Long?) : NoteAction
    data class ChangeCoverImageUri(val uri: String?) : NoteAction
    data object SaveNote : NoteAction
    data object NavigateBack : NoteAction
    data object DeleteNote : NoteAction
}