package com.example.june.core.domain.data_classes

data class Journal(
    val id: Long,
    val title: String,
    val content: String,
    val coverImageUri: String?,
    val createdAt: Long,
    val updatedAt: Long?,
    val dateTime: Long,
    val isBookmarked: Boolean = false,
    val isArchived: Boolean = false,
    val isDraft: Boolean = true,
)