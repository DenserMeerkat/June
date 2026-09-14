package com.denser.june.core.domain.markdown

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.JournalLocation
import com.denser.june.core.domain.model.SongDetails
import kotlinx.serialization.Serializable
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

@Serializable
private data class JournalFrontmatter(
    val id: String? = null,
    val title: String? = null,
    val emoji: String? = null,
    val createdAt: String? = null,
    val created: String? = null,
    val updatedAt: String? = null,
    val updated: String? = null,
    val dateTime: String? = null,
    val date: String? = null,
    val isBookmarked: Boolean = false,
    val bookmarked: Boolean = false,
    val isArchived: Boolean = false,
    val archived: Boolean = false,
    val tags: List<String> = emptyList(),
    val tag: String? = null,
    val location: JournalLocation? = null,
    val song: SongDetails? = null
)

object MarkdownEngine {
    private const val TAG = "MarkdownEngine"

    private val frontmatterRegex = Regex("^---\\s*\\n([\\s\\S]*?)\\n---(?:\\s*\\n)?")
    private val mediaRegex = Regex("(?:!\\[[^\\]]*\\]\\(([^\\)]+)\\)|!\\[\\[([^\\]]+)\\]\\])")

    private val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false,
            encodeDefaults = false
        )
    )

    private val yamlDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    private val fileDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HHmm").withZone(ZoneId.systemDefault())

    fun toMarkdown(
        journal: Journal,
        relativeMediaPathPrefix: String? = null,
        includeMedia: Boolean = true
    ): String {
        val frontmatter = JournalFrontmatter(
            id = journal.id,
            title = journal.title.ifBlank { null },
            emoji = journal.emoji?.ifBlank { null },
            createdAt = yamlDateFormatter.format(Instant.ofEpochMilli(journal.createdAt)),
            updatedAt = journal.updatedAt?.let { yamlDateFormatter.format(Instant.ofEpochMilli(it)) },
            dateTime = yamlDateFormatter.format(Instant.ofEpochMilli(journal.dateTime)),
            isBookmarked = journal.isBookmarked,
            isArchived = journal.isArchived,
            tags = journal.tags,
            location = journal.location,
            song = journal.songDetails
        )
        val yamlString = yaml.encodeToString(JournalFrontmatter.serializer(), frontmatter).trim()

        val sb = StringBuilder()
        sb.append("---\n")
        sb.append(yamlString)
        sb.append("\n---\n\n")

        if (journal.title.isNotBlank()) {
            sb.append("# ").append(journal.title).append("\n\n")
        }

        val metaParts = mutableListOf<String>()
        val readableDate = fileDateFormatter.format(Instant.ofEpochMilli(journal.dateTime))
        metaParts.add(readableDate)
        journal.location?.let { loc ->
            val locName = loc.name ?: loc.address ?: loc.locality
            if (!locName.isNullOrBlank()) metaParts.add("📍 $locName")
        }
        journal.songDetails?.let { song ->
            if (song.title.isNotBlank()) {
                val songText = if (song.artistName.isNotBlank()) "${song.title} — ${song.artistName}" else song.title
                metaParts.add("🎵 $songText")
            }
        }
        if (metaParts.size > 1) {
            sb.append("*").append(metaParts.joinToString(" • ")).append("*\n\n")
        }

        var content = journal.content
        if (includeMedia && !relativeMediaPathPrefix.isNullOrBlank()) {
            journal.images.forEach { imgPath ->
                val fileName = File(imgPath).name
                val relativePath = "$relativeMediaPathPrefix/$fileName"
                content = content.replace(imgPath, relativePath)
            }
        }
        sb.append(content)

        if (includeMedia) {
            val unreferencedImages = journal.images.filter { img ->
                val fileName = File(img).name
                !content.contains(img) && !content.contains(fileName)
            }
            if (unreferencedImages.isNotEmpty()) {
                sb.append("\n\n## Media\n")
                unreferencedImages.forEach { img ->
                    val fileName = File(img).name
                    val displayPath = if (!relativeMediaPathPrefix.isNullOrBlank()) {
                        "$relativeMediaPathPrefix/$fileName"
                    } else {
                        img
                    }
                    sb.append("![").append(fileName).append("](").append(displayPath).append(")\n")
                }
            }
        }

        return sb.toString()
    }

    fun fromMarkdown(rawText: String, fallbackTitle: String? = null, isDraft: Boolean = false): Journal {
        val normalized = rawText.replace("\r\n", "\n").replace('\r', '\n')
        val match = frontmatterRegex.find(normalized)

        val (frontmatter, rawBody) = if (match != null && match.range.first == 0) {
            val yamlText = match.groupValues[1].trim()
            val body = normalized.substring(match.range.last + 1).trimStart('\n', ' ')
            val parsedFm = try {
                yaml.decodeFromString(JournalFrontmatter.serializer(), yamlText)
            } catch (e: Exception) {
                AppLogger.w(AppLogger.Category.BACKUP, TAG, "Failed to parse YAML frontmatter: ${e.message}")
                null
            }
            parsedFm to body
        } else {
            null to normalized.trim()
        }

        val mediaHeaderIdx = rawBody.lastIndexOf("\n## Media")
        var content = if (mediaHeaderIdx != -1) rawBody.substring(0, mediaHeaderIdx).trimEnd() else rawBody

        var title = frontmatter?.title ?: ""
        if (title.isBlank()) {
            val firstLine = content.lines().firstOrNull()?.trim() ?: ""
            if (firstLine.startsWith("# ") || firstLine.startsWith("## ")) {
                title = firstLine.removePrefix("## ").removePrefix("# ").trim()
                content = content.lines().drop(1).joinToString("\n").trimStart('\n', ' ')
            } else if (!fallbackTitle.isNullOrBlank()) {
                title = cleanFallbackTitle(fallbackTitle)
            }
        } else {
            val firstLine = content.lines().firstOrNull()?.trim() ?: ""
            if (firstLine.equals("# $title", ignoreCase = true) || firstLine.equals("## $title", ignoreCase = true)) {
                content = content.lines().drop(1).joinToString("\n").trimStart('\n', ' ')
            }
        }

        val lines = content.lines()
        val firstNonEmptyLine = lines.firstOrNull { it.isNotBlank() }?.trim() ?: ""
        if (firstNonEmptyLine.startsWith("*") && firstNonEmptyLine.endsWith("*") && (firstNonEmptyLine.contains("📍") || firstNonEmptyLine.contains("🎵"))) {
            val dropIndex = lines.indexOfFirst { it.trim() == firstNonEmptyLine }
            content = lines.drop(dropIndex + 1).joinToString("\n").trimStart('\n', ' ')
        }

        val images = mediaRegex.findAll(rawBody).mapNotNull { matchResult ->
            (matchResult.groups[1]?.value ?: matchResult.groups[2]?.value)?.trim()?.takeIf { it.isNotBlank() }
        }.distinct().toList()

        val combinedTags = mutableListOf<String>()
        frontmatter?.tags?.let { combinedTags.addAll(it) }
        frontmatter?.tag?.let { if (it.isNotBlank() && !combinedTags.contains(it)) combinedTags.add(it) }

        val createdAt = parseDate(frontmatter?.createdAt ?: frontmatter?.created)
        val updatedAt = parseDate(frontmatter?.updatedAt ?: frontmatter?.updated)
        val dateTime = parseDate(frontmatter?.dateTime ?: frontmatter?.date)
        val finalDateTime = dateTime ?: createdAt ?: System.currentTimeMillis()
        val finalCreatedAt = createdAt ?: finalDateTime

        return Journal(
            id = frontmatter?.id?.ifBlank { null } ?: UUID.randomUUID().toString(),
            title = title,
            content = content,
            emoji = frontmatter?.emoji?.ifBlank { null },
            images = images,
            location = frontmatter?.location,
            songDetails = frontmatter?.song,
            tags = combinedTags.distinct(),
            createdAt = finalCreatedAt,
            updatedAt = updatedAt,
            dateTime = finalDateTime,
            isBookmarked = frontmatter?.isBookmarked ?: frontmatter?.bookmarked ?: false,
            isArchived = frontmatter?.isArchived ?: frontmatter?.archived ?: false,
            isDraft = isDraft
        )
    }

    fun generateFileName(journal: Journal, forSingleExport: Boolean = false): String {
        val formattedDate = fileDateFormatter.format(Instant.ofEpochMilli(journal.dateTime))
        val rawTitle = journal.title.ifBlank {
            journal.content.lines().firstOrNull()?.trim()?.take(40) ?: ""
        }
        val sanitizedTitle = rawTitle
            .replace("[\\\\/:*?\"<>|]+".toRegex(), "")
            .replace("\\s+".toRegex(), "_")
            .trim('_', ' ')
            .take(50)

        return when {
            forSingleExport && sanitizedTitle.isNotEmpty() -> "${sanitizedTitle}.md"
            sanitizedTitle.isNotEmpty() -> "${formattedDate}_${sanitizedTitle}.md"
            else -> {
                val formattedTime = timeFormatter.format(Instant.ofEpochMilli(journal.dateTime))
                "${formattedDate}_${formattedTime}.md"
            }
        }
    }

    private fun cleanFallbackTitle(name: String): String {
        var res = name
        if (res.endsWith(".md", ignoreCase = true)) res = res.substring(0, res.length - 3)
        if (res.endsWith(".markdown", ignoreCase = true)) res = res.substring(0, res.length - 9)
        res = res.replace("^[0-9]{4}-[0-9]{2}-[0-9]{2}_?".toRegex(), "")
        res = res.replace("_[0-9a-fA-F-]{36}$".toRegex(), "")
        res = res.replace("_[0-9]+$".toRegex(), "")
        return res.replace("_", " ").trim()
    }

    private fun parseDate(str: String?): Long? {
        if (str.isNullOrBlank()) return null
        str.toLongOrNull()?.let { return it }

        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                if (pattern == "yyyy-MM-dd") {
                    val localDate = LocalDate.parse(str, DateTimeFormatter.ofPattern(pattern))
                    return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } else {
                    val formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault())
                    val parsed = formatter.parse(str)
                    return Instant.from(parsed).toEpochMilli()
                }
            } catch (_: DateTimeParseException) {
                // Try next
            } catch (_: Exception) {
                // Try next
            }
        }
        return null
    }
}
