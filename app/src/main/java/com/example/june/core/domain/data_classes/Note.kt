package com.example.june.core.domain.data_classes

data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val coverImageUri: String?,
    val createdAt: Long,
    val updatedAt: Long?,
    val dateTime: Long?,
)