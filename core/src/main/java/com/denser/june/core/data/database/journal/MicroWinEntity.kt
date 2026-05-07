package com.denser.june.core.data.database.journal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "micro_wins")
data class MicroWinEntity(
    @PrimaryKey
    val id: String,
    val journalId: String,
    val action: String,
    val proof: String,
    val createdAt: Long = System.currentTimeMillis()
)
