package com.denser.june.core.media.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File

object SongMediaStorage {

    fun persistSongArt(context: Context, uri: Uri, maxDimension: Int = 1024, quality: Int = 80): String? {
        return try {
            val contentResolver = context.contentResolver
            val songArtDir = File(context.filesDir, "song_media/art").apply { if (!exists()) mkdirs() }
            val fileName = "art_${System.currentTimeMillis()}_${(0..999).random()}.jpg"
            val file = File(songArtDir, fileName)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }

            val maxEdge = maxOf(bounds.outWidth, bounds.outHeight)
            var sampleSize = 1
            while (maxEdge / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val rawBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }

            if (rawBitmap != null) {
                val orientation = try {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    } ?: ExifInterface.ORIENTATION_NORMAL
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

                file.outputStream().use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
                finalBitmap.recycle()
            } else {
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if (file.exists() && file.length() > 0L) file.absolutePath else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAudioDurationMs(path: String?): Long? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            dur?.takeIf { it > 0L }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    fun resolveSongMedia(context: Context, path: String?, subDir: String): File? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (file.exists() && file.length() > 0L) return file
        val candidate = File(File(context.filesDir, "song_media/$subDir"), file.name)
        return candidate.takeIf { it.exists() && it.length() > 0L }
    }
}
