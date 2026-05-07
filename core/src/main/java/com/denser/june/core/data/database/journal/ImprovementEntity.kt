package com.denser.june.core.data.database.journal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "improvements")
data class ImprovementEntity(
    @PrimaryKey
    val id: String,
    val journalId: String,
    val content: String,
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
