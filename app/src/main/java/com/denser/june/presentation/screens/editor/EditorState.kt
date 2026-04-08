package com.denser.june.presentation.screens.editor

import com.denser.june.core.domain.model.JournalLocation
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.utils.getTodayAtMidnight

data class EditorState(
    val journalId: String? = null,
    val title: String = "",
    val content: String = "",
    val emoji: String? = null,
    val images: List<String> = emptyList(),
    val location: JournalLocation? = null,
    val songDetails: SongDetails? = null,
    val tags: List<String> = emptyList(),
    val tagSuggestions: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val dateTime: Long = getTodayAtMidnight(),
    val isBookmarked: Boolean = false,
    val isArchived: Boolean = false,
    val isLoading: Boolean = false,
    val isDirty: Boolean = false,
    val isDraft: Boolean = true,
    val isFetchingSong: Boolean = false
) {
    val hasContent: Boolean
        get() = title.isNotBlank() ||
                content.isNotBlank() ||
                emoji != null ||
                images.isNotEmpty() ||
                songDetails != null ||
                location != null ||
                tags.isNotEmpty()
}