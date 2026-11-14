package com.denser.june.core.media.exif

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.denser.june.core.utils.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ParsedAudioInfo(
    val title: String?,
    val durationMs: Long,
    val dateTakenMillis: Long,
    val bitrate: String?,
    val sampleRate: String?,
    val channels: String?,
    val format: String,
    val deviceName: String?,
    val sizeBytes: Long,
    val fileName: String
)

object AudioMetadataParser {

    private const val MIN_VALID_EPOCH_MILLIS = 946684800000L

    fun parseAudioMetadata(
        context: Context,
        file: File,
        fallbackDateTime: Long? = null
    ): ParsedAudioInfo {
        val retriever = MediaMetadataRetriever()
        var duration = 0L
        var dateTakenMillis: Long? = null
        var rawBitrate: Long? = null
        var rawSampleRate: Int? = null
        var title: String? = null
        var artist: String? = null
        var author: String? = null

        try {
            if (file.exists() && file.length() > 0L) {
                retriever.setDataSource(file.absolutePath)
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                rawBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
                rawSampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
                author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)?.takeIf { it.isNotBlank() }
                val dateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                if (!dateStr.isNullOrBlank() && !dateStr.startsWith("1904") && !dateStr.startsWith("1970")) {
                    dateTakenMillis = parseIsoDate(dateStr)
                }
            }
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        if (dateTakenMillis == null || dateTakenMillis <= MIN_VALID_EPOCH_MILLIS) {
            val validFallback = fallbackDateTime?.takeIf { it > MIN_VALID_EPOCH_MILLIS }
            val lastMod = if (file.exists()) file.lastModified().takeIf { it > MIN_VALID_EPOCH_MILLIS } else null
            dateTakenMillis = validFallback ?: lastMod ?: System.currentTimeMillis()
        }

        var channelCount = 0
        if (file.exists() && file.length() > 0L) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(file.absolutePath)
                for (i in 0 until extractor.trackCount) {
                    val trackFormat = extractor.getTrackFormat(i)
                    val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channelCount = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        }
                        if (rawSampleRate == null && trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            rawSampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        break
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { extractor.release() } catch (_: Exception) {}
            }
        }

        val bitrateStr = if (rawBitrate != null && rawBitrate > 0L) {
            "${rawBitrate / 1000} kbps"
        } else null

        val sampleRateStr = if (rawSampleRate != null && rawSampleRate > 0) {
            if (rawSampleRate >= 1000) {
                String.format(Locale.getDefault(), "%.1f kHz", rawSampleRate / 1000f)
            } else {
                "$rawSampleRate Hz"
            }
        } else null

        val channelsStr = when (channelCount) {
            1 -> "Mono"
            2 -> "Stereo"
            in 3..8 -> "${channelCount}ch"
            else -> null
        }

        val device = artist ?: author ?: "Audio Recording"
        val format = file.extension.uppercase().ifBlank { "M4A" }
        val size = if (file.exists()) file.length() else 0L

        return ParsedAudioInfo(
            title = title,
            durationMs = duration,
            dateTakenMillis = dateTakenMillis,
            bitrate = bitrateStr,
            sampleRate = sampleRateStr,
            channels = channelsStr,
            format = format,
            deviceName = device,
            sizeBytes = size,
            fileName = file.name
        )
    }

    fun parseAudioMetadata(
        context: Context,
        uri: Uri,
        fallbackDateTime: Long? = null
    ): ParsedAudioInfo {
        val retriever = MediaMetadataRetriever()
        var duration = 0L
        var dateTakenMillis: Long? = null
        var rawBitrate: Long? = null
        var rawSampleRate: Int? = null
        var title: String? = null
        var artist: String? = null
        var author: String? = null

        try {
            retriever.setDataSource(context, uri)
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            rawBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            rawSampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
            author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)?.takeIf { it.isNotBlank() }
            val dateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            if (!dateStr.isNullOrBlank() && !dateStr.startsWith("1904") && !dateStr.startsWith("1970")) {
                dateTakenMillis = parseIsoDate(dateStr)
            }
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        if (dateTakenMillis == null || dateTakenMillis <= MIN_VALID_EPOCH_MILLIS) {
            val docMod = FileUtils.getDocumentLastModified(context, uri)?.takeIf { it > MIN_VALID_EPOCH_MILLIS }
            val validFallback = fallbackDateTime?.takeIf { it > MIN_VALID_EPOCH_MILLIS }
            dateTakenMillis = docMod ?: validFallback ?: System.currentTimeMillis()
        }

        val bitrateStr = if (rawBitrate != null && rawBitrate > 0L) {
            "${rawBitrate / 1000} kbps"
        } else null

        val sampleRateStr = if (rawSampleRate != null && rawSampleRate > 0) {
            if (rawSampleRate >= 1000) {
                String.format(Locale.getDefault(), "%.1f kHz", rawSampleRate / 1000f)
            } else {
                "$rawSampleRate Hz"
            }
        } else null

        val device = artist ?: author ?: "Audio Recording"

        return ParsedAudioInfo(
            title = title,
            durationMs = duration,
            dateTakenMillis = dateTakenMillis,
            bitrate = bitrateStr,
            sampleRate = sampleRateStr,
            channels = null,
            format = "M4A",
            deviceName = device,
            sizeBytes = 0L,
            fileName = "audio"
        )
    }

    private fun parseIsoDate(dateStr: String): Long? {
        val patterns = arrayOf(
            "yyyyMMdd'T'HHmmss.SSS'Z'",
            "yyyyMMdd'T'HHmmss'Z'",
            "yyyyMMdd'T'HHmmss",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    if (pattern.endsWith("'Z'")) {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                }
                val parsed = sdf.parse(dateStr)
                if (parsed != null && parsed.time > MIN_VALID_EPOCH_MILLIS) return parsed.time
            } catch (_: Exception) {}
        }
        return null
    }
}

