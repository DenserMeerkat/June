package com.denser.june.core.data.database.journal

import androidx.room.Database
import androidx.room.RoomDatabase

import com.denser.june.core.data.database.song.SongLibraryDao
import com.denser.june.core.data.database.song.SongLibraryEntity

@Database(
    entities = [
        JournalEntity::class,
        TagEntity::class,
        JournalTagCrossRef::class,
        DeletedJournalTombstone::class,
        SongLibraryEntity::class
    ],
    version = JournalDatabase.VERSION,
    exportSchema = true
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    abstract fun songLibraryDao(): SongLibraryDao

    companion object {
        const val VERSION = 5
        const val DB_NAME = "journal_database"
    }
}