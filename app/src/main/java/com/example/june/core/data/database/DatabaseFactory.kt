package com.example.june.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

class DatabaseFactory(
    private val context: Context
) {
    fun create(): RoomDatabase.Builder<JournalDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(JournalDatabase.DB_NAME)

        return Room.databaseBuilder(appContext, dbFile.absolutePath)
    }
}