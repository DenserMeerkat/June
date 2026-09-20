package com.denser.june.core.data.database.song

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_library")
data class SongLibraryEntity(
    @PrimaryKey
    val contentHash: String,
    val localPath: String,
    val localArtPath: String? = null,
    val sourceUrl: String? = null,
    val sourceType: String = "LINK",
    val title: String,
    val artistName: String,
    val albumName: String? = null,
    val genre: String? = null,
    val thumbnailUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
