package com.denser.june.core.data.database.converters

import androidx.room.TypeConverter
import com.denser.june.core.domain.model.FontVariationConfig
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.domain.model.JournalLocation
import com.denser.june.core.domain.model.JournalTheme
import com.denser.june.core.domain.model.SongDetails
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
        if (value.isEmpty()) return "[]"
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toImagesList(value: String): List<String> {
        if (value.isEmpty() || value == "[]") return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromFontVariationConfig(value: FontVariationConfig?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toFontVariationConfig(value: String?): FontVariationConfig? {
        return value?.let {
            try {
                json.decodeFromString(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun fromJournalTheme(value: JournalTheme?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toJournalTheme(value: String?): JournalTheme? {
        return value?.let {
            try {
                json.decodeFromString(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun fromAudiosList(value: List<JournalAudio>): String {
        if (value.isEmpty()) return "[]"
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toAudiosList(value: String): List<JournalAudio> {
        if (value.isEmpty() || value == "[]") return emptyList()
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}