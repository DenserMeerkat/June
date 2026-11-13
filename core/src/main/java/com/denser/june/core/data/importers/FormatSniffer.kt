package com.denser.june.core.data.importers

import android.content.Context
import android.net.Uri
import com.denser.june.core.domain.importers.ImportSource
import com.denser.june.core.utils.FileUtils
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object FormatSniffer {

    private val SQLITE_HEADER = "SQLite format 3".toByteArray(Charsets.US_ASCII)
    private val ZIP_HEADER = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

    fun sniff(context: Context, uri: Uri): ImportSource? {
        val displayName = (FileUtils.getDisplayName(context, uri) ?: uri.lastPathSegment ?: "").lowercase()
        if (displayName.endsWith(".diag") || displayName.endsWith(".diary")) return ImportSource.Diarium
        if (displayName.endsWith(".sp")) return ImportSource.StoryPad

        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                sniff(stream, displayName)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun sniff(stream: InputStream, displayName: String = ""): ImportSource? {
        val lowerName = displayName.lowercase()
        if (lowerName.endsWith(".diag") || lowerName.endsWith(".diary")) return ImportSource.Diarium
        if (lowerName.endsWith(".sp")) return ImportSource.StoryPad

        val bis = if (stream.markSupported()) stream else BufferedInputStream(stream)
        bis.mark(65536)
        val header = ByteArray(32)
        val read = bis.read(header)
        if (read < 4) return null

        if (header.take(SQLITE_HEADER.size) == SQLITE_HEADER.toList()) {
            return ImportSource.Diarium
        }

        bis.reset()
        if (header[0] == ZIP_HEADER[0] && header[1] == ZIP_HEADER[1] &&
            header[2] == ZIP_HEADER[2] && header[3] == ZIP_HEADER[3]
        ) {
            return sniffZipStream(bis)
        }

        return sniffJsonStream(bis)
    }

    private fun sniffZipStream(stream: InputStream): ImportSource? {
        return try {
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                var foundDailyYou = false
                var foundDayOne = false
                var foundStoryPad = false

                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (name == "journal.json" || name.endsWith("/journal.json")) {
                        foundDayOne = true
                        break
                    }
                    if (name == "daily_you_export.json" || name.endsWith("/daily_you_export.json") ||
                        name == "daily_you.db" || name.endsWith("/daily_you.db")
                    ) {
                        foundDailyYou = true
                        break
                    }
                    if (name.endsWith(".json") || name.endsWith(".sp")) {
                        val buffer = ByteArray(8192)
                        val bytesRead = zis.read(buffer)
                        if (bytesRead > 0) {
                            val snippet = String(buffer, 0, bytesRead, Charsets.UTF_8)
                            if (snippet.contains("daily-you")) {
                                foundDailyYou = true
                                break
                            }
                            if (snippet.contains("stories") && (snippet.contains("tables") || snippet.contains("data") || snippet.contains("meta_data"))) {
                                foundStoryPad = true
                                break
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }

                when {
                    foundDayOne -> ImportSource.DayOne
                    foundDailyYou -> ImportSource.DailyYou
                    foundStoryPad -> ImportSource.StoryPad
                    else -> null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sniffJsonStream(stream: InputStream): ImportSource? {
        return try {
            val buffer = ByteArray(32768)
            val read = stream.read(buffer)
            if (read > 0) {
                val snippet = String(buffer, 0, read, Charsets.UTF_8)
                when {
                    snippet.contains("daily-you") || snippet.contains("mood_scale") -> ImportSource.DailyYou
                    snippet.contains("\"stories\"") || (snippet.contains("tables") && snippet.contains("stories")) || (snippet.contains("data") && snippet.contains("stories")) -> ImportSource.StoryPad
                    snippet.contains("entries") && (snippet.contains("Journal") || snippet.contains("creationDate") || snippet.contains("uuid")) -> ImportSource.DayOne
                    else -> null
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
