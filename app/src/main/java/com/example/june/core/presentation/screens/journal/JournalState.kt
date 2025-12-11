package com.example.june.core.presentation.screens.journal

data class JournalState(
    val title: String = "",
    val content: String = "",
    val coverImageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val dateTime: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false,
    val isArchived: Boolean = false,
    val isLoading: Boolean = false,
    val isDirty: Boolean = false
 ){
    val isEmpty: Boolean
        get() = title.isBlank() && content.isBlank()
}