package com.denser.june.core.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object FileUtils {

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
}

fun File.computeSHA256(): String = FileUtils.computeSHA256(this)

fun Long.formatBytes(): String {
    if (this <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return String.format(Locale.getDefault(), "%.1f %s", this / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}