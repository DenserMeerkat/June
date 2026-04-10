package com.denser.june.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Journal")
data class Journal(
    @Serializable(with = StringIdSerializer::class)
    val id: String,
    val title: String,
    val content: String,
    val emoji: String? = null,
    val images: List<String> = emptyList(),
    val location: JournalLocation? = null,
    val songDetails: SongDetails? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long?,
    val dateTime: Long,
    val isBookmarked: Boolean = false,
    val isArchived: Boolean = false,
    val isDraft: Boolean = true,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val cloudId: String? = null,
) {
    val isDeleted: Boolean get() = deletedAt != null
}

@Serializable
data class JournalLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val name: String? = null,
    val locality: String? = null
)
