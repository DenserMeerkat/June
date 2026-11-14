package com.denser.june.core.data.importers

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.net.Uri
import com.denser.june.core.domain.importers.ImportSource
import com.denser.june.core.domain.importers.ImportSummary
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.domain.model.JournalLocation
import com.denser.june.core.domain.model.JournalTheme
import com.denser.june.core.domain.markdown.HtmlToMarkdown
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.media.storage.AudioStorageManager
import com.denser.june.core.media.storage.MediaStorageManager
import com.denser.june.core.utils.MoodEmojiMapper
import com.denser.june.core.utils.TagUtils
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class DiariumImporter(
    private val journalRepo: JournalRepository? = null,
    private val context: Context? = null
) {

    companion object {
        private const val TICKS_AT_UNIX_EPOCH = 621355968000000000L

        fun diariumTicksToWallClockMillis(ticks: Long): Long {
            return try {
                val usSinceEpoch = (ticks - TICKS_AT_UNIX_EPOCH) / 10L
                val instant = Instant.ofEpochMilli(usSinceEpoch / 1000L)
                val wallClock = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
                wallClock.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {
                (ticks - TICKS_AT_UNIX_EPOCH) / 10000L
            }
        }
    }

    suspend fun import(
        uri: Uri,
        onProgress: ((Float, String) -> Unit)? = null
    ): Result<ImportSummary> {
        if (context == null || journalRepo == null) {
            return Result.failure(IllegalStateException("Context and JournalRepository required for import"))
        }
        val tempDbFile = File(context.cacheDir, "diarium_import_${System.currentTimeMillis()}.db")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempDbFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(IllegalArgumentException("Cannot read Diarium file"))

            val database = SQLiteDatabase.openDatabase(
                tempDbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            try {
                val parsed = parseDiariumDatabase(database)
                val journals = parsed.journals
                if (journals.isEmpty()) {
                    return Result.failure(IllegalStateException("No entries found in Diarium database"))
                }

                val existingJournals = journalRepo.getAllJournals()
                val existingCache = existingJournals.associateBy { Pair(it.dateTime, it.title) }

                var importedCount = 0
                var updatedCount = 0
                var skippedCount = 0
                val errors = mutableListOf<String>()

                journals.forEachIndexed { index, journal ->
                    try {
                        when (ImporterHelper.saveOrUpdateJournal(journalRepo, journal, existingCache)) {
                            ImportEntryResult.Inserted -> importedCount++
                            ImportEntryResult.Updated -> updatedCount++
                            ImportEntryResult.Skipped -> skippedCount++
                        }
                        onProgress?.invoke(
                            (index + 1).toFloat() / journals.size,
                            "Imported ${index + 1} of ${journals.size}"
                        )
                    } catch (e: Exception) {
                        errors.add("Failed to save entry ${journal.title}: ${e.message}")
                    }
                }

                Result.success(
                    ImportSummary(
                        source = ImportSource.Diarium,
                        totalJournals = journals.size,
                        importedJournals = importedCount,
                        updatedJournals = updatedCount,
                        mediaExtracted = parsed.mediaCount,
                        audiosExtracted = parsed.audioCount,
                        skippedJournals = skippedCount,
                        errors = errors
                    )
                )
            } finally {
                database.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (tempDbFile.exists()) {
                tempDbFile.delete()
            }
        }
    }

    private data class DiariumTagDef(
        val name: String,
        val type: Int,
        val colorHex: String?
    )

    private data class DiariumParseResult(
        val journals: List<Journal>,
        val mediaCount: Int,
        val audioCount: Int
    )

    private fun parseDiariumDatabase(db: SQLiteDatabase): DiariumParseResult {
        val tagDefs = mutableMapOf<Long, DiariumTagDef>()
        try {
            db.rawQuery("SELECT * FROM Tags", null).use { cursor ->
                val idIdx = cursor.getColumnIndex("DiaryTagId")
                val valIdx = cursor.getColumnIndex("Value")
                val typeIdx = cursor.getColumnIndex("Type")
                val colorIdx = cursor.getColumnIndex("Color")

                while (cursor.moveToNext()) {
                    val id = if (idIdx != -1) cursor.getLong(idIdx) else -1L
                    val value = if (valIdx != -1) cursor.getString(valIdx) ?: "" else ""
                    val type = if (typeIdx != -1) cursor.getInt(typeIdx) else 0
                    val color = if (colorIdx != -1 && !cursor.isNull(colorIdx)) cursor.getString(colorIdx) else null
                    if (id != -1L && value.isNotBlank()) {
                        tagDefs[id] = DiariumTagDef(value, type, color)
                    }
                }
            }
        } catch (_: Exception) {}

        val entryTagsMap = mutableMapOf<Long, MutableList<String>>()
        val entryPrimaryColorMap = mutableMapOf<Long, Int>()
        try {
            db.rawQuery("SELECT * FROM EntryTags", null).use { cursor ->
                val entryIdIdx = cursor.getColumnIndex("DiaryEntryId")
                val tagIdIdx = cursor.getColumnIndex("DiaryTagId")
                val trackingValIdx = cursor.getColumnIndex("TrackingValue")

                while (cursor.moveToNext()) {
                    val entryId = if (entryIdIdx != -1) cursor.getLong(entryIdIdx) else -1L
                    val tagId = if (tagIdIdx != -1) cursor.getLong(tagIdIdx) else -1L
                    val trackingVal = if (trackingValIdx != -1 && !cursor.isNull(trackingValIdx)) cursor.getString(trackingValIdx) else null

                    if (entryId != -1L && tagId != -1L) {
                        val def = tagDefs[tagId]
                        if (def != null) {
                            val tagString = when (def.type) {
                                1 -> "@${def.name}"
                                2 -> if (!trackingVal.isNullOrBlank()) "${def.name}: $trackingVal" else def.name
                                else -> def.name
                            }
                            TagUtils.normalizeToTopic(tagString)?.let { normalized ->
                                entryTagsMap.getOrPut(entryId) { mutableListOf() }.add(normalized)
                            }

                            if (!def.colorHex.isNullOrBlank() && !entryPrimaryColorMap.containsKey(entryId)) {
                                try {
                                    entryPrimaryColorMap[entryId] = Color.parseColor(def.colorHex)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        var totalMediaCount = 0
        var totalAudioCount = 0
        val entryImagesMap = mutableMapOf<Long, MutableList<String>>()
        val entryAudiosMap = mutableMapOf<Long, MutableList<JournalAudio>>()

        try {
            db.rawQuery("SELECT * FROM Media", null).use { cursor ->
                val entryIdIdx = cursor.getColumnIndex("DiaryEntryId")
                val typeIdx = cursor.getColumnIndex("Type")
                val dataIdx = cursor.getColumnIndex("Data")

                while (cursor.moveToNext()) {
                    val entryId = if (entryIdIdx != -1) cursor.getLong(entryIdIdx) else -1L
                    val type = if (typeIdx != -1) cursor.getInt(typeIdx) else -1
                    val blob = if (dataIdx != -1 && !cursor.isNull(dataIdx)) cursor.getBlob(dataIdx) else null

                    if (entryId != -1L && blob != null && blob.isNotEmpty() && context != null) {
                        if (type == 0) {
                            val mediaIndex = entryImagesMap[entryId]?.size ?: 0
                            val localPath = MediaStorageManager.saveImportedBytes(context, blob, "Diarium", "${entryId}_$mediaIndex", "jpg")
                            entryImagesMap.getOrPut(entryId) { mutableListOf() }.add(localPath)
                            totalMediaCount++
                        } else if (type == 1) {
                            val audioIndex = entryAudiosMap[entryId]?.size ?: 0
                            val entryTimestamp = diariumTicksToWallClockMillis(entryId)
                            val audio = AudioStorageManager.saveImportedAudioBytes(
                                context = context,
                                bytes = blob,
                                source = "Diarium",
                                identifier = "${entryId}_$audioIndex",
                                extension = "m4a",
                                fallbackDateTime = entryTimestamp,
                                title = "Voice note"
                            )
                            if (audio != null) {
                                entryAudiosMap.getOrPut(entryId) { mutableListOf() }.add(audio)
                                totalAudioCount++
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val journals = mutableListOf<Journal>()
        db.rawQuery("SELECT * FROM Entries ORDER BY DiaryEntryId ASC", null).use { cursor ->
            val idIdx = cursor.getColumnIndex("DiaryEntryId")
            val headingIdx = cursor.getColumnIndex("Heading")
            val textIdx = cursor.getColumnIndex("Text")
            val ratingIdx = cursor.getColumnIndex("Rating")
            val latIdx = cursor.getColumnIndex("Latitude")
            val lonIdx = cursor.getColumnIndex("Longitude")
            val locNameIdx = cursor.getColumnIndex("LocationName")

            while (cursor.moveToNext()) {
                val diaryEntryId = if (idIdx != -1) cursor.getLong(idIdx) else continue
                val heading = if (headingIdx != -1 && !cursor.isNull(headingIdx)) cursor.getString(headingIdx) else null
                val htmlText = if (textIdx != -1 && !cursor.isNull(textIdx)) cursor.getString(textIdx) else null
                val rating = if (ratingIdx != -1 && !cursor.isNull(ratingIdx)) cursor.getInt(ratingIdx) else null
                val lat = if (latIdx != -1 && !cursor.isNull(latIdx)) cursor.getDouble(latIdx) else null
                val lon = if (lonIdx != -1 && !cursor.isNull(lonIdx)) cursor.getDouble(lonIdx) else null
                val locName = if (locNameIdx != -1 && !cursor.isNull(locNameIdx)) cursor.getString(locNameIdx) else null

                val epochMs = diariumTicksToWallClockMillis(diaryEntryId)
                val markdownContent = HtmlToMarkdown.convert(htmlText)

                val title = if (!heading.isNullOrBlank()) {
                    heading.trim()
                } else {
                    DateTimeFormatter.ofPattern("MMMM d, yyyy")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochMilli(epochMs))
                }

                val emoji = when (rating) {
                    5 -> "😍"
                    4 -> "😊"
                    3 -> "😐"
                    2 -> "🙁"
                    1 -> "😢"
                    else -> null
                }

                val location = if (lat != null && lon != null) {
                    JournalLocation(
                        latitude = lat,
                        longitude = lon,
                        name = locName,
                        address = locName
                    )
                } else null

                val tags = entryTagsMap[diaryEntryId] ?: emptyList()
                val images = entryImagesMap[diaryEntryId] ?: emptyList()
                val audios = entryAudiosMap[diaryEntryId] ?: emptyList()

                val theme = entryPrimaryColorMap[diaryEntryId]?.let {
                    JournalTheme(primaryColor = it, style = "TonalSpot")
                }

                val deterministicId = ImporterHelper.generateDeterministicId(ImportSource.Diarium, "$diaryEntryId")

                journals.add(
                    Journal(
                        id = deterministicId,
                        title = title,
                        content = markdownContent,
                        emoji = emoji,
                        images = images,
                        audios = audios,
                        location = location,
                        tags = TagUtils.normalizeTags(tags),
                        theme = theme,
                        createdAt = epochMs,
                        updatedAt = null,
                        dateTime = epochMs,
                        isDraft = false
                    )
                )
            }
        }

        return DiariumParseResult(journals, totalMediaCount, totalAudioCount)
    }
}
