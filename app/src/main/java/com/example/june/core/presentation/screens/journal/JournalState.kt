package com.example.june.core.presentation.screens.journal

data class JournalState(
    val title: String = "",
    val content: String = "",
    val coverImageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val dateTime: Long? = null,
    val isLoading: Boolean = false
 ){
    val isEmpty: Boolean
        get() = title.isBlank() && content.isBlank()
}