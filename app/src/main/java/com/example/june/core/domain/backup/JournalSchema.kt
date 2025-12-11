package com.example.june.core.domain.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Journal")
data class JournalSchema(
    val id: Long,
    val title: String,
    val content: String,
    val coverImageUri: String?,
    val createdAt: Long,
    val updatedAt: Long?,
    val dateTime: Long?,
)