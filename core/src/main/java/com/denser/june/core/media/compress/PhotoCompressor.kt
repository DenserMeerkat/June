package com.denser.june.core.media.compress

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.denser.june.core.media.compress.model.CompressionPreset
import com.denser.june.core.media.compress.model.CompressionPreviewResult
import com.denser.june.core.media.storage.MediaStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PhotoCompressor {

    fun shouldCompressPhoto(file: File, maxDimension: Int = 2048): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val fileSize = file.length()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        if (maxSide <= 0) return false
        if (maxSide <= 1080 && fileSize <= 350 * 1024L) return false
        val totalPixels = (bounds.outWidth.toLong() * bounds.outHeight.toLong()).coerceAtLeast(1L)
        val bpp = fileSize.toFloat() / totalPixels.toFloat()
        if (bpp <= 0.30f && maxSide <= maxDimension) return false
        return maxSide > maxDimension || (fileSize > 800 * 1024L && bpp > 0.35f)
    }

    suspend fun generatePhotoPreview(
        context: Context,
        sourcePath: String,
        preset: CompressionPreset
    ): CompressionPreviewResult = withContext(Dispatchers.IO) {
        val targetOriginalFile = MediaStorageManager.resolveMediaFile(context, sourcePath)
        val origFile = CompressUtils.getUncompressedOriginalFile(context, sourcePath)
        val sourceFile = origFile ?: targetOriginalFile
        val origLength = (origFile ?: targetOriginalFile).length()
        if (preset == CompressionPreset.ORIGINAL || !sourceFile.exists() || origLength == 0L) {
            return@withContext CompressionPreviewResult(
                preset = CompressionPreset.ORIGINAL,
                previewPath = sourceFile.absolutePath,
                originalSizeBytes = origLength,
                compressedSizeBytes = origLength,
                savingsPercent = 0,
                isAlreadyOptimal = origFile == null
            )
        }

        val (maxDimension, quality) = when (preset) {
            CompressionPreset.LOW -> 1080 to 68
            CompressionPreset.MEDIUM -> 1440 to 76
            CompressionPreset.HIGH -> 2048 to 84
            CompressionPreset.ORIGINAL -> Int.MAX_VALUE to 100
        }

        val pathHash = Integer.toHexString(sourceFile.absolutePath.hashCode())
        val cacheFile = File(context.cacheDir, "preview_${preset.name.lowercase()}_${pathHash}_${sourceFile.nameWithoutExtension}.jpg")
        if (cacheFile.exists() && cacheFile.length() > 0L) {
            val cachedLength = cacheFile.length()
            val savings = if (origLength > 0L) {
                (((origLength - cachedLength).toFloat() / origLength.toFloat()) * 100f).toInt()
            } else 0
            val isOptimal = cachedLength >= (origLength * 0.85f).toLong() || savings <= 0
            return@withContext CompressionPreviewResult(
                preset = preset,
                previewPath = if (isOptimal) sourceFile.absolutePath else cacheFile.absolutePath,
                originalSizeBytes = origLength,
                compressedSizeBytes = if (isOptimal) origLength else cachedLength,
                savingsPercent = if (isOptimal) 0 else savings,
                isAlreadyOptimal = isOptimal
            )
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)

        if (maxSide <= maxDimension) {
            val totalPixels = (bounds.outWidth.toLong() * bounds.outHeight.toLong()).coerceAtLeast(1L)
            val bpp = origLength.toFloat() / totalPixels.toFloat()
            if (bpp <= 0.30f && origLength <= 400 * 1024L) {
                return@withContext CompressionPreviewResult(
                    preset = preset,
                    previewPath = sourceFile.absolutePath,
                    originalSizeBytes = origLength,
                    compressedSizeBytes = origLength,
                    savingsPercent = 0,
                    isAlreadyOptimal = true
                )
            }
        }

        var sampleSize = 1
        while (maxSide / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val rawBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOpts)
            ?: return@withContext CompressionPreviewResult(
                preset = preset,
                previewPath = sourceFile.absolutePath,
                originalSizeBytes = origLength,
                compressedSizeBytes = origLength,
                savingsPercent = 0,
                isAlreadyOptimal = true
            )

        val orientation = try {
            ExifInterface(sourceFile.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }

        val rotatedBitmap = if (!matrix.isIdentity) {
            val rb = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (rb != rawBitmap) rawBitmap.recycle()
            rb
        } else {
            rawBitmap
        }

        val currentMax = maxOf(rotatedBitmap.width, rotatedBitmap.height)
        val finalBitmap = if (currentMax > maxDimension) {
            val scale = maxDimension.toFloat() / currentMax
            val scaled = Bitmap.createScaledBitmap(
                rotatedBitmap,
                (rotatedBitmap.width * scale).toInt().coerceAtLeast(1),
                (rotatedBitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (scaled != rotatedBitmap) rotatedBitmap.recycle()
            scaled
        } else {
            rotatedBitmap
        }

        cacheFile.outputStream().use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        finalBitmap.recycle()

        copyExifAttributes(sourceFile, cacheFile, preset)

        val newLength = cacheFile.length()
        val savings = if (origLength > 0L) {
            (((origLength - newLength).toFloat() / origLength.toFloat()) * 100f).toInt()
        } else 0

        val isOptimal = newLength >= (origLength * 0.85f).toLong() || savings <= 0

        CompressionPreviewResult(
            preset = preset,
            previewPath = if (isOptimal) sourceFile.absolutePath else cacheFile.absolutePath,
            originalSizeBytes = origLength,
            compressedSizeBytes = if (isOptimal) origLength else newLength,
            savingsPercent = if (isOptimal) 0 else savings,
            isAlreadyOptimal = isOptimal
        )
    }

    suspend fun compressPhoto(
        context: Context,
        path: String,
        preset: CompressionPreset
    ): Boolean = withContext(Dispatchers.IO) {
        val file = MediaStorageManager.resolveMediaFile(context, path)
        if (!file.exists() || file.length() == 0L) return@withContext false
        val origFile = CompressUtils.getUncompressedOriginalFile(context, path)
        if (preset == CompressionPreset.ORIGINAL) {
            if (origFile != null && origFile.exists()) {
                val origTimestamp = origFile.lastModified()
                val ok = origFile.copyTo(file, overwrite = true).exists()
                if (origTimestamp > 0L) {
                    file.setLastModified(origTimestamp)
                }
                return@withContext ok
            }
            return@withContext false
        }

        if (origFile == null) {
            CompressUtils.saveUncompressedOriginalFile(context, path)
        }

        val preview = generatePhotoPreview(context, path, preset)
        if (preview.isAlreadyOptimal || preview.previewPath == file.absolutePath) {
            return@withContext true
        }

        val previewFile = File(preview.previewPath)
        val success = if (previewFile.exists() && previewFile.length() > 0L) {
            CompressUtils.applyCompressedFile(previewFile, file)
        } else {
            false
        }
        success
    }

    private fun copyExifAttributes(sourceFile: File, destFile: File, preset: CompressionPreset) {
        try {
            if (!sourceFile.exists() || !destFile.exists()) return
            val srcExif = ExifInterface(sourceFile.absolutePath)
            val dstExif = ExifInterface(destFile.absolutePath)

            val tags = arrayOf(
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_OFFSET_TIME,
                ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
                ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
                ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_WHITE_BALANCE,
                ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
                ExifInterface.TAG_METERING_MODE,
                ExifInterface.TAG_LENS_MAKE,
                ExifInterface.TAG_LENS_MODEL,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_PROCESSING_METHOD
            )

            for (tag in tags) {
                val value = srcExif.getAttribute(tag)
                if (value != null) {
                    dstExif.setAttribute(tag, value)
                }
            }
            dstExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            dstExif.saveAttributes()
        } catch (_: Exception) {}
    }
}
