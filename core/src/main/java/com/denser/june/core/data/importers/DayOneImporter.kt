package com.denser.june.core.data.importers

import android.content.Context
import android.net.Uri
import com.denser.june.core.domain.importers.ImportSource
import com.denser.june.core.domain.importers.ImportSummary
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.domain.model.JournalLocation
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.media.storage.AudioStorageManager
import com.denser.june.core.media.storage.MediaStorageManager
import com.denser.june.core.utils.MoodEmojiMapper
import com.denser.june.core.utils.TagUtils
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

class DayOneImporter(
    private val journalRepo: JournalRepository? = null,
    private val context: Context? = null
) {

    companion object {
        const val SUPPORTED_VERSION = "1.0"
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
                            if (name.equals("Journal.json", ignoreCase = true) || name.endsWith("/Journal.json", ignoreCase = true)) {
                                jsonContent = String(zis.readBytes(), Charsets.UTF_8)
                            } else if (name.startsWith("photos/") || name.contains("/photos/")) {
                                val fileName = File(name).name
                                val savedPath = MediaStorageManager.saveImportedStream(context, zis, "DayOne", fileName)
                                extractedMediaMap[fileName] = savedPath
                                extractedMediaMap[fileName.substringBeforeLast('.')] = savedPath
                            } else if (name.startsWith("audio/") || name.startsWith("audios/") ||
                                name.contains("/audio/") || name.contains("/audios/")
                            ) {
                                val fileName = File(name).name
                                val savedAudio = AudioStorageManager.saveImportedAudioStream(context, zis, "DayOne", fileName)
                                if (savedAudio != null) {
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
                return Result.failure(IllegalStateException("No Day One Journal.json found"))
            }

            val (journals, versionWarning) = parseDayOneJson(jsonContent!!, extractedMediaMap, extractedAudioMap)
            if (journals.isEmpty()) {
                return Result.failure(IllegalStateException("No entries found in Day One export"))
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
                    source = ImportSource.DayOne,
                    totalJournals = journals.size,
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
        }
    }

    internal fun parseDayOneJson(
        jsonString: String,
        mediaMap: Map<String, String>,
        audioMap: Map<String, JournalAudio>
    ): Pair<List<Journal>, String?> {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val metadata = root["metadata"]?.let { if (it is JsonObject) it else null }
        val version = metadata?.get("version")?.jsonPrimitive?.content
        val versionWarning = if (version != null && version != SUPPORTED_VERSION) {
            "Day One export version $version detected (expected $SUPPORTED_VERSION). Best-effort import applied."
        } else null

        val entriesArray = root["entries"]?.jsonArray ?: return Pair(emptyList(), versionWarning)
        val journals = mutableListOf<Journal>()

        for (item in entriesArray) {
            if (item is JsonObject) {
                parseDayOneEntry(item, mediaMap, audioMap)?.let { journals.add(it) }
            }
        }

        return Pair(journals, versionWarning)
    }

    private fun parseDayOneEntry(
        entry: JsonObject,
        mediaMap: Map<String, String>,
        audioMap: Map<String, JournalAudio>
    ): Journal? {
        val rawText = entry["text"]?.jsonPrimitive?.content ?: ""
        val creationDateStr = entry["creationDate"]?.jsonPrimitive?.content ?: return null
        val modifiedDateStr = entry["modifiedDate"]?.jsonPrimitive?.content
        val isStarred = entry["starred"]?.jsonPrimitive?.booleanOrNull ?: false
        val externalUuid = entry["uuid"]?.jsonPrimitive?.content

        val epochMs = parseIsoTime(creationDateStr)
        val updatedMs = modifiedDateStr?.let { parseIsoTime(it) }

        val images = mutableListOf<String>()
        var textWithResolvedMedia = rawText

        entry["photos"]?.jsonArray?.forEach { photoEl ->
            if (photoEl is JsonObject) {
                val md5 = photoEl["md5"]?.jsonPrimitive?.content
                val type = photoEl["type"]?.jsonPrimitive?.content ?: "jpg"
                val identifier = photoEl["identifier"]?.jsonPrimitive?.content
                val filename = photoEl["filename"]?.jsonPrimitive?.content

                val localPath = (md5?.let { mediaMap[it] ?: mediaMap["$it.$type"] })
                    ?: (identifier?.let { mediaMap[it] })
                    ?: (filename?.let { mediaMap[it] ?: mediaMap[File(it).name] })

                if (localPath != null) {
                    images.add(localPath)
                    if (identifier != null) {
                        textWithResolvedMedia = textWithResolvedMedia.replace(
                            "![](dayone-moment://$identifier)",
                            "![]($localPath)"
                        )
                    }
                }
            }
        }

        val cleanedText = textWithResolvedMedia
            .replace(Regex("!\\[\\]\\(dayone-moment://[a-zA-Z0-9/_-]+\\)"), "")
            .trim()
        val (title, content) = splitTitleAndContent(cleanedText, epochMs)

        val locObj = entry["location"]?.let { if (it is JsonObject) it else null }
        val location = locObj?.let {
            val lat = it["latitude"]?.jsonPrimitive?.doubleOrNull
            val lon = it["longitude"]?.jsonPrimitive?.doubleOrNull
            val placeName = it["placeName"]?.jsonPrimitive?.content
            val localityName = it["localityName"]?.jsonPrimitive?.content
            val adminArea = it["administrativeArea"]?.jsonPrimitive?.content
            val country = it["country"]?.jsonPrimitive?.content

            if (lat != null && lon != null) {
                val fullAddress = listOfNotNull(localityName, adminArea, country).joinToString(", ")
                JournalLocation(
                    latitude = lat,
                    longitude = lon,
                    name = placeName ?: localityName,
                    locality = localityName,
                    address = fullAddress.ifBlank { null }
                )
            } else null
        }

        val weatherObj = entry["weather"]?.let { if (it is JsonObject) it else null }
        val weatherDesc = weatherObj?.get("conditionsDescription")?.jsonPrimitive?.content
        val emoji = MoodEmojiMapper.fromWeather(weatherDesc)

        val tagsList = mutableListOf<String>()
        entry["tags"]?.jsonArray?.forEach { tagEl ->
            tagEl.jsonPrimitive.content.let { tagsList.add(it) }
        }
        val normalizedTags = TagUtils.normalizeTags(tagsList)

        val audios = mutableListOf<JournalAudio>()
        entry["audios"]?.jsonArray?.forEach { audioEl ->
            if (audioEl is JsonObject) {
                val md5 = audioEl["md5"]?.jsonPrimitive?.content
                val type = audioEl["type"]?.jsonPrimitive?.content ?: "m4a"
                val identifier = audioEl["identifier"]?.jsonPrimitive?.content
                val filename = audioEl["filename"]?.jsonPrimitive?.content

                val localAudio = (md5?.let { audioMap[it] ?: audioMap["$it.$type"] })
                    ?: (identifier?.let { audioMap[it] })
                    ?: (filename?.let { audioMap[it] ?: audioMap[File(it).name] })

                if (localAudio != null) {
                    audios.add(localAudio)
                }
            }
        }

        val deterministicId = if (!externalUuid.isNullOrBlank()) {
            ImporterHelper.generateDeterministicId(ImportSource.DayOne, externalUuid)
        } else {
            ImporterHelper.generateDeterministicId(ImportSource.DayOne, "$epochMs")
        }

        return Journal(
            id = deterministicId,
            title = title,
            content = content,
            emoji = emoji,
            images = images,
            audios = audios,
            location = location,
            tags = normalizedTags,
            isBookmarked = isStarred,
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
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(isoString, Instant::from).toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
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
                .withZone(java.time.ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(fallbackTimeMs))
            Pair(dateStr, text)
        }
    }
}
