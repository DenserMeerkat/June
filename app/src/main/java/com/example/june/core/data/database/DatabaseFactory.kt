package com.example.june.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.june.core.data.database.chat.ChatDatabase
import com.example.june.core.data.database.journal.JournalDatabase

class DatabaseFactory(
    private val context: Context
) {
    fun createJournalDatabase(): RoomDatabase.Builder<JournalDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(JournalDatabase.DB_NAME)
        return Room.databaseBuilder(appContext, dbFile.absolutePath)
    }

    fun createChatDatabase(): RoomDatabase.Builder<ChatDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(ChatDatabase.DB_NAME)
        return Room.databaseBuilder(appContext, dbFile.absolutePath)
    }
}