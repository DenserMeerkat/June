package com.example.june.core.data.mappers

import com.example.june.core.data.database.chat.MessageEntity
import com.example.june.core.domain.data_classes.Message
import com.example.june.core.data.database.chat.ChatEntity
import com.example.june.core.domain.data_classes.Chat

fun MessageEntity.toMessage(): Message {
    return Message(
        id = id,
        chatId = chatId,
        senderName = senderName,
        messageContent = messageContent,
        timestamp = timestamp,
        isIncoming = isIncoming,
        createdAt = createdAt
    )
}

fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        chatId = chatId,
        senderName = senderName,
        messageContent = messageContent,
        timestamp = timestamp,
        isIncoming = isIncoming,
        createdAt = createdAt
    )
}

fun ChatEntity.toChat(): Chat {
    return Chat(
        id = id,
        chatName = chatName,
        chatAvatarUrl = chatAvatarUrl,
        lastMessagePreview = lastMessagePreview,
        lastMessageTimestamp = lastMessageTimestamp,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Chat.toEntity(): ChatEntity {
    return ChatEntity(
        id = id,
        chatName = chatName,
        chatAvatarUrl = chatAvatarUrl,
        lastMessagePreview = lastMessagePreview,
        lastMessageTimestamp = lastMessageTimestamp,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}