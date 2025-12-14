package com.example.june.core.domain.data_classes

data class Message(
    val id: Long = 0,
    val chatId: Long,
    val senderName: String,
    val messageContent: String,
    val timestamp: Long,
    val isIncoming: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)