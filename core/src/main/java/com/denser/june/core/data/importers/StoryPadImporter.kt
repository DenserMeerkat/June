package com.denser.june.core.data.importers

import android.content.Context
import android.net.Uri
import com.denser.june.core.media.storage.AudioStorageManager
import com.denser.june.core.media.storage.MediaStorageManager
import com.denser.june.core.domain.importers.ImportSource
import com.denser.june.core.domain.importers.ImportSummary
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.domain.model.JournalLocation
import com.denser.june.core.domain.model.JournalTheme
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.utils.MoodEmojiMapper
import com.denser.june.core.utils.TagUtils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

class StoryPadImporter(
    private val journalRepo: JournalRepository? = null,
    private val context: Context? = null
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private data class StoryPadTag(
        val id: String,
        val title: String,
        val emoji: String?,
        val categoryId: Int?
    )

    suspend fun import(
        uri: Uri,
        onProgress: ((Float, String) -> Unit)? = null
    ): Result<ImportSummary> {
        if (context == null || journalRepo == null) {
            return Result.failure(IllegalStateException("Context and JournalRepository required for import"))
        }
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalArgumentException("Cannot read input file"))

            val isZip = bytes.size >= 4 &&
                    bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
                    bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

            var jsonContent: String? = null
            val extractedMediaMap = mutableMapOf<String, String>()
            val extractedAudioMap = mutableMapOf<String, JournalAudio>()

            if (isZip) {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory) {
                            if (name.endsWith(".json", ignoreCase = true) || name.endsWith(".sp", ignoreCase = true)) {
                                val candidate = String(zis.readBytes(), Charsets.UTF_8)
                                if (candidate.contains("stories") || candidate.contains("meta_data")) {
                                    jsonContent = candidate
                                }
                            } else if (name.startsWith("images/") || name.contains("/images/") ||
                                name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true) ||
                                name.endsWith(".png", ignoreCase = true) || name.endsWith(".webp", ignoreCase = true)
                            ) {
                                val fileName = File(name).name
                                val savedPath = MediaStorageManager.saveImportedStream(context, zis, "StoryPad", fileName)
                                extractedMediaMap[name] = savedPath
                                extractedMediaMap[fileName] = savedPath
                                extractedMediaMap[fileName.substringBeforeLast('.')] = savedPath
                            } else if (name.startsWith("audio/") || name.contains("/audio/") ||
                                name.endsWith(".m4a", ignoreCase = true) || name.endsWith(".mp3", ignoreCase = true) ||
                                name.endsWith(".aac", ignoreCase = true) || name.endsWith(".wav", ignoreCase = true)
                            ) {
                                val fileName = File(name).name
                                val savedAudio = AudioStorageManager.saveImportedAudioStream(context, zis, "StoryPad", fileName)
                                if (savedAudio != null) {
                                    extractedAudioMap[name] = savedAudio
                                    extractedAudioMap[fileName] = savedAudio
                                    extractedAudioMap[fileName.substringBeforeLast('.')] = savedAudio
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

            if (jsonContent.isNullOrBlank()) {
                return Result.failure(IllegalStateException("No StoryPad backup JSON content found"))
            }

            val journals = parseStoryPadJson(jsonContent!!, extractedMediaMap, extractedAudioMap)
            if (journals.isEmpty()) {
                return Result.failure(IllegalStateException("No entries found in StoryPad backup"))
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
                    source = ImportSource.StoryPad,
                    totalJournals = journals.size,
                    importedJournals = importedCount,
                    updatedJournals = updatedCount,
                    mediaExtracted = extractedMediaMap.values.distinct().size,
                    audiosExtracted = extractedAudioMap.values.distinct().size,
                    skippedJournals = skippedCount,
                    errors = errors
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun parseStoryPadJson(
        jsonString: String,
        extractedMediaMap: Map<String, String> = emptyMap(),
        extractedAudioMap: Map<String, JournalAudio> = emptyMap()
    ): List<Journal> {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val tables = root["data"]?.jsonObject ?: root["tables"]?.jsonObject ?: root

        val tagMap = mutableMapOf<String, StoryPadTag>()
        tables["tags"]?.let { if (it is JsonArray) it else null }?.forEach { tagEl ->
            if (tagEl is JsonObject) {
                val tagId = tagEl["id"]?.jsonPrimitive?.content
                val tagTitle = tagEl["title"]?.jsonPrimitive?.content ?: ""
                val tagEmoji = tagEl["emoji"]?.jsonPrimitive?.content
                val categoryId = tagEl["category_id"]?.jsonPrimitive?.intOrNull
                    ?: tagEl["categoryId"]?.jsonPrimitive?.intOrNull
                if (!tagId.isNullOrBlank()) {
                    tagMap[tagId] = StoryPadTag(tagId, tagTitle, tagEmoji, categoryId)
                }
            }
        }

        val storiesArray = tables["stories"]?.let { if (it is JsonArray) it else null } ?: return emptyList()

        val journals = mutableListOf<Journal>()
        for (item in storiesArray) {
            if (item is JsonObject) {
                parseStoryEntry(item, tagMap, extractedMediaMap, extractedAudioMap)?.let { journals.add(it) }
            }
        }
        return journals
    }

    private fun parseStoryEntry(
        story: JsonObject,
        tagMap: Map<String, StoryPadTag>,
        extractedMediaMap: Map<String, String>,
        extractedAudioMap: Map<String, JournalAudio>
    ): Journal? {
        val year = story["year"]?.jsonPrimitive?.intOrNull
        val month = story["month"]?.jsonPrimitive?.intOrNull
        val day = story["day"]?.jsonPrimitive?.intOrNull
        val hour = story["hour"]?.jsonPrimitive?.intOrNull ?: 12
        val minute = story["minute"]?.jsonPrimitive?.intOrNull ?: 0
        val second = story["second"]?.jsonPrimitive?.intOrNull ?: 0
        val createdAtStr = story["created_at"]?.jsonPrimitive?.content
            ?: story["createdAt"]?.jsonPrimitive?.content
        val updatedAtStr = story["updated_at"]?.jsonPrimitive?.content
            ?: story["updatedAt"]?.jsonPrimitive?.content
        val externalId = story["id"]?.jsonPrimitive?.content
            ?: story["storyId"]?.jsonPrimitive?.content
            ?: story["story_id"]?.jsonPrimitive?.content

        val epochMs = if (year != null && month != null && day != null) {
            try {
                val ldt = LocalDateTime.of(year, month, day, hour, minute, second)
                ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {
                createdAtStr?.let { parseIsoTime(it) } ?: System.currentTimeMillis()
            }
        } else {
            createdAtStr?.let { parseIsoTime(it) } ?: System.currentTimeMillis()
        }

        val updatedMs = updatedAtStr?.let { parseIsoTime(it) }

        val starred = story["starred"]?.jsonPrimitive?.booleanOrNull ?: false
        val pinned = story["pinned"]?.jsonPrimitive?.booleanOrNull ?: false
        val isBookmarked = starred || pinned

        val tagsList = mutableListOf<String>()
        var resolvedEmoji = MoodEmojiMapper.fromText(story["feeling"]?.jsonPrimitive?.content)

        val rawTagsArray = story["tags"]?.let { if (it is JsonArray) it else null }
        rawTagsArray?.forEach { tagEl ->
            val rawId = tagEl.jsonPrimitive.content
            val tagObj = tagMap[rawId]
            if (tagObj != null) {
                if (resolvedEmoji == null && !tagObj.emoji.isNullOrBlank()) {
                    resolvedEmoji = tagObj.emoji
                }
                if (tagObj.title.equals("Emoji", ignoreCase = true)) {
                    if (!tagObj.emoji.isNullOrBlank()) {
                        tagsList.add(tagObj.emoji)
                    }
                } else if (tagObj.title.isNotBlank()) {
                    tagsList.add(tagObj.title)
                }
            } else if (rawId.isNotBlank() && !rawId.all { it.isDigit() }) {
                tagsList.add(rawId)
            }
        }
        val normalizedTags = TagUtils.normalizeTags(tagsList)

        val latestContent = (story["latest_content"] ?: story["latestContent"])?.let { if (it is JsonObject) it else null }
        val explicitTitle = latestContent?.get("title")?.jsonPrimitive?.content
        val plainText = latestContent?.get("plain_text")?.jsonPrimitive?.content
            ?: latestContent?.get("plainText")?.jsonPrimitive?.content

        val journalImages = mutableListOf<String>()
        val journalAudios = mutableListOf<JournalAudio>()
        val pagesList = mutableListOf<String>()

        val richPages = (latestContent?.get("rich_pages") ?: latestContent?.get("richPages"))?.let { if (it is JsonArray) it else null }
        richPages?.forEach { pageEl ->
            if (pageEl is JsonObject) {
                val pageTitle = pageEl["title"]?.jsonPrimitive?.content
                val md = pageEl["markdown"]?.jsonPrimitive?.content
                if (!md.isNullOrBlank()) {
                    val pageText = if (!pageTitle.isNullOrBlank() && !md.trimStart().startsWith("#")) {
                        "# $pageTitle\n\n$md"
                    } else {
                        md
                    }
                    pagesList.add(pageText.trim())
                } else {
                    val bodyArray = pageEl["body"]?.let { if (it is JsonArray) it else null }
                    if (bodyArray != null) {
                        val sb = StringBuilder()
                        for (op in bodyArray) {
                            if (op is JsonObject) {
                                val ins = op["insert"]
                                if (ins != null) {
                                    if (ins is JsonObject) {
                                        val mediaPath = ins["media"]?.jsonPrimitive?.content ?: ins["image"]?.jsonPrimitive?.content
                                        val audioPath = ins["audio"]?.jsonPrimitive?.content
                                        if (!mediaPath.isNullOrBlank()) {
                                            val resolved = extractedMediaMap[mediaPath]
                                                ?: extractedMediaMap[File(mediaPath).name]
                                                ?: mediaPath
                                            sb.append("\n![]($resolved)\n")
                                            if (extractedMediaMap.containsKey(mediaPath) || extractedMediaMap.containsKey(File(mediaPath).name)) {
                                                if (!journalImages.contains(resolved)) journalImages.add(resolved)
                                            }
                                        } else if (!audioPath.isNullOrBlank()) {
                                            val audioObj = extractedAudioMap[audioPath] ?: extractedAudioMap[File(audioPath).name]
                                            val resolved = audioObj?.path ?: audioPath
                                            sb.append("\n[Audio]($resolved)\n")
                                            if (audioObj != null && !journalAudios.any { it.path == audioObj.path }) {
                                                journalAudios.add(audioObj)
                                            }
                                        }
                                    } else {
                                        sb.append(ins.jsonPrimitive.content)
                                    }
                                }
                            }
                        }
                        val bodyText = sb.toString().trim()
                        if (bodyText.isNotEmpty()) {
                            val pageText = if (!pageTitle.isNullOrBlank() && !bodyText.startsWith("#")) {
                                "# $pageTitle\n\n$bodyText"
                            } else {
                                bodyText
                            }
                            pagesList.add(pageText)
                        }
                    }
                }
            }
        }

        if (pagesList.isEmpty()) {
            val legacyPages = latestContent?.get("pages")?.let { if (it is JsonArray) it else null }
            legacyPages?.forEach { pageEl ->
                if (pageEl is JsonArray) {
                    val sb = StringBuilder()
                    for (op in pageEl) {
                        if (op is JsonObject) {
                            val ins = op["insert"]
                            if (ins != null) {
                                if (ins is JsonObject) {
                                    val mediaPath = ins["media"]?.jsonPrimitive?.content ?: ins["image"]?.jsonPrimitive?.content
                                    val audioPath = ins["audio"]?.jsonPrimitive?.content
                                    if (!mediaPath.isNullOrBlank()) {
                                        val resolved = extractedMediaMap[mediaPath]
                                            ?: extractedMediaMap[File(mediaPath).name]
                                            ?: mediaPath
                                        sb.append("\n![]($resolved)\n")
                                        if (extractedMediaMap.containsKey(mediaPath) || extractedMediaMap.containsKey(File(mediaPath).name)) {
                                            if (!journalImages.contains(resolved)) journalImages.add(resolved)
                                        }
                                    } else if (!audioPath.isNullOrBlank()) {
                                        val audioObj = extractedAudioMap[audioPath] ?: extractedAudioMap[File(audioPath).name]
                                        val resolved = audioObj?.path ?: audioPath
                                        sb.append("\n[Audio]($resolved)\n")
                                        if (audioObj != null && !journalAudios.any { it.path == audioObj.path }) {
                                            journalAudios.add(audioObj)
                                        }
                                    }
                                } else {
                                    sb.append(ins.jsonPrimitive.content)
                                }
                            }
                        }
                    }
                    val bodyText = sb.toString().trim()
                    if (bodyText.isNotEmpty()) {
                        pagesList.add(bodyText)
                    }
                }
            }
        }

        val combinedContent = if (pagesList.isNotEmpty()) {
            pagesList.joinToString("\n\n")
        } else {
            plainText ?: ""
        }

        val (title, content) = if (!explicitTitle.isNullOrBlank()) {
            Pair(explicitTitle.trim(), combinedContent)
        } else {
            splitTitleAndContent(combinedContent, epochMs)
        }

        val preferences = story["preferences"]?.let { if (it is JsonObject) it else null }
        val colorSeedValue = preferences?.get("color_seed_value")?.jsonPrimitive?.intOrNull
            ?: preferences?.get("colorSeedValue")?.jsonPrimitive?.intOrNull
        val theme = colorSeedValue?.let { JournalTheme(primaryColor = it, style = "TonalSpot") }

        val fontFamily = preferences?.get("font_family")?.jsonPrimitive?.content
            ?: preferences?.get("fontFamily")?.jsonPrimitive?.content

        val place = story["place"]?.let { if (it is JsonObject) it else null }
        val location = place?.let {
            val placeName = it["place_name"]?.jsonPrimitive?.content ?: it["name"]?.jsonPrimitive?.content
            val locality = it["locality"]?.jsonPrimitive?.content ?: placeName
            val address = it["address"]?.jsonPrimitive?.content
            val lat = it["latitude"]?.jsonPrimitive?.doubleOrNull
                ?: it["latLng"]?.let { ll -> if (ll is JsonObject) ll["latitude"]?.jsonPrimitive?.doubleOrNull else null }
            val lon = it["longitude"]?.jsonPrimitive?.doubleOrNull
                ?: it["latLng"]?.let { ll -> if (ll is JsonObject) ll["longitude"]?.jsonPrimitive?.doubleOrNull else null }
            if (lat != null && lon != null) {
                JournalLocation(
                    latitude = lat,
                    longitude = lon,
                    name = placeName,
                    locality = locality,
                    address = address
                )
            } else null
        }

        val storyAssets = story["assets"]?.let { if (it is JsonArray) it else null }
        storyAssets?.forEach { assetEl ->
            val assetId = assetEl.jsonPrimitive.content
            val localMedia = extractedMediaMap[assetId]
            if (localMedia != null && !journalImages.contains(localMedia)) {
                journalImages.add(localMedia)
            }
            val localAudio = extractedAudioMap[assetId]
            if (localAudio != null && !journalAudios.any { it.path == localAudio.path }) {
                journalAudios.add(localAudio)
            }
        }

        val deterministicId = if (!externalId.isNullOrBlank()) {
            ImporterHelper.generateDeterministicId(ImportSource.StoryPad, externalId)
        } else {
            ImporterHelper.generateDeterministicId(ImportSource.StoryPad, "$epochMs")
        }

        return Journal(
            id = deterministicId,
            title = title,
            content = content,
            emoji = resolvedEmoji,
            images = journalImages,
            audios = journalAudios,
            location = location,
            tags = normalizedTags,
            createdAt = epochMs,
            updatedAt = updatedMs,
            dateTime = epochMs,
            isBookmarked = isBookmarked,
            isArchived = false,
            isDraft = false,
            isLocked = false,
            theme = theme,
            font = fontFamily
        )
    }

    private fun splitTitleAndContent(rawText: String, fallbackTime: Long): Pair<String, String> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) {
            return Pair("Entry on ${DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(fallbackTime))}", "")
        }

        val lines = trimmed.lines()
        val firstLine = lines.first().trim()

        return if (firstLine.startsWith("#")) {
            val title = firstLine.removePrefix("#").trim()
            val remaining = lines.drop(1).joinToString("\n").trim()
            Pair(if (title.isBlank()) "Untitled" else title, remaining)
        } else if (lines.size > 1 && firstLine.length <= 80) {
            val remaining = lines.drop(1).joinToString("\n").trim()
            Pair(firstLine, remaining)
        } else {
            val title = if (firstLine.length > 50) firstLine.take(50) + "..." else firstLine
            Pair(title, trimmed)
        }
    }

    private fun parseIsoTime(isoString: String): Long {
        return try {
            if (isoString.contains("Z") || (isoString.contains("+") && isoString.lastIndexOf('+') > 10)) {
                Instant.parse(isoString).toEpochMilli()
            } else {
                LocalDateTime.parse(isoString).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        } catch (_: Exception) {
            try {
                Instant.parse(isoString).toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
