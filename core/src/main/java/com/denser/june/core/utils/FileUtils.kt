package com.denser.june.core.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    fun persistMedia(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)

            val extension = when {
                mimeType?.startsWith("video") == true -> "mp4"
                else -> "jpg"
            }

            val inputStream = contentResolver.openInputStream(uri) ?: return null

            val mediaDir = File(context.filesDir, "journal_media").apply { if (!exists()) mkdirs() }
            val fileName = "media_${System.currentTimeMillis()}_${(0..999).random()}.$extension"
            val file = File(mediaDir, fileName)

            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun persistSongArt(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            val songArtDir = File(context.filesDir, "song_media/art").apply { if (!exists()) mkdirs() }
            val fileName = "art_${System.currentTimeMillis()}_${(0..999).random()}.jpg"
            val file = File(songArtDir, fileName)

            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteMedia(path: String?): Boolean {
        if (path == null) return false
        return try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun cleanOrphanedFiles(context: Context, activePaths: List<String>) {
        val mediaDir = File(context.filesDir, "journal_media")
        if (!mediaDir.exists()) return

        val activeFileNames = activePaths.map { File(it).name }.toSet()

        mediaDir.listFiles()?.forEach { file ->
            if (file.name !in activeFileNames) {
                file.delete()
            }
        }
    }

    fun createTempPictureUri(context: Context): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            context.externalCacheDir
        )

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            image
        )
    }

    fun createTempVideoUri(context: Context): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val videoFileName = "MP4_${timeStamp}_"

        val video = File.createTempFile(
            videoFileName,
            ".mp4",
            context.externalCacheDir
        )

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            video
        )
    }

    fun computeSHA256(file: File): String {
        if (!file.exists() || file.length() == 0L) return ""
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var read: Int
            while (stream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun getDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (colIdx != -1) {
                            return cursor.getString(colIdx)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        return uri.lastPathSegment?.let { File(it).name }
    }

    fun getAudioDurationMs(path: String?): Long? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return null
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val dur = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
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

fun File.computeSHA256(): String = FileUtils.computeSHA256(this)