package com.example.june.core.domain.data_classes

data class Chat(
    val id: Long = 0,
    val chatName: String,
    val chatAvatarUrl: String? = null,
    val lastMessagePreview: String? = null,
    val lastMessageTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)