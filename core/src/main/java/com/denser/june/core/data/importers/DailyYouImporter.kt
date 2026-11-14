package com.denser.june.core.data.importers

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.denser.june.core.domain.importers.ImportSource
import com.denser.june.core.domain.importers.ImportSummary
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.media.storage.AudioStorageManager
import com.denser.june.core.media.storage.MediaStorageManager
import com.denser.june.core.utils.MoodEmojiMapper
import com.denser.june.core.utils.TagUtils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

class DailyYouImporter(
    private val journalRepo: JournalRepository? = null,
    private val context: Context? = null
) {

    companion object {
        const val SUPPORTED_VERSION = 2
        private val AUDIO_EXTENSIONS = setOf("m4a", "mp3", "wav", "aac", "ogg", "3gp")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun import(
        uri: Uri,
        onProgress: ((Float, String) -> Unit)? = null
    ): Result<ImportSummary> {
        if (context == null || journalRepo == null) {
            return Result.failure(IllegalStateException("Context and JournalRepository required for import"))
        }
        val tempDbFile = File(context.cacheDir, "daily_you_${System.currentTimeMillis()}.db")
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalArgumentException("Cannot read input file"))

            val isZip = bytes.size >= 4 &&
                    bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
                    bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

            var jsonContent: String? = null
            var hasDb = false
            val extractedMediaMap = mutableMapOf<String, String>()
            val extractedAudioMap = mutableMapOf<String, JournalAudio>()

            if (isZip) {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory) {
                            val lowerName = name.lowercase()
                            if (lowerName == "daily_you.db" || lowerName.endsWith("/daily_you.db")) {
                                FileOutputStream(tempDbFile).use { out -> zis.copyTo(out) }
                                hasDb = true
                            } else if (lowerName.endsWith(".json") &&
                                (jsonContent == null || lowerName.endsWith("daily_you_export.json"))
                            ) {
                                jsonContent = String(zis.readBytes(), Charsets.UTF_8)
                            } else {
                                val fileName = File(name).name
                                val ext = fileName.substringAfterLast('.', "").lowercase()
                                if (ext in AUDIO_EXTENSIONS || name.startsWith("Audio/")) {
                                    val savedAudio = AudioStorageManager.saveImportedAudioStream(context, zis, "DailyYou", fileName)
                                    if (savedAudio != null) {
                                        extractedAudioMap[name] = savedAudio
                                        extractedAudioMap[fileName] = savedAudio
                                        extractedAudioMap[name.removePrefix("attachments/")] = savedAudio
                                    }
                                } else if (name.startsWith("Images/") || name.startsWith("Video/") ||
                                    name.startsWith("attachments/") || name.startsWith("media/")
                                ) {
                                    val savedPath = MediaStorageManager.saveImportedStream(context, zis, "DailyYou", fileName)
                                    extractedMediaMap[name] = savedPath
                                    extractedMediaMap[fileName] = savedPath
                                    extractedMediaMap[name.removePrefix("attachments/")] = savedPath
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } else {
                jsonContent = String(bytes, Charsets.UTF_8)
            }

            val parseResult: Pair<List<Journal>, String?> = if (hasDb && tempDbFile.exists()) {
                Pair(parseDailyYouDatabase(tempDbFile, extractedMediaMap, extractedAudioMap), null)
            } else if (!jsonContent.isNullOrBlank()) {
                parseDailyYouJson(jsonContent!!, extractedMediaMap, extractedAudioMap)
            } else {
                return Result.failure(IllegalStateException("No Daily You database or JSON content found"))
            }

            val (parsedJournals, versionWarning) = parseResult
            if (parsedJournals.isEmpty()) {
                return Result.failure(IllegalStateException("No entries found in Daily You export"))
            }

            val existingJournals = journalRepo.getAllJournals()
            val existingCache = existingJournals.associateBy { Pair(it.dateTime, it.title) }

            var importedCount = 0
            var updatedCount = 0
            var skippedCount = 0
            val errors = mutableListOf<String>()

            parsedJournals.forEachIndexed { index, journal ->
                try {
                    when (ImporterHelper.saveOrUpdateJournal(journalRepo, journal, existingCache)) {
                        ImportEntryResult.Inserted -> importedCount++
                        ImportEntryResult.Updated -> updatedCount++
                        ImportEntryResult.Skipped -> skippedCount++
                    }
                    onProgress?.invoke(
                        (index + 1).toFloat() / parsedJournals.size,
                        "Imported ${index + 1} of ${parsedJournals.size}"
                    )
                } catch (e: Exception) {
                    errors.add("Failed to save entry ${journal.title}: ${e.message}")
                }
            }

            Result.success(
                ImportSummary(
                    source = ImportSource.DailyYou,
                    totalJournals = parsedJournals.size,
                    importedJournals = importedCount,
                    updatedJournals = updatedCount,
                    mediaExtracted = extractedMediaMap.size,
                    audiosExtracted = extractedAudioMap.size,
                    skippedJournals = skippedCount,
                    versionWarning = versionWarning,
                    errors = errors
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (tempDbFile.exists()) {
                tempDbFile.delete()
            }
        }
    }

    private fun parseDailyYouDatabase(
        dbFile: File,
        mediaMap: Map<String, String>,
        audioMap: Map<String, JournalAudio>
    ): List<Journal> {
        val database = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            val moodMap = mutableMapOf<Int, String>()
            try {
                database.rawQuery("SELECT mood, icon, icon_type FROM mood_scale", null).use { cursor ->
                    val moodIdx = cursor.getColumnIndex("mood")
                    val iconIdx = cursor.getColumnIndex("icon")
                    val typeIdx = cursor.getColumnIndex("icon_type")

                    while (cursor.moveToNext()) {
                        val mood = if (moodIdx != -1) cursor.getInt(moodIdx) else continue
                        val icon = if (iconIdx != -1 && !cursor.isNull(iconIdx)) cursor.getString(iconIdx) else null
                        val iconType = if (typeIdx != -1) cursor.getInt(typeIdx) else 0

                        if (!icon.isNullOrBlank()) {
                            val emoji = if (iconType == 0 || iconType == 1) icon else MoodEmojiMapper.fromText(icon)
                            if (emoji != null) {
                                moodMap[mood] = emoji
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            val tagDefs = mutableMapOf<Long, String>()
            try {
                database.rawQuery("SELECT id, name FROM tags", null).use { cursor ->
                    val idIdx = cursor.getColumnIndex("id")
                    val nameIdx = cursor.getColumnIndex("name")

                    while (cursor.moveToNext()) {
                        val id = if (idIdx != -1) cursor.getLong(idIdx) else continue
                        val name = if (nameIdx != -1 && !cursor.isNull(nameIdx)) cursor.getString(nameIdx) else continue
                        if (name.isNotBlank()) {
                            tagDefs[id] = name.trim()
                        }
                    }
                }
            } catch (_: Exception) {}

            val entryTagsMap = mutableMapOf<Long, MutableList<String>>()
            try {
                database.rawQuery("SELECT entry_id, tag_id FROM entry_tags", null).use { cursor ->
                    val entryIdIdx = cursor.getColumnIndex("entry_id")
                    val tagIdIdx = cursor.getColumnIndex("tag_id")

                    while (cursor.moveToNext()) {
                        val entryId = if (entryIdIdx != -1) cursor.getLong(entryIdIdx) else continue
                        val tagId = if (tagIdIdx != -1) cursor.getLong(tagIdIdx) else continue

                        tagDefs[tagId]?.let { tagName ->
                            entryTagsMap.getOrPut(entryId) { mutableListOf() }.add(tagName)
                        }
                    }
                }
            } catch (_: Exception) {}

            val entryImagesMap = mutableMapOf<Long, MutableList<String>>()
            val entryAudiosMap = mutableMapOf<Long, MutableList<JournalAudio>>()
            try {
                database.rawQuery("SELECT entry_id, type, file_path FROM entry_attachments ORDER BY rank ASC", null).use { cursor ->
                    val entryIdIdx = cursor.getColumnIndex("entry_id")
                    val typeIdx = cursor.getColumnIndex("type")
                    val pathIdx = cursor.getColumnIndex("file_path")

                    while (cursor.moveToNext()) {
                        val entryId = if (entryIdIdx != -1) cursor.getLong(entryIdIdx) else continue
                        val type = if (typeIdx != -1 && !cursor.isNull(typeIdx)) cursor.getString(typeIdx) else ""
                        val filePath = if (pathIdx != -1 && !cursor.isNull(pathIdx)) cursor.getString(pathIdx) else continue
                        val fileName = File(filePath).name

                        if (type == "audio") {
                            val audio = audioMap[filePath] ?: audioMap[fileName] ?: audioMap["Audio/$fileName"]
                            if (audio != null) {
                                entryAudiosMap.getOrPut(entryId) { mutableListOf() }.add(audio)
                            }
                        } else {
                            val localPath = mediaMap[filePath] ?: mediaMap[fileName] ?: mediaMap["Images/$fileName"] ?: mediaMap["Video/$fileName"]
                            if (localPath != null && File(localPath).exists()) {
                                entryImagesMap.getOrPut(entryId) { mutableListOf() }.add(localPath)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            val journals = mutableListOf<Journal>()
            database.rawQuery("SELECT id, text, mood, time_create, time_modified FROM entries ORDER BY id ASC", null).use { cursor ->
                val idIdx = cursor.getColumnIndex("id")
                val textIdx = cursor.getColumnIndex("text")
                val moodIdx = cursor.getColumnIndex("mood")
                val createIdx = cursor.getColumnIndex("time_create")
                val modifiedIdx = cursor.getColumnIndex("time_modified")

                while (cursor.moveToNext()) {
                    val id = if (idIdx != -1) cursor.getLong(idIdx) else continue
                    val text = if (textIdx != -1 && !cursor.isNull(textIdx)) cursor.getString(textIdx) else ""
                    val mood = if (moodIdx != -1 && !cursor.isNull(moodIdx)) cursor.getInt(moodIdx) else null
                    val createStr = if (createIdx != -1 && !cursor.isNull(createIdx)) cursor.getString(createIdx) else null
                    val modifiedStr = if (modifiedIdx != -1 && !cursor.isNull(modifiedIdx)) cursor.getString(modifiedIdx) else null

                    val epochMs = createStr?.let { parseIsoTime(it) } ?: System.currentTimeMillis()
                    val updatedMs = modifiedStr?.let { parseIsoTime(it) }

                    val (title, content) = splitTitleAndContent(text, epochMs)
                    val emoji = mood?.let { moodMap[it] }

                    val images = entryImagesMap[id] ?: emptyList()
                    val audios = entryAudiosMap[id] ?: emptyList()
                    val tags = entryTagsMap[id] ?: emptyList()

                    val deterministicId = ImporterHelper.generateDeterministicId(ImportSource.DailyYou, "$id")

                    journals.add(
                        Journal(
                            id = deterministicId,
                            title = title,
                            content = content,
                            emoji = emoji,
                            images = images,
                            audios = audios,
                            tags = TagUtils.normalizeTags(tags),
                            createdAt = epochMs,
                            updatedAt = updatedMs,
                            dateTime = epochMs,
                            isDraft = false
                        )
                    )
                }
            }

            return journals
        } finally {
            database.close()
        }
    }

    internal fun parseDailyYouJson(
        jsonString: String,
        mediaMap: Map<String, String> = emptyMap(),
        audioMap: Map<String, JournalAudio> = emptyMap()
    ): Pair<List<Journal>, String?> {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val metadata = root["metadata"]?.let { if (it is JsonObject) it else null }
        val version = root["version"]?.jsonPrimitive?.intOrNull
            ?: metadata?.get("version")?.jsonPrimitive?.intOrNull
            ?: 1
        val versionWarning = if (version > SUPPORTED_VERSION) {
            "Daily You schema version $version detected (expected <= $SUPPORTED_VERSION). Best-effort import applied."
        } else null

        val journals = if (version >= 2) {
            parseV2Format(root, mediaMap, audioMap)
        } else {
            parseV1Format(root, mediaMap, audioMap)
        }

        return Pair(journals, versionWarning)
    }

    private fun parseV1Format(
        root: JsonObject,
        mediaMap: Map<String, String>,
        audioMap: Map<String, JournalAudio>
    ): List<Journal> {
        val entriesArray = root["entries"]?.let { if (it is JsonArray) it else null }
            ?: root["daily-you"]?.let { if (it is JsonObject) it["entries"]?.jsonArray else null }
            ?: root["daily_you"]?.let { if (it is JsonObject) it["entries"]?.jsonArray else null }
            ?: return emptyList()

        val journals = mutableListOf<Journal>()
        for (item in entriesArray) {
            if (item is JsonObject) {
                parseV1Entry(item, mediaMap, audioMap)?.let { journals.add(it) }
            }
        }
        return journals
    }

    private fun parseV1Entry(
        entry: JsonObject,
        mediaMap: Map<String, String>,
        audioMap: Map<String, JournalAudio>
    ): Journal? {
        val rawText = entry["text"]?.jsonPrimitive?.content
            ?: entry["notes"]?.jsonPrimitive?.content
            ?: entry["content"]?.jsonPrimitive?.content
            ?: ""
        val explicitTitle = entry["title"]?.jsonPrimitive?.content
        val createdAtStr = entry["timeCreate"]?.jsonPrimitive?.content
            ?: entry["createdAt"]?.jsonPrimitive?.content
            ?: entry["date"]?.jsonPrimitive?.content
            ?: return null
        val updatedAtStr = entry["timeModified"]?.jsonPrimitive?.content
            ?: entry["updatedAt"]?.jsonPrimitive?.content
        val externalId = entry["id"]?.jsonPrimitive?.content

        val epochMs = parseIsoTime(createdAtStr)
        val updatedMs = updatedAtStr?.let { parseIsoTime(it) }

        val (title, content) = if (!explicitTitle.isNullOrBlank()) {
            Pair(explicitTitle.trim(), rawText)
        } else {
            splitTitleAndContent(rawText, epochMs)
        }

        val rating = entry["rating"]?.jsonPrimitive?.intOrNull ?: entry["mood"]?.jsonPrimitive?.intOrNull
        val emoji = rating?.let { MoodEmojiMapper.fromRating(it) }
            ?: MoodEmojiMapper.fromText(entry["mood"]?.jsonPrimitive?.content)

        val tagsList = mutableListOf<String>()
        entry["tags"]?.let { if (it is JsonArray) it else null }?.forEach { tagEl ->
            if (tagEl is JsonObject) {
                val name = tagEl["name"]?.jsonPrimitive?.content ?: tagEl["id"]?.jsonPrimitive?.content
                if (!name.isNullOrBlank()) tagsList.add(name)
            } else {
                tagsList.add(tagEl.jsonPrimitive.content)
            }
        }
        val normalizedTags = TagUtils.normalizeTags(tagsList)

        val images = mutableListOf<String>()
        val audios = mutableListOf<JournalAudio>()
        entry["attachments"]?.let { if (it is JsonArray) it else null }?.forEach { attEl ->
            if (attEl is JsonObject) {
                val filePath = attEl["path"]?.jsonPrimitive?.content
                    ?: attEl["filePath"]?.jsonPrimitive?.content
                    ?: attEl["file_name"]?.jsonPrimitive?.content
                val type = attEl["type"]?.jsonPrimitive?.content
                if (!filePath.isNullOrBlank()) {
                    val fileName = File(filePath).name
                    if (type == "audio" || fileName.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS) {
                        val localAudio = audioMap[filePath] ?: audioMap[fileName]
                        if (localAudio != null) audios.add(localAudio)
                    } else {
                        val localPath = mediaMap[filePath] ?: mediaMap[fileName]
                        if (localPath != null) images.add(localPath)
                    }
                }
            } else {
                val attPath = attEl.jsonPrimitive.content
                val fileName = File(attPath).name
                val ext = fileName.substringAfterLast('.', "").lowercase()
                if (ext in AUDIO_EXTENSIONS) {
                    val localAudio = audioMap[attPath] ?: audioMap[fileName]
                    if (localAudio != null) audios.add(localAudio)
                } else {
                    val localPath = mediaMap[attPath] ?: mediaMap[fileName]
                    if (localPath != null) images.add(localPath)
                }
            }
        }

        val deterministicId = if (!externalId.isNullOrBlank()) {
            ImporterHelper.generateDeterministicId(ImportSource.DailyYou, externalId)
        } else {
            ImporterHelper.generateDeterministicId(ImportSource.DailyYou, "$epochMs")
        }

        return Journal(
            id = deterministicId,
            title = title,
            content = content,
            emoji = emoji,
            images = images,
            audios = audios,
            tags = normalizedTags,
            createdAt = epochMs,
            updatedAt = updatedMs,
            dateTime = epochMs,
            isDraft = false
        )
    }

    private fun parseV2Format(
        root: JsonObject,
        mediaMap: Map<String, String>,
        audioMap: Map<String, JournalAudio>
    ): List<Journal> {
        val moodScaleMap = mutableMapOf<Int, String>()
        val moodScaleArray = (root["moodScale"] ?: root["mood_scale"])?.let { if (it is JsonArray) it else null }
        moodScaleArray?.forEach { scaleEl ->
            if (scaleEl is JsonObject) {
                val score = scaleEl["mood"]?.jsonPrimitive?.intOrNull
                    ?: scaleEl["score"]?.jsonPrimitive?.intOrNull
                val icon = scaleEl["icon"]?.jsonPrimitive?.content
                val iconType = scaleEl["iconType"]?.jsonPrimitive?.content
                    ?: scaleEl["icon_type"]?.jsonPrimitive?.content
                val label = scaleEl["label"]?.jsonPrimitive?.content

                if (score != null) {
                    val emoji = if (iconType == "character" && !icon.isNullOrBlank()) {
                        icon
                    } else {
                        MoodEmojiMapper.fromText(icon) ?: MoodEmojiMapper.fromText(label) ?: MoodEmojiMapper.fromRating(score)
                    }
                    if (emoji != null) {
                        moodScaleMap[score] = emoji
                    }
                }
            }
        }

        if (moodScaleMap.isEmpty()) {
            moodScaleMap[1] = "😭"
            moodScaleMap[2] = "🙁"
            moodScaleMap[3] = "😐"
            moodScaleMap[4] = "🙂"
            moodScaleMap[5] = "😄"
        }

        val categoriesMap = mutableMapOf<String, String>()
        root["categories"]?.let { if (it is JsonArray) it else null }?.forEach { catEl ->
            if (catEl is JsonObject) {
                val id = catEl["id"]?.jsonPrimitive?.content
                val name = catEl["name"]?.jsonPrimitive?.content
                if (!name.isNullOrBlank()) {
                    if (id != null) categoriesMap[id] = name.trim()
                    categoriesMap[name.trim()] = name.trim()
                }
            }
        }

        val tagsMap = mutableMapOf<String, String>()
        root["tags"]?.let { if (it is JsonArray) it else null }?.forEach { tagEl ->
            if (tagEl is JsonObject) {
                val id = tagEl["id"]?.jsonPrimitive?.content
                val name = tagEl["name"]?.jsonPrimitive?.content
                if (!name.isNullOrBlank()) {
                    if (id != null) tagsMap[id] = name.trim()
                    tagsMap[name.trim()] = name.trim()
                }
            }
        }

        val entriesArray = root["entries"]?.let { if (it is JsonArray) it else null }
            ?: root["daily-you"]?.let { if (it is JsonObject) it["entries"]?.jsonArray else null }
            ?: root["daily_you"]?.let { if (it is JsonObject) it["entries"]?.jsonArray else null }
            ?: return emptyList()

        val journals = mutableListOf<Journal>()
        for (item in entriesArray) {
            if (item is JsonObject) {
                parseV2Entry(item, moodScaleMap, categoriesMap, tagsMap, mediaMap, audioMap)?.let { journals.add(it) }
            }
        }
        return journals
    }

    private fun parseV2Entry(
        entry: JsonObject,
        moodScaleMap: Map<Int, String>,
        categoriesMap: Map<String, String>,
        tagsMap: Map<String, String>,
        mediaMap: Map<String, String>,
        audioMap: Map<String, JournalAudio>
    ): Journal? {
        val rawText = entry["text"]?.jsonPrimitive?.content
            ?: entry["notes"]?.jsonPrimitive?.content
            ?: entry["content"]?.jsonPrimitive?.content
            ?: ""
        val explicitTitle = entry["title"]?.jsonPrimitive?.content
        val createdAtStr = entry["timeCreate"]?.jsonPrimitive?.content
            ?: entry["createdAt"]?.jsonPrimitive?.content
            ?: entry["date"]?.jsonPrimitive?.content
            ?: return null
        val modifiedAtStr = entry["timeModified"]?.jsonPrimitive?.content
            ?: entry["updatedAt"]?.jsonPrimitive?.content
        val externalId = entry["id"]?.jsonPrimitive?.content

        val epochMs = parseIsoTime(createdAtStr)
        val updatedMs = modifiedAtStr?.let { parseIsoTime(it) }

        val (title, content) = if (!explicitTitle.isNullOrBlank()) {
            Pair(explicitTitle.trim(), rawText)
        } else {
            splitTitleAndContent(rawText, epochMs)
        }

        val moodScore = entry["mood"]?.jsonPrimitive?.intOrNull ?: entry["rating"]?.jsonPrimitive?.intOrNull
        val emoji = moodScore?.let { moodScaleMap[it] ?: MoodEmojiMapper.fromRating(it) }

        val categoryId = entry["category_id"]?.jsonPrimitive?.content
            ?: entry["categoryId"]?.jsonPrimitive?.content
            ?: entry["category"]?.jsonPrimitive?.content
        val categoryName = categoryId?.let { categoriesMap[it] ?: it }

        val tagsList = mutableListOf<String>()
        val tagElements = entry["tags"]?.let { if (it is JsonArray) it else null }
            ?: entry["tagIds"]?.let { if (it is JsonArray) it else null }
        tagElements?.forEach { tagEl ->
            if (tagEl is JsonObject) {
                val name = tagEl["name"]?.jsonPrimitive?.content ?: tagEl["id"]?.jsonPrimitive?.content
                if (!name.isNullOrBlank()) tagsList.add(name)
            } else {
                val raw = tagEl.jsonPrimitive.content
                val resolved = tagsMap[raw] ?: raw
                if (resolved.isNotBlank()) tagsList.add(resolved)
            }
        }
        val normalizedTags = TagUtils.normalizeTags(tagsList, categorySpace = categoryName)

        val images = mutableListOf<String>()
        val audios = mutableListOf<JournalAudio>()
        entry["attachments"]?.let { if (it is JsonArray) it else null }?.forEach { attEl ->
            if (attEl is JsonObject) {
                val filePath = attEl["path"]?.jsonPrimitive?.content
                    ?: attEl["filePath"]?.jsonPrimitive?.content
                    ?: attEl["file_name"]?.jsonPrimitive?.content
                val type = attEl["type"]?.jsonPrimitive?.content
                if (!filePath.isNullOrBlank()) {
                    val fileName = File(filePath).name
                    if (type == "audio" || fileName.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS) {
                        val localAudio = audioMap[filePath] ?: audioMap[fileName]
                        if (localAudio != null) audios.add(localAudio)
                    } else {
                        val localPath = mediaMap[filePath] ?: mediaMap[fileName]
                        if (localPath != null) images.add(localPath)
                    }
                }
            } else {
                val filePath = attEl.jsonPrimitive.content
                val fileName = File(filePath).name
                val localPath = mediaMap[filePath] ?: mediaMap[fileName]
                if (localPath != null) images.add(localPath)
            }
        }

        val deterministicId = if (!externalId.isNullOrBlank()) {
            ImporterHelper.generateDeterministicId(ImportSource.DailyYou, externalId)
        } else {
            ImporterHelper.generateDeterministicId(ImportSource.DailyYou, "$epochMs")
        }

        return Journal(
            id = deterministicId,
            title = title,
            content = content,
            emoji = emoji,
            images = images,
            audios = audios,
            tags = normalizedTags,
            createdAt = epochMs,
            updatedAt = updatedMs,
            dateTime = epochMs,
            isDraft = false
        )
    }

    private fun parseIsoTime(isoString: String): Long {
        return try {
            Instant.parse(isoString).toEpochMilli()
        } catch (_: Exception) {
            try {
                val ldt = LocalDateTime.parse(isoString)
                ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {
                try {
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(isoString, Instant::from).toEpochMilli()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
            }
        }
    }

    private fun splitTitleAndContent(text: String, fallbackTimeMs: Long): Pair<String, String> {
        val lines = text.lines()
        val firstNonEmptyLine = lines.firstOrNull { it.isNotBlank() }?.trim() ?: ""

        return if (firstNonEmptyLine.isNotBlank() && firstNonEmptyLine.length <= 80 && !firstNonEmptyLine.startsWith("![")) {
            val title = firstNonEmptyLine.removePrefix("#").trim()
            val remainingContent = lines.drop(lines.indexOfFirst { it.trim() == firstNonEmptyLine } + 1)
                .joinToString("\n")
                .trim()
            Pair(title, remainingContent)
        } else {
            val dateStr = DateTimeFormatter.ofPattern("MMMM d, yyyy")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(fallbackTimeMs))
            Pair(dateStr, text)
        }
    }
}
