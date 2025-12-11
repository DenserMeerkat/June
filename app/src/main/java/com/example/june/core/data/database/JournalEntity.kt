package com.example.june.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journals")
data class JournalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val title: String,
    val content: String,
    val coverImageUri: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long? = null,
    val dateTime: Long? = null,
)


