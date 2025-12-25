package com.example.june.core.domain.backup

import com.example.june.core.domain.data_classes.JournalLocation
import com.example.june.core.domain.data_classes.SongDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Journal")
data class JournalSchema(
    val id: Long,
    val title: String,
    val content: String,
    val images: List<String> = emptyList(),
    val location: JournalLocation? = null,
    val songDetails: SongDetails? = null,
    val createdAt: Long,
    val updatedAt: Long?,
    val dateTime: Long,
    val isBookmarked: Boolean = false,
    val isArchived: Boolean = false,
    val isDraft: Boolean = true,
)