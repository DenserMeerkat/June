package com.denser.june.core.media.exif

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import com.denser.june.core.media.exif.model.MediaExifInfo
import com.denser.june.core.media.storage.MediaStorageManager
import com.denser.june.core.utils.FileUtils
import com.denser.june.core.utils.formatBytes
import com.denser.june.core.utils.toExifDateTimeDisplay
import java.io.File
import java.io.InputStream

object ExifUtils {
    private val VIDEO_EXTENSIONS = setOf(".mp4", ".mov", ".m4v", ".mkv", ".webm", ".3gp", ".avi", ".ts")
    private val REGEX_NUMBERS_ONLY = Regex("^[0-9.]+$")

    fun isVideoPath(pathOrName: String): Boolean =
        VIDEO_EXTENSIONS.any { pathOrName.endsWith(it, ignoreCase = true) }

    fun readMediaExif(
        context: Context,
        path: String,
        fallbackDateTime: Long? = null,
        journalTitle: String? = null,
        journalMediaCount: Int = 0,
        isBackedUp: Boolean = false
    ): MediaExifInfo {
        val cleanPath = sanitizeMediaPath(path)

        if (cleanPath.startsWith("content://")) {
            val uri = Uri.parse(cleanPath)
            val rawResolverName = FileUtils.getDisplayName(context, uri) ?: "media"
            val nameFromResolver = if (rawResolverName.contains("_media_")) "media_" + rawResolverName.substringAfterLast("_media_") else rawResolverName
            val isVideoUri = (context.contentResolver.getType(uri)?.startsWith("video") == true) || isVideoPath(nameFromResolver) || isVideoPath(cleanPath)
            val size = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } catch (_: Exception) { 0L }
            val sizeFormatted = size.formatBytes()

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
                } catch (_: Exception) { Pair(false, null) }

                try { retriever.release() } catch (_: Exception) {}

                val extractedDevice = try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        VideoDeviceParser.extractVideoDeviceInfo(pfd)
                    } ?: context.contentResolver.openInputStream(uri)?.use { VideoDeviceParser.extractVideoDeviceInfo(it) }
                } catch (_: Exception) { null }

                val cleanDevice = extractedDevice?.trim()?.takeIf { !it.matches(REGEX_NUMBERS_ONLY) && it.length >= 3 }

                return MediaExifInfo(
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
                    megapixels = null,
                    resolution = videoMeta.resolution,
                    fileSizeFormatted = sizeFormatted,
                    fileSizeBytes = size,
                    filePath = cleanPath,
                    isBackedUp = isBackedUp,
                    isJuneOptimized = false,
                    isVideo = true,
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
                } catch (_: Exception) { null }

                val photo = PhotoMetadataParser.parsePhotoExif(
                    exif = exif,
                    rawWidth = w,
                    rawHeight = h,
                    isScreenshot = nameFromResolver.contains("Screenshot", ignoreCase = true),
                    fallbackDateTime = fallbackDateTime
                )

                return MediaExifInfo(
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
                    isJuneOptimized = photo.isJuneOptimized,
                    isVideo = false,
                    isHdr = photo.isHdr
                )
            }
        }

        val file = MediaStorageManager.resolveMediaFile(context, cleanPath)
        val rawName = file.name
        val name = if (rawName.contains("_media_")) "media_" + rawName.substringAfterLast("_media_") else rawName
        val size = if (file.exists()) file.length() else 0L
        val sizeFormatted = size.formatBytes()
        val isVideoFile = isVideoPath(name) || isVideoPath(cleanPath)

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

            return MediaExifInfo(
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
                megapixels = null,
                resolution = videoMeta.resolution,
                fileSizeFormatted = sizeFormatted,
                fileSizeBytes = size,
                filePath = file.absolutePath,
                isBackedUp = isBackedUp,
                isJuneOptimized = false,
                isVideo = true,
                isHdr = isHdr,
                hdrFormat = hdrFormat
            )
        }

        if (!file.exists() || size == 0L) {
            val dateMillis = fallbackDateTime ?: System.currentTimeMillis()
            return MediaExifInfo(
                dateTaken = dateMillis.toExifDateTimeDisplay(),
                dateTakenMillis = dateMillis,
                journalTitle = journalTitle,
                journalMediaCount = journalMediaCount,
                deviceName = if (isVideoFile) "Video Details" else "Photo Details",
                fileName = name,
                fileSizeFormatted = sizeFormatted,
                fileSizeBytes = size,
                filePath = file.absolutePath,
                isBackedUp = isBackedUp,
                isVideo = isVideoFile
            )
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
            } catch (_: Exception) { null }
        }

        val photo = PhotoMetadataParser.parsePhotoExif(
            exif = exif,
            rawWidth = bounds.outWidth,
            rawHeight = bounds.outHeight,
            isScreenshot = name.contains("Screenshot", ignoreCase = true),
            fallbackDateTime = fallbackDateTime ?: file.lastModified()
        )

        return MediaExifInfo(
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
            isJuneOptimized = photo.isJuneOptimized,
            isVideo = false,
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
