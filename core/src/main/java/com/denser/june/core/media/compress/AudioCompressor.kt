package com.denser.june.core.media.compress

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.*
import com.denser.june.core.media.compress.model.CompressionPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

object AudioCompressor {

    fun getAudioTargetBitrate(preset: CompressionPreset): Int {
        return when (preset) {
            CompressionPreset.LOW -> 64_000
            CompressionPreset.MEDIUM -> 128_000
            CompressionPreset.HIGH -> 192_000
            CompressionPreset.ORIGINAL -> 0
        }
    }

    fun getBitrateForQuality(qualityKbps: Int): Int {
        return when {
            qualityKbps <= 64 -> 64_000
            qualityKbps <= 128 -> 128_000
            else -> 192_000
        }
    }

    fun shouldCompressAudio(file: File, targetBitrate: Int): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val ext = file.extension.lowercase()
        if (ext in listOf("wav", "flac", "aiff", "alac")) return true

        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
            retriever.release()

            if (mime.contains("wav") || mime.contains("flac") || mime.contains("raw")) return true
            if (bitrate > 0 && bitrate > targetBitrate + 16_000) return true
            file.length() > 10 * 1024 * 1024L
        } catch (_: Exception) {
            false
        }
    }

    suspend fun compressAudio(
        context: Context,
        inputFile: File,
        outputFile: File,
        targetBitrate: Int
    ): Boolean = withContext(Dispatchers.IO) {
        if (!inputFile.exists() || inputFile.length() == 0L) return@withContext false

        try {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(inputFile))
            val audioEncoderSettings = AudioEncoderSettings.Builder()
                .setBitrate(targetBitrate)
                .build()
            val encoderFactory = DefaultEncoderFactory.Builder(context)
                .setRequestedAudioEncoderSettings(audioEncoderSettings)
                .build()

            val success = suspendCancellableCoroutine { continuation ->
                var finished = false
                val transformer = Transformer.Builder(context)
                    .setEncoderFactory(encoderFactory)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(comp: Composition, exportResult: ExportResult) {
                            if (!finished) {
                                finished = true
                                if (continuation.isActive) continuation.resume(true)
                            }
                        }

                        override fun onError(
                            comp: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            if (!finished) {
                                finished = true
                                if (continuation.isActive) continuation.resume(false)
                            }
                        }
                    })
                    .build()

                continuation.invokeOnCancellation {
                    transformer.cancel()
                }

                Handler(Looper.getMainLooper()).post {
                    try {
                        transformer.start(mediaItem, outputFile.absolutePath)
                    } catch (_: Exception) {
                        if (!finished) {
                            finished = true
                            if (continuation.isActive) continuation.resume(false)
                        }
                    }
                }
            }

            if (success && outputFile.exists() && outputFile.length() > 0L) {
                true
            } else {
                if (outputFile.exists()) outputFile.delete()
                false
            }
        } catch (_: Exception) {
            if (outputFile.exists()) outputFile.delete()
            false
        }
    }
}
