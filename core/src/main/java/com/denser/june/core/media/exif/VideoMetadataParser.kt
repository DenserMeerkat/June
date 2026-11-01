package com.denser.june.core.media.exif

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.ParcelFileDescriptor
import com.denser.june.core.utils.toSongTimestamp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.round

data class ParsedVideoInfo(
    val width: Int,
    val height: Int,
    val durationStr: String?,
    val dateTakenMillis: Long,
    val fps: String?,
    val bitrate: String?,
    val qualityLabel: String?,
    val resolution: String?
)

object VideoMetadataParser {
    private const val METADATA_KEY_VIDEO_CODEC_MIME_TYPE = 40

    fun parseVideoMetadata(
        retriever: MediaMetadataRetriever,
        fallbackDateTime: Long?
    ): ParsedVideoInfo {
        var vWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        var vHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        if (rotation == 90 || rotation == 270) {
            val temp = vWidth
            vWidth = vHeight
            vHeight = temp
        }

        val vDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val durationStr = if (vDuration > 0L) vDuration.toSongTimestamp() else null

        val dateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
        val dateTakenMillis = parseIsoDate(dateStr) ?: fallbackDateTime ?: System.currentTimeMillis()

        val fps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()?.let {
            "${round(it).toInt()} fps"
        }

        val bitrateLong = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
        val bitrate = if (bitrateLong != null && bitrateLong > 0L) {
            String.format(Locale.getDefault(), "%.1f Mbps", bitrateLong / 1_000_000f)
        } else null

        val shortEdge = minOf(vWidth, vHeight).takeIf { it > 0 } ?: vHeight
        val qualityLabel = when {
            shortEdge >= 2160 -> "4K"
            shortEdge >= 1440 -> "2K"
            shortEdge >= 1080 -> "1080p"
            shortEdge >= 720 -> "720p"
            shortEdge >= 480 -> "480p"
            shortEdge > 0 -> "${shortEdge}p"
            else -> null
        }
        val resolution = if (vWidth > 0 && vHeight > 0) "$vWidth x $vHeight" else null

        return ParsedVideoInfo(
            width = vWidth,
            height = vHeight,
            durationStr = durationStr,
            dateTakenMillis = dateTakenMillis,
            fps = fps,
            bitrate = bitrate,
            qualityLabel = qualityLabel,
            resolution = resolution
        )
    }

    fun isHdrVideo(
        retriever: MediaMetadataRetriever,
        pfd: ParcelFileDescriptor? = null,
        file: File? = null
    ): Pair<Boolean, String?> {
        val codecMime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            retriever.extractMetadata(METADATA_KEY_VIDEO_CODEC_MIME_TYPE)
        } else null
        if (codecMime?.contains("dolby-vision", ignoreCase = true) == true || codecMime?.contains("dvhe", ignoreCase = true) == true) {
            return Pair(true, "Dolby Vision")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val colorTransfer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)?.toIntOrNull()
            val colorStandard = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_STANDARD)?.toIntOrNull()
            if (colorTransfer == 7) {
                return Pair(true, "HLG")
            } else if (colorTransfer == 6) {
                return Pair(true, "HDR10")
            } else if (colorStandard == 6) {
                return Pair(true, "HDR")
            }
        }
        try {
            val extractor = MediaExtractor()
            if (pfd != null) {
                extractor.setDataSource(pfd.fileDescriptor)
            } else if (file != null && file.exists()) {
                extractor.setDataSource(file.absolutePath)
            } else {
                return Pair(false, null)
            }
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    if (mime.contains("dolby-vision", ignoreCase = true) || mime.contains("dvhe", ignoreCase = true)) {
                        extractor.release()
                        return Pair(true, "Dolby Vision")
                    }
                    val transfer = if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                        format.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
                    } else -1
                    val standard = if (format.containsKey(MediaFormat.KEY_COLOR_STANDARD)) {
                        format.getInteger(MediaFormat.KEY_COLOR_STANDARD)
                    } else -1
                    extractor.release()
                    if (transfer == 7) return Pair(true, "HLG")
                    if (transfer == 6) return Pair(true, "HDR10")
                    if (standard == 6 || format.containsKey(MediaFormat.KEY_HDR_STATIC_INFO)) return Pair(true, "HDR")
                    return Pair(false, null)
                }
            }
            extractor.release()
        } catch (_: Exception) {}
        return Pair(false, null)
    }

    private fun parseIsoDate(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(dateStr)?.time
        } catch (_: Exception) {
            null
        }
    }
}
