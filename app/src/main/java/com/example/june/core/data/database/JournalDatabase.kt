package com.example.june.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [JournalEntity::class], version = 3, exportSchema = false)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    companion object {
        const val DB_NAME = "journal_database"
    }
}