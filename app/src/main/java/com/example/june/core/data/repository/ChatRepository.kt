package com.example.june.core.data.repository

import com.example.june.core.data.database.chat.ChatDao
import com.example.june.core.data.database.chat.ChatWithMessages
import com.example.june.core.data.database.chat.MessageDao
import com.example.june.core.data.mappers.toChat
import com.example.june.core.data.mappers.toEntity
import com.example.june.core.data.mappers.toMessage
import com.example.june.core.domain.ChatRepo
import com.example.june.core.domain.data_classes.Chat
import com.example.june.core.domain.data_classes.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) : ChatRepo {

    override suspend fun insertChat(chat: Chat): Long {
        return withContext(Dispatchers.IO) {
            chatDao.insertChat(chat.toEntity())
        }
    }

    override fun getAllChats(): Flow<List<Chat>> {
        return chatDao.getAllChats().map { entities ->
            entities.map { it.toChat() }
        }
    }

    override suspend fun getChatById(id: Long): Chat? {
        return withContext(Dispatchers.IO) {
            chatDao.getChatById(id)?.toChat()
        }
    }

    override fun getChatByIdFlow(id: Long): Flow<Chat?> {
        return chatDao.getChatByIdFlow(id).map { it?.toChat() }
    }

    override suspend fun updateChat(chat: Chat) {
        withContext(Dispatchers.IO) {
            chatDao.updateChat(chat.toEntity())
        }
    }

    override suspend fun deleteChat(id: Long) {
        withContext(Dispatchers.IO) {
            chatDao.deleteChat(id)
        }
    }

    override suspend fun deleteAllChats() {
        withContext(Dispatchers.IO) {
            chatDao.deleteAllChats()
        }
    }

    override fun searchChats(query: String): Flow<List<Chat>> {
        return chatDao.searchChats(query).map { entities ->
            entities.map { it.toChat() }
        }
    }

    override suspend fun insertMessage(message: Message): Long {
        return withContext(Dispatchers.IO) {
            val messageId = messageDao.insertMessage(message.toEntity())

            val chat = chatDao.getChatById(message.chatId)
            chat?.let {
                chatDao.updateChat(
                    it.copy(
                        lastMessagePreview = message.messageContent,
                        lastMessageTimestamp = message.timestamp,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            messageId
        }
    }

    override fun getMessagesForChat(chatId: Long): Flow<List<Message>> {
        return messageDao.getMessagesForChat(chatId).map { entities ->
            entities.map { it.toMessage() }
        }
    }

    override suspend fun getMessageById(id: Long): Message? {
        return withContext(Dispatchers.IO) {
            messageDao.getMessageById(id)?.toMessage()
        }
    }

    override suspend fun updateMessage(message: Message) {
        withContext(Dispatchers.IO) {
            messageDao.updateMessage(message.toEntity())
        }
    }

    override suspend fun deleteMessage(id: Long) {
        withContext(Dispatchers.IO) {
            messageDao.deleteMessage(id)
        }
    }

    override suspend fun deleteAllMessagesForChat(chatId: Long) {
        withContext(Dispatchers.IO) {
            messageDao.deleteAllMessagesForChat(chatId)
        }
    }

    override suspend fun getLastMessageForChat(chatId: Long): Message? {
        return withContext(Dispatchers.IO) {
            messageDao.getLastMessageForChat(chatId)?.toMessage()
        }
    }

    override suspend fun getChatWithMessages(chatId: Long): ChatWithMessages? {
        return withContext(Dispatchers.IO) {
            chatDao.getChatWithMessages(chatId)
        }
    }

    override fun getAllChatsWithMessages(): Flow<List<ChatWithMessages>> {
        return chatDao.getAllChatsWithMessages()
    }
}