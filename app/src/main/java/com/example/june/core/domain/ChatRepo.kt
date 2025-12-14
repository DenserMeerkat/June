package com.example.june.core.domain

import com.example.june.core.data.database.chat.ChatWithMessages
import com.example.june.core.domain.data_classes.Chat
import com.example.june.core.domain.data_classes.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepo {
    suspend fun insertChat(chat: Chat): Long
    fun getAllChats(): Flow<List<Chat>>
    suspend fun getChatById(id: Long): Chat?
    fun getChatByIdFlow(id: Long): Flow<Chat?>
    suspend fun updateChat(chat: Chat)
    suspend fun deleteChat(id: Long)
    suspend fun deleteAllChats()
    fun searchChats(query: String): Flow<List<Chat>>

    suspend fun insertMessage(message: Message): Long
    fun getMessagesForChat(chatId: Long): Flow<List<Message>>
    suspend fun getMessageById(id: Long): Message?
    suspend fun updateMessage(message: Message)
    suspend fun deleteMessage(id: Long)
    suspend fun deleteAllMessagesForChat(chatId: Long)
    suspend fun getLastMessageForChat(chatId: Long): Message?

    suspend fun getChatWithMessages(chatId: Long): ChatWithMessages?
    fun getAllChatsWithMessages(): Flow<List<ChatWithMessages>>
}