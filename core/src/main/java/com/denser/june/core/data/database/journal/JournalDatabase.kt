package com.denser.june.core.data.database.journal

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        JournalEntity::class,
        TagEntity::class,
        JournalTagCrossRef::class,
        DeletedJournalTombstone::class,
        ImprovementEntity::class,
        MicroWinEntity::class
    ],
    version = JournalDatabase.VERSION,
    exportSchema = true
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    abstract fun aiDao(): AiDao

    companion object {
        const val VERSION = 5
        const val DB_NAME = "journal_database"
    }
}