package com.denser.june.core.media.exif

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.denser.june.core.media.exif.model.AudioMetadata
import com.denser.june.core.media.exif.model.MediaMetadata
import com.denser.june.core.media.exif.model.PhotoMetadata
import com.denser.june.core.media.exif.model.VideoMetadata
import com.denser.june.core.media.storage.AudioStorageManager
import com.denser.june.core.media.storage.MediaStorageManager
import com.denser.june.core.utils.FileUtils
import com.denser.june.core.utils.formatBytes
import com.denser.june.core.utils.toAudioTimestamp
import com.denser.june.core.utils.toExifDateTimeDisplay

object MediaMetadataReader {
    private val VIDEO_EXTENSIONS = setOf(".mp4", ".mov", ".m4v", ".mkv", ".webm", ".3gp", ".avi", ".ts")
    private val AUDIO_EXTENSIONS = setOf(".m4a", ".mp3", ".aac", ".wav", ".ogg", ".flac", ".opus", ".m4r", ".wma", ".amr")
    private val REGEX_NUMBERS_ONLY = Regex("^[0-9.]+$")

    fun isVideoPath(pathOrName: String): Boolean =
        VIDEO_EXTENSIONS.any { pathOrName.endsWith(it, ignoreCase = true) }

    fun isAudioPath(pathOrName: String): Boolean =
        AUDIO_EXTENSIONS.any { pathOrName.endsWith(it, ignoreCase = true) }

    fun isPhotoPath(pathOrName: String): Boolean =
        !isVideoPath(pathOrName) && !isAudioPath(pathOrName)

    fun readMediaExif(
        context: Context,
        path: String,
        fallbackDateTime: Long? = null,
        journalTitle: String? = null,
        journalMediaCount: Int = 0,
        isBackedUp: Boolean = false
    ): MediaMetadata = readMediaMetadata(
        context = context,
        path = path,
        fallbackDateTime = fallbackDateTime,
        journalTitle = journalTitle,
        journalMediaCount = journalMediaCount,
        isBackedUp = isBackedUp
    )

    fun readMediaMetadata(
        context: Context,
        path: String,
        fallbackDateTime: Long? = null,
        journalTitle: String? = null,
        journalMediaCount: Int = 0,
        isBackedUp: Boolean = false
    ): MediaMetadata {
        val cleanPath = sanitizeMediaPath(path)

        if (cleanPath.startsWith("content://")) {
            val uri = cleanPath.toUri()
            val rawResolverName = FileUtils.getDisplayName(context, uri) ?: "media"
            val nameFromResolver = if (rawResolverName.contains("_media_")) {
                "media_" + rawResolverName.substringAfterLast("_media_")
            } else {
                rawResolverName
            }
            val isAudioUri = (context.contentResolver.getType(uri)?.startsWith("audio") == true) ||
                isAudioPath(nameFromResolver) ||
                isAudioPath(cleanPath)
            val isVideoUri = (context.contentResolver.getType(uri)?.startsWith("video") == true) ||
                isVideoPath(nameFromResolver) ||
                isVideoPath(cleanPath)
            val size = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } catch (_: Exception) {
                0L
            }
            val sizeFormatted = size.formatBytes()

            if (isAudioUri) {
                val audioMeta = AudioMetadataParser.parseAudioMetadata(context, uri, fallbackDateTime)
                return AudioMetadata(
                    dateTaken = audioMeta.dateTakenMillis.toExifDateTimeDisplay(),
                    dateTakenMillis = audioMeta.dateTakenMillis,
                    journalTitle = journalTitle,
                    journalMediaCount = journalMediaCount,
                    deviceName = audioMeta.deviceName ?: "Audio Recording",
                    duration = if (audioMeta.durationMs > 0L) audioMeta.durationMs.toAudioTimestamp() else null,
                    durationMs = audioMeta.durationMs,
                    bitrate = audioMeta.bitrate,
                    sampleRate = audioMeta.sampleRate,
                    channels = audioMeta.channels,
                    audioFormat = audioMeta.format,
                    title = audioMeta.title,
                    fileName = nameFromResolver,
                    fileSizeFormatted = sizeFormatted,
                    fileSizeBytes = size,
                    filePath = cleanPath,
                    isBackedUp = isBackedUp
                )
            }

            if (isVideoUri) {
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                } catch (_: Exception) {}

                val videoMeta = VideoMetadataParser.parseVideoMetadata(retriever, fallbackDateTime)
                val (isHdr, hdrFormat) = try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        VideoMetadataParser.isHdrVideo(retriever, pfd = pfd)
                    } ?: VideoMetadataParser.isHdrVideo(retriever)
                } catch (_: Exception) {
                    Pair(false, null)
                }

                try { retriever.release() } catch (_: Exception) {}

                val extractedDevice = try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        VideoDeviceParser.extractVideoDeviceInfo(pfd)
                    } ?: context.contentResolver.openInputStream(uri)?.use { VideoDeviceParser.extractVideoDeviceInfo(it) }
                } catch (_: Exception) {
                    null
                }

                val cleanDevice = extractedDevice?.trim()?.takeIf { !it.matches(REGEX_NUMBERS_ONLY) && it.length >= 3 }

                return VideoMetadata(
                    dateTaken = videoMeta.dateTakenMillis.toExifDateTimeDisplay(),
                    dateTakenMillis = videoMeta.dateTakenMillis,
                    journalTitle = journalTitle,
                    journalMediaCount = journalMediaCount,
                    deviceName = cleanDevice ?: "Video Details",
                    duration = videoMeta.durationStr,
                    fps = videoMeta.fps,
                    bitrate = videoMeta.bitrate,
                    qualityLabel = videoMeta.qualityLabel,
                    fileName = nameFromResolver,
                    width = videoMeta.width,
                    height = videoMeta.height,
                    resolution = videoMeta.resolution,
                    fileSizeFormatted = sizeFormatted,
                    fileSizeBytes = size,
                    filePath = cleanPath,
                    isBackedUp = isBackedUp,
                    isHdr = isHdr,
                    hdrFormat = hdrFormat
                )
            } else {
                var w = 0
                var h = 0
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeStream(stream, null, bounds)
                        w = bounds.outWidth
                        h = bounds.outHeight
                    }
                } catch (_: Exception) {}

                val exif = try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        ExifInterface(stream)
                    }
                } catch (_: Exception) {
                    null
                }

                val photo = PhotoMetadataParser.parsePhotoExif(
                    exif = exif,
                    rawWidth = w,
                    rawHeight = h,
                    isScreenshot = nameFromResolver.contains("Screenshot", ignoreCase = true),
                    fallbackDateTime = fallbackDateTime
                )

                return PhotoMetadata(
                    dateTaken = photo.dateTakenMillis.toExifDateTimeDisplay(),
                    dateTakenMillis = photo.dateTakenMillis,
                    journalTitle = journalTitle,
                    journalMediaCount = journalMediaCount,
                    deviceName = photo.deviceName,
                    aperture = photo.aperture,
                    shutterSpeed = photo.shutterSpeed,
                    focalLength = photo.focalLength,
                    iso = photo.iso,
                    fileName = nameFromResolver,
                    width = photo.width,
                    height = photo.height,
                    megapixels = photo.megapixels,
                    resolution = photo.resolution,
                    fileSizeFormatted = sizeFormatted,
                    fileSizeBytes = size,
                    filePath = cleanPath,
                    isBackedUp = isBackedUp,
                    isHdr = photo.isHdr
                )
            }
        }

        val isAudioFile = isAudioPath(cleanPath)
        val file = if (isAudioFile) {
            AudioStorageManager.resolveAudioFile(context, cleanPath)
        } else {
            MediaStorageManager.resolveMediaFile(context, cleanPath)
        }
        val rawName = file.name
        val name = if (rawName.contains("_media_")) {
            "media_" + rawName.substringAfterLast("_media_")
        } else {
            rawName
        }
        val size = if (file.exists()) file.length() else 0L
        val sizeFormatted = size.formatBytes()
        val isVideoFile = isVideoPath(name) || isVideoPath(cleanPath)

        if (isAudioFile && file.exists() && size > 0L) {
            val audioMeta = AudioMetadataParser.parseAudioMetadata(context, file, fallbackDateTime)
            return AudioMetadata(
                dateTaken = audioMeta.dateTakenMillis.toExifDateTimeDisplay(),
                dateTakenMillis = audioMeta.dateTakenMillis,
                journalTitle = journalTitle,
                journalMediaCount = journalMediaCount,
                deviceName = audioMeta.deviceName ?: "Audio Recording",
                duration = if (audioMeta.durationMs > 0L) audioMeta.durationMs.toAudioTimestamp() else null,
                durationMs = audioMeta.durationMs,
                bitrate = audioMeta.bitrate,
                sampleRate = audioMeta.sampleRate,
                channels = audioMeta.channels,
                audioFormat = audioMeta.format,
                title = audioMeta.title,
                fileName = name,
                fileSizeFormatted = sizeFormatted,
                fileSizeBytes = size,
                filePath = file.absolutePath,
                isBackedUp = isBackedUp
            )
        }

        if (isVideoFile && file.exists() && size > 0L) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
            } catch (_: Exception) {}

            val videoMeta = VideoMetadataParser.parseVideoMetadata(retriever, fallbackDateTime ?: file.lastModified())
            val (isHdr, hdrFormat) = VideoMetadataParser.isHdrVideo(retriever, file = file)
            try { retriever.release() } catch (_: Exception) {}

            val extractedDevice = VideoDeviceParser.extractVideoDeviceInfo(file)
            val cleanDevice = extractedDevice?.trim()?.takeIf { !it.matches(REGEX_NUMBERS_ONLY) && it.length >= 3 }

            return VideoMetadata(
                dateTaken = videoMeta.dateTakenMillis.toExifDateTimeDisplay(),
                dateTakenMillis = videoMeta.dateTakenMillis,
                journalTitle = journalTitle,
                journalMediaCount = journalMediaCount,
                deviceName = cleanDevice ?: "Video Details",
                duration = videoMeta.durationStr,
                fps = videoMeta.fps,
                bitrate = videoMeta.bitrate,
                qualityLabel = videoMeta.qualityLabel,
                fileName = name,
                width = videoMeta.width,
                height = videoMeta.height,
                resolution = videoMeta.resolution,
                fileSizeFormatted = sizeFormatted,
                fileSizeBytes = size,
                filePath = file.absolutePath,
                isBackedUp = isBackedUp,
                isHdr = isHdr,
                hdrFormat = hdrFormat
            )
        }

        if (!file.exists() || size == 0L) {
            val dateMillis = fallbackDateTime ?: System.currentTimeMillis()
            return if (isVideoFile) {
                VideoMetadata(
                    dateTaken = dateMillis.toExifDateTimeDisplay(),
                    dateTakenMillis = dateMillis,
                    journalTitle = journalTitle,
                    journalMediaCount = journalMediaCount,
                    deviceName = "Video Details",
                    fileName = name,
                    fileSizeFormatted = sizeFormatted,
                    fileSizeBytes = size,
                    filePath = file.absolutePath,
                    isBackedUp = isBackedUp
                )
            } else if (isAudioFile) {
                AudioMetadata(
                    dateTaken = dateMillis.toExifDateTimeDisplay(),
                    dateTakenMillis = dateMillis,
                    journalTitle = journalTitle,
                    journalMediaCount = journalMediaCount,
                    deviceName = "Audio Recording",
                    fileName = name,
                    fileSizeFormatted = sizeFormatted,
                    fileSizeBytes = size,
                    filePath = file.absolutePath,
                    isBackedUp = isBackedUp
                )
            } else {
                PhotoMetadata(
                    dateTaken = dateMillis.toExifDateTimeDisplay(),
                    dateTakenMillis = dateMillis,
                    journalTitle = journalTitle,
                    journalMediaCount = journalMediaCount,
                    deviceName = "Photo Details",
                    fileName = name,
                    fileSizeFormatted = sizeFormatted,
                    fileSizeBytes = size,
                    filePath = file.absolutePath,
                    isBackedUp = isBackedUp
                )
            }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            BitmapFactory.decodeFile(file.absolutePath, bounds)
        } catch (_: Exception) {}

        val exif = try {
            ExifInterface(file.absolutePath)
        } catch (_: Exception) {
            try {
                file.inputStream().use { stream -> ExifInterface(stream) }
            } catch (_: Exception) {
                null
            }
        }

        val photo = PhotoMetadataParser.parsePhotoExif(
            exif = exif,
            rawWidth = bounds.outWidth,
            rawHeight = bounds.outHeight,
            isScreenshot = name.contains("Screenshot", ignoreCase = true),
            fallbackDateTime = fallbackDateTime ?: file.lastModified()
        )

        return PhotoMetadata(
            dateTaken = photo.dateTakenMillis.toExifDateTimeDisplay(),
            dateTakenMillis = photo.dateTakenMillis,
            journalTitle = journalTitle,
            journalMediaCount = journalMediaCount,
            deviceName = photo.deviceName,
            aperture = photo.aperture,
            shutterSpeed = photo.shutterSpeed,
            focalLength = photo.focalLength,
            iso = photo.iso,
            fileName = name,
            width = photo.width,
            height = photo.height,
            megapixels = photo.megapixels,
            resolution = photo.resolution,
            fileSizeFormatted = sizeFormatted,
            fileSizeBytes = size,
            filePath = file.absolutePath,
            isBackedUp = isBackedUp,
            isHdr = photo.isHdr
        )
    }

    private fun sanitizeMediaPath(path: String): String {
        var decoded = path
        while (decoded.contains("%2F", ignoreCase = true) || decoded.contains("%3A", ignoreCase = true)) {
            val next = try { Uri.decode(decoded) } catch (_: Exception) { decoded } ?: decoded
            if (next == decoded) break
            decoded = next
        }
        return if (decoded.startsWith("file://")) decoded.removePrefix("file://") else decoded
    }
}

typealias ExifUtils = MediaMetadataReader
