package com.denser.june.core.domain.data_classes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Journal")
data class Journal(
    val id: Long,
    val title: String,
    val content: String,
    val emoji: String? = null,
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

@Serializable
data class JournalLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val name: String? = null,
    val locality: String? = null
)