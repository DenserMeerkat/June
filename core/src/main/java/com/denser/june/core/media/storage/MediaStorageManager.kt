package com.denser.june.core.media.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaStorageManager {

    fun persistMedia(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)

            val extension = when {
                mimeType?.startsWith("video") == true -> "mp4"
                else -> "jpg"
            }

            val inputStream = contentResolver.openInputStream(uri) ?: return null

            val mediaDir = File(context.filesDir, "media").apply { if (!exists()) mkdirs() }
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
        val activeFileNames = activePaths.map { File(it).name }.toSet()
        val mediaDir = File(context.filesDir, "media")
        if (mediaDir.exists()) {
            mediaDir.listFiles()?.forEach { file ->
                if (file.name !in activeFileNames) {
                    file.delete()
                }
            }
        }
        val legacyDir = File(context.filesDir, "journal_media")
        if (legacyDir.exists()) {
            legacyDir.listFiles()?.forEach { file ->
                if (file.name !in activeFileNames) {
                    file.delete()
                }
            }
            if (legacyDir.listFiles()?.isEmpty() == true) {
                legacyDir.delete()
            }
        }
    }

    fun migrateLegacyMediaStorage(context: Context) {
        try {
            val legacyDir = File(context.filesDir, "journal_media")
            if (!legacyDir.exists() || !legacyDir.isDirectory) return
            val targetDir = File(context.filesDir, "media").apply { if (!exists()) mkdirs() }
            legacyDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val destFile = File(targetDir, file.name)
                    if (destFile.exists() && destFile.length() > 0L) {
                        file.delete()
                    } else {
                        val renamed = file.renameTo(destFile)
                        if (!renamed) {
                            try {
                                file.copyTo(destFile, overwrite = true)
                                file.delete()
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            if (legacyDir.listFiles()?.isEmpty() == true) {
                legacyDir.delete()
            }
        } catch (_: Exception) {}
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

    fun resolveMediaFile(context: Context, path: String?): File {
        if (path.isNullOrBlank()) return File("")
        var decoded: String = path
        while (decoded.contains("%2F", ignoreCase = true) || decoded.contains("%3A", ignoreCase = true)) {
            val next = try { Uri.decode(decoded) } catch (_: Exception) { decoded } ?: decoded
            if (next == decoded) break
            decoded = next
        }
        val clean = if (decoded.startsWith("file://")) decoded.removePrefix("file://") else decoded
        val direct = File(clean)
        if (direct.exists() && direct.length() > 0L) return direct

        val fileName = direct.name.substringAfterLast('/')
        val internalMedia = File(File(context.filesDir, "media"), fileName)
        if (internalMedia.exists() && internalMedia.length() > 0L) return internalMedia

        val legacyMedia = File(File(context.filesDir, "journal_media"), fileName)
        if (legacyMedia.exists() && legacyMedia.length() > 0L) return legacyMedia

        return direct
    }

    fun getMediaDir(context: Context): File {
        return File(context.filesDir, "media").apply {
            if (!exists()) mkdirs()
        }
    }

    fun saveImportedStream(
        context: Context,
        inputStream: InputStream,
        source: String,
        originalFileName: String
    ): String {
        val mediaDir = getMediaDir(context)
        val ext = originalFileName.substringAfterLast('.', "jpg")
        val safeBase = originalFileName.substringBeforeLast('.')
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(40)
        val safeSource = source.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "import_${safeSource}_${safeBase}.$ext"
        val targetFile = File(mediaDir, fileName)
        if (targetFile.exists() && targetFile.length() > 0) {
            val buf = ByteArray(8192)
            while (inputStream.read(buf) > 0) {}
            return targetFile.absolutePath
        }
        FileOutputStream(targetFile).use { fos ->
            inputStream.copyTo(fos)
        }
        return targetFile.absolutePath
    }

    fun saveImportedBytes(
        context: Context,
        bytes: ByteArray,
        source: String,
        identifier: String,
        extension: String = "jpg"
    ): String {
        val mediaDir = getMediaDir(context)
        val safeId = identifier.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
        val safeSource = source.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val fileName = "import_${safeSource}_${safeId}.$extension"
        val targetFile = File(mediaDir, fileName)
        if (targetFile.exists() && targetFile.length() == bytes.size.toLong()) {
            return targetFile.absolutePath
        }
        FileOutputStream(targetFile).use { it.write(bytes) }
        return targetFile.absolutePath
    }
}
