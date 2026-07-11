package com.denser.june.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.denser.june.core.BuildConfig
import com.denser.june.core.data.database.journal.JournalDatabase
import com.denser.june.core.domain.logging.AppLogger

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
        ).apply {
            addMigrations(
                DatabaseMigrations.MIGRATION_1_2,
                DatabaseMigrations.MIGRATION_2_3,
                DatabaseMigrations.MIGRATION_3_4
            )
            setQueryCallback({ sqlQuery, _ ->
                if (AppLogger.isCategoryEnabled(AppLogger.Category.DATABASE)) {
                    val cleanedQuery = sqlQuery.replace(Regex("\\s+"), " ").trim()
                    val formattedQuery = if (cleanedQuery.length > 200) {
                        cleanedQuery.take(200) + "..."
                    } else {
                        cleanedQuery
                    }
                    AppLogger.d(
                        AppLogger.Category.DATABASE,
                        "RoomDatabase",
                        "Query: $formattedQuery"
                    )
                }
            }, java.util.concurrent.Executor { it.run() })
        }
    }
}