package com.denser.june.core.data.database.journal

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        JournalEntity::class,
        TagEntity::class,
        JournalTagCrossRef::class
    ],
    version = 3,
    exportSchema = true
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    companion object {
        const val DB_NAME = "journal_database"
    }
}