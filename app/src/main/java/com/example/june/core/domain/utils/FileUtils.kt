package com.example.june.core.domain.utils

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

            val fileName = "media_${System.currentTimeMillis()}_${(0..999).random()}.$extension"
            val file = File(context.filesDir, fileName)

            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
}