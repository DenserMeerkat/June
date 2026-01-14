package com.denser.june.core.presentation.screens.journal

import com.denser.june.core.domain.data_classes.JournalLocation
import com.denser.june.core.domain.data_classes.SongDetails
import com.denser.june.core.domain.utils.getTodayAtMidnight

data class JournalState(
    val journalId: Long? = null,
    val title: String = "",
    val content: String = "",
    val emoji: String? = null,
    val images: List<String> = emptyList(),
    val location: JournalLocation? = null,
    val songDetails: SongDetails? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val dateTime: Long = getTodayAtMidnight(),
    val isBookmarked: Boolean = false,
    val isArchived: Boolean = false,
    val isLoading: Boolean = false,
    val isDirty: Boolean = false,
    val isDraft: Boolean = true,
    val isEditMode: Boolean = true,
    val isFetchingSong: Boolean = false
)