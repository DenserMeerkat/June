package com.example.june.core.presentation.screens.journal

import com.example.june.core.domain.data_classes.JournalLocation
import com.example.june.core.domain.data_classes.SongDetails

data class JournalState(
    val journalId: Long? = null,
    val title: String = "",
    val content: String = "",
    val images: List<String> = emptyList(),
    val location: JournalLocation? = null,
    val songDetails: SongDetails? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val dateTime: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false,
    val isArchived: Boolean = false,
    val isLoading: Boolean = false,
    val isDirty: Boolean = false,
    val isDraft: Boolean = true,
    val isEditMode: Boolean = true,
    val isFetchingSong: Boolean = false
)