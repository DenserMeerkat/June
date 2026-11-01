package com.denser.june.core.media.compress

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.denser.june.core.media.compress.model.CompressionPreset
import com.denser.june.core.media.compress.model.CompressionPreviewResult
import com.denser.june.core.media.exif.ExifUtils
import com.denser.june.core.media.storage.MediaStorageManager
import java.io.File

object CompressUtils {

    fun getUncompressedOriginalFile(context: Context, path: String): File? {
        val file = MediaStorageManager.resolveMediaFile(context, path)
        val tempOrig = File(context.cacheDir, "uncompressed_${file.name}")
        return if (tempOrig.exists() && tempOrig.length() > 0L) tempOrig else null
    }

    fun saveUncompressedOriginalFile(context: Context, path: String): File? {
        val file = MediaStorageManager.resolveMediaFile(context, path)
        if (!file.exists() || file.length() == 0L) return null
        val tempOrig = File(context.cacheDir, "uncompressed_${file.name}")
        if (!tempOrig.exists()) {
            try {
                file.copyTo(tempOrig, overwrite = true)
            } catch (_: Exception) {
                return null
            }
        }
        return if (tempOrig.exists() && tempOrig.length() > 0L) tempOrig else null
    }

    fun restoreUncompressedOriginalFile(context: Context, path: String): Boolean {
        val file = MediaStorageManager.resolveMediaFile(context, path)
        val tempOrig = File(context.cacheDir, "uncompressed_${file.name}")
        val success = if (tempOrig.exists() && tempOrig.length() > 0L) {
            try {
                val origTimestamp = tempOrig.lastModified()
                val copied = tempOrig.copyTo(file, overwrite = true)
                if (origTimestamp > 0L) {
                    file.setLastModified(origTimestamp)
                }
                tempOrig.delete()
                copied.exists()
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }
        cleanUncompressedOriginalFile(context, path)
        return success
    }

    fun cleanUncompressedOriginalFile(context: Context, path: String) {
        val file = MediaStorageManager.resolveMediaFile(context, path)
        val tempOrig = File(context.cacheDir, "uncompressed_${file.name}")
        if (tempOrig.exists()) {
            tempOrig.delete()
        }
        val legacyMarker = File(context.cacheDir, "preset_${file.name}")
        if (legacyMarker.exists()) {
            legacyMarker.delete()
        }
        val baseName = file.nameWithoutExtension
        context.cacheDir.listFiles()?.forEach { cached ->
            if (cached.name.startsWith("preview_") && cached.name.contains(baseName)) {
                cached.delete()
            }
        }
    }

    fun inferPhotoPreset(width: Int, height: Int, fileSize: Long): CompressionPreset? {
        if (width <= 0 || height <= 0 || fileSize <= 0L) return null
        val maxEdge = maxOf(width, height)
        if (maxEdge > 2048) return null
        val totalPixels = (width.toLong() * height.toLong()).coerceAtLeast(1L)
        val bpp = fileSize.toFloat() / totalPixels.toFloat()

        return when {
            maxEdge <= 1080 && (bpp <= 0.35f || fileSize <= 450 * 1024L) -> CompressionPreset.LOW
            maxEdge <= 1440 && (bpp <= 0.40f || fileSize <= 900 * 1024L) -> CompressionPreset.MEDIUM
            maxEdge <= 2048 && (bpp <= 0.48f || fileSize <= 1_600_000L) -> CompressionPreset.HIGH
            else -> null
        }
    }

    fun inferVideoPreset(shortEdge: Int, bitrate: Int): CompressionPreset? {
        if (shortEdge <= 0) return null
        if (shortEdge > 1440) return null
        val hasBitrate = bitrate > 0

        return when {
            shortEdge <= 480 && (!hasBitrate || bitrate <= 2_000_000) -> CompressionPreset.LOW
            shortEdge <= 720 && hasBitrate && bitrate <= 1_800_000 -> CompressionPreset.LOW
            shortEdge <= 720 && (!hasBitrate || bitrate <= 4_000_000) -> CompressionPreset.MEDIUM
            shortEdge <= 1080 && hasBitrate && bitrate <= 3_500_000 -> CompressionPreset.MEDIUM
            shortEdge <= 1080 && (!hasBitrate || bitrate <= 6_500_000) -> CompressionPreset.HIGH
            shortEdge <= 1440 && hasBitrate && bitrate <= 8_500_000 -> CompressionPreset.HIGH
            else -> null
        }
    }

    fun inferAppliedPreset(file: File): CompressionPreset? {
        if (!file.exists() || file.length() == 0L) return null
        return if (ExifUtils.isVideoPath(file.name)) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                val shortEdge = VideoCompressor.getRotatedShortEdge(retriever)
                val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
                inferVideoPreset(shortEdge, bitrate)
            } catch (_: Exception) {
                null
            } finally {
                retriever.release()
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            inferPhotoPreset(bounds.outWidth, bounds.outHeight, file.length())
        }
    }

    fun getAppliedPreset(context: Context, path: String): CompressionPreset? {
        return inferAppliedPreset(MediaStorageManager.resolveMediaFile(context, path))
    }


    fun applyCompressedFile(previewFile: File, targetOriginalFile: File): Boolean {
        if (!previewFile.exists() || previewFile.length() == 0L) return false
        return try {
            val origTimestamp = targetOriginalFile.lastModified()
            val copied = previewFile.copyTo(targetOriginalFile, overwrite = true)
            if (origTimestamp > 0L) {
                targetOriginalFile.setLastModified(origTimestamp)
            }
            copied.exists() && copied.length() > 0L
        } catch (_: Exception) {
            false
        }
    }

    fun cleanupPreviewCache(cacheMap: Map<CompressionPreset, CompressionPreviewResult>, excludeFile: File? = null) {
        cacheMap.values.forEach { res ->
            if (res.previewPath.isNotBlank()) {
                val f = File(res.previewPath)
                if (f.exists() && f != excludeFile && f.name.startsWith("preview_")) {
                    f.delete()
                }
            }
        }
    }

    fun getAvailablePresets(context: Context, path: String): Set<CompressionPreset> {
        val origFile = getUncompressedOriginalFile(context, path)
        val targetFile = origFile ?: MediaStorageManager.resolveMediaFile(context, path)
        val presets = getAvailablePresets(targetFile)
        return if (origFile != null) {
            presets + CompressionPreset.ORIGINAL
        } else {
            presets
        }
    }

    fun getAvailablePresets(file: File): Set<CompressionPreset> {
        val result = mutableSetOf(CompressionPreset.ORIGINAL)
        if (!file.exists() || file.length() == 0L) return result

        val isVideo = ExifUtils.isVideoPath(file.name)
        if (isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                val shortEdge = VideoCompressor.getRotatedShortEdge(retriever)
                val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0

                if (shortEdge > 480 || bitrate > 1_200_000) result.add(CompressionPreset.LOW)
                if (shortEdge > 720 || bitrate > 2_500_000) result.add(CompressionPreset.MEDIUM)
                if (shortEdge > 1080 || bitrate > 4_500_000) result.add(CompressionPreset.HIGH)
            } catch (_: Exception) {
            } finally {
                retriever.release()
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val maxEdge = maxOf(bounds.outWidth, bounds.outHeight)
            val fileSize = file.length()
            val totalPixels = (bounds.outWidth.toLong() * bounds.outHeight.toLong()).coerceAtLeast(1L)
            val bpp = fileSize.toFloat() / totalPixels.toFloat()

            if (maxEdge > 1080 || (bpp > 0.32f && fileSize > 300 * 1024L)) {
                result.add(CompressionPreset.LOW)
            }
            if (maxEdge > 1440 || (bpp > 0.38f && fileSize > 600 * 1024L)) {
                result.add(CompressionPreset.MEDIUM)
            }
            if (maxEdge > 2048 || (bpp > 0.45f && fileSize > 1_200_000L)) {
                result.add(CompressionPreset.HIGH)
            }
        }
        return result
    }

    fun shouldCompressMedia(
        context: Context,
        path: String,
        targetVideoHeight: Int = 1080,
        maxPhotoDimension: Int = 2048
    ): Boolean {
        val file = MediaStorageManager.resolveMediaFile(context, path)
        if (!file.exists() || file.length() == 0L) return false
        return if (ExifUtils.isVideoPath(file.name) || ExifUtils.isVideoPath(path)) {
            VideoCompressor.shouldCompressVideo(file, targetVideoHeight)
        } else {
            PhotoCompressor.shouldCompressPhoto(file, maxPhotoDimension)
        }
    }

    suspend fun generatePhotoPreview(
        context: Context,
        sourcePath: String,
        preset: CompressionPreset
    ): CompressionPreviewResult = PhotoCompressor.generatePhotoPreview(context, sourcePath, preset)

    suspend fun estimateVideoCompression(
        context: Context,
        sourcePath: String,
        preset: CompressionPreset
    ): CompressionPreviewResult = VideoCompressor.estimateVideoCompression(context, sourcePath, preset)

    suspend fun compressPhoto(
        context: Context,
        path: String,
        preset: CompressionPreset
    ): Boolean = PhotoCompressor.compressPhoto(context, path, preset)

    suspend fun compressVideo(
        context: Context,
        path: String,
        preset: CompressionPreset
    ): Boolean = VideoCompressor.compressVideo(context, path, preset)
}
