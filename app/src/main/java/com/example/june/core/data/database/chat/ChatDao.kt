package com.example.june.core.data.database.chat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY lastMessageTimestamp DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getChatById(id: Long): ChatEntity?

    @Query("SELECT * FROM chats WHERE id = :id")
    fun getChatByIdFlow(id: Long): Flow<ChatEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity): Long

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChat(id: Long)

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()

    @Query("SELECT * FROM chats WHERE chatName LIKE '%' || :query || '%'")
    fun searchChats(query: String): Flow<List<ChatEntity>>

    @Transaction
    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatWithMessages(chatId: Long): ChatWithMessages?

    @Transaction
    @Query("SELECT * FROM chats ORDER BY lastMessageTimestamp DESC")
    fun getAllChatsWithMessages(): Flow<List<ChatWithMessages>>
}