package com.example.june.core.data.database.journal

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.june.core.data.database.converters.JournalTypeConverters
import com.example.june.core.domain.data_classes.JournalLocation
import com.example.june.core.domain.data_classes.SongDetails

@Entity(tableName = "journals")
@TypeConverters(JournalTypeConverters::class)
data class JournalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val title: String,
    val content: String,
    val emoji: String? = null,
    val images: List<String> = emptyList(),
    val location: JournalLocation? = null,
    val songDetails: SongDetails? = null,
    val createdAt: Long = 0,
    val updatedAt: Long? = null,
    val dateTime: Long,
    val isBookmarked: Boolean = false,
    val isArchived: Boolean = false,
    val isDraft: Boolean = true,
)