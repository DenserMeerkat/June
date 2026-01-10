package com.denser.june.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.denser.june.core.data.database.journal.JournalDatabase

class DatabaseFactory(
    private val context: Context
) {
    fun createJournalDatabase(): RoomDatabase.Builder<JournalDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(JournalDatabase.DB_NAME)

        return Room.databaseBuilder(
            appContext,
            JournalDatabase::class.java, 
            dbFile.absolutePath
        ).fallbackToDestructiveMigration(dropAllTables = true)
    }
}