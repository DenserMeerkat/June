package com.example.june.core.data.database.converters

import androidx.room.TypeConverter
import com.example.june.core.domain.data_classes.JournalLocation
import com.example.june.core.domain.data_classes.SongDetails
import kotlinx.serialization.json.Json

class JournalTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromSongDetails(value: SongDetails?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toSongDetails(value: String?): SongDetails? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromJournalLocation(value: JournalLocation?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toJournalLocation(value: String?): JournalLocation? {
        return value?.let { json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromImagesList(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toImagesList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}