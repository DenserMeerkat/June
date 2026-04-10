package com.denser.june.core.data.database.journal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_journal_tombstones")
data class DeletedJournalTombstone(
    @PrimaryKey
    val id: String,
    val deletedAt: Long = System.currentTimeMillis()
)
