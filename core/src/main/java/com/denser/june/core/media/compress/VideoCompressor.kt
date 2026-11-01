package com.denser.june.core.media.compress

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.*
import com.denser.june.core.media.compress.model.CompressionPreset
import com.denser.june.core.media.compress.model.CompressionPreviewResult
import com.denser.june.core.media.storage.MediaStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

object VideoCompressor {

    internal fun getRotatedShortEdge(retriever: MediaMetadataRetriever): Int {
        val origHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val origWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        val rotatedHeight = if (rotation == 90 || rotation == 270) origWidth else origHeight
        val rotatedWidth = if (rotation == 90 || rotation == 270) origHeight else origWidth
        return minOf(rotatedHeight, rotatedWidth).takeIf { it > 0 } ?: rotatedHeight
    }

    internal fun File.readRotatedShortEdge(): Int {
        if (!exists() || length() == 0L) return 0
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(absolutePath)
            val edge = getRotatedShortEdge(retriever)
            retriever.release()
            edge
        } catch (_: Exception) {
            0
        }
    }

    fun getVideoTargetSpecs(sourceHeight: Int, preset: CompressionPreset): Pair<Int, Int> {
        val isHighRes = sourceHeight > 1080
        return when (preset) {
            CompressionPreset.LOW -> if (isHighRes) 720 to 2_000_000 else 480 to 1_000_000
            CompressionPreset.MEDIUM -> if (isHighRes) 1080 to 4_000_000 else 720 to 2_000_000
            CompressionPreset.HIGH -> if (isHighRes) 1440 to 8_000_000 else 1080 to 3_800_000
            CompressionPreset.ORIGINAL -> 0 to 0
        }
    }

    fun shouldCompressVideo(file: File, targetHeight: Int = 1080): Boolean {
        if (!file.exists() || file.length() <= 5 * 1024 * 1024L) return false
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val actualHeight = getRotatedShortEdge(retriever)
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            retriever.release()
            if (actualHeight <= 480) return false
            val maxTargetBitrate = when {
                targetHeight <= 480 -> 1_200_000
                targetHeight <= 720 -> 2_500_000
                else -> 4_200_000
            }
            if (actualHeight <= targetHeight && bitrate in 1..maxTargetBitrate && file.length() <= 20 * 1024 * 1024L) {
                return false
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    @UnstableApi
    suspend fun estimateVideoCompression(
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

        val retriever = MediaMetadataRetriever()
        var durationMs = 0L
        var sourceBitrate = 0
        var actualHeight = 0

        try {
            retriever.setDataSource(sourceFile.absolutePath)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            sourceBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            actualHeight = getRotatedShortEdge(retriever)
        } catch (_: Exception) {
        } finally {
            retriever.release()
        }

        val (targetHeight, targetBitrate) = getVideoTargetSpecs(actualHeight, preset)
        if (targetHeight <= 0 || targetBitrate <= 0) {
            return@withContext CompressionPreviewResult(
                preset = preset,
                previewPath = sourceFile.absolutePath,
                originalSizeBytes = origLength,
                compressedSizeBytes = origLength,
                savingsPercent = 0,
                isAlreadyOptimal = true
            )
        }

        if (actualHeight in 1..targetHeight && sourceBitrate in 1..targetBitrate) {
            return@withContext CompressionPreviewResult(
                preset = preset,
                previewPath = sourceFile.absolutePath,
                originalSizeBytes = origLength,
                compressedSizeBytes = origLength,
                savingsPercent = 0,
                isAlreadyOptimal = true
            )
        }

        val audioBitrate = 128_000
        val totalTargetBitrate = targetBitrate + audioBitrate
        val durationSeconds = (durationMs / 1000f).coerceAtLeast(1f)
        val estimatedBytes = ((totalTargetBitrate * durationSeconds) / 8).toLong()

        val newLength = minOf(estimatedBytes, origLength)
        val savings = if (origLength > 0L) {
            (((origLength - newLength).toFloat() / origLength.toFloat()) * 100f).toInt()
        } else 0

        val isOptimal = newLength >= (origLength * 0.85f).toLong() || savings <= 0

        val pathHash = Integer.toHexString(sourceFile.absolutePath.hashCode())
        val cacheFile = File(context.cacheDir, "preview_video_${preset.name.lowercase()}_${pathHash}_${sourceFile.nameWithoutExtension}.mp4")
        val previewPath = if (!isOptimal) {
            if (cacheFile.exists() && cacheFile.length() > 0L) {
                cacheFile.absolutePath
            } else {
                val clipEndMs = minOf(durationMs, 15000L).coerceAtLeast(1000L)
                val ok = transcodeVideo(context, sourceFile, cacheFile, targetHeight, targetBitrate, clipEndMs)
                if (ok) cacheFile.absolutePath else sourceFile.absolutePath
            }
        } else {
            sourceFile.absolutePath
        }

        val isClipped = durationMs > 15000L

        CompressionPreviewResult(
            preset = preset,
            previewPath = previewPath,
            originalSizeBytes = origLength,
            compressedSizeBytes = if (isOptimal) origLength else newLength,
            savingsPercent = if (isOptimal) 0 else savings,
            isAlreadyOptimal = isOptimal,
            isClipped = isClipped && !isOptimal
        )
    }

    @UnstableApi
    suspend fun compressVideo(
        context: Context,
        path: String,
        preset: CompressionPreset
    ): Boolean = withContext(Dispatchers.IO) {
        val targetFile = MediaStorageManager.resolveMediaFile(context, path)
        if (!targetFile.exists() || targetFile.length() == 0L) return@withContext false
        val origFile = CompressUtils.getUncompressedOriginalFile(context, path)
        if (preset == CompressionPreset.ORIGINAL) {
            if (origFile != null && origFile.exists()) {
                val origTimestamp = targetFile.lastModified()
                val ok = origFile.copyTo(targetFile, overwrite = true).exists()
                if (origTimestamp > 0L) {
                    targetFile.setLastModified(origTimestamp)
                }
                return@withContext ok
            }
            return@withContext false
        }

        if (origFile == null) {
            CompressUtils.saveUncompressedOriginalFile(context, path)
        }

        val inputFile = origFile ?: targetFile
        val actualHeight = inputFile.readRotatedShortEdge()

        val (targetHeight, targetBitrate) = getVideoTargetSpecs(actualHeight, preset)
        if (targetHeight <= 0 || targetBitrate <= 0) return@withContext false

        val tempOutputFile = File(context.cacheDir, "comp_${System.currentTimeMillis()}_${targetFile.name}")
        val success = transcodeVideo(context, inputFile, tempOutputFile, targetHeight, targetBitrate, clipEndMs = null)

        if (success && tempOutputFile.exists() && tempOutputFile.length() > 0L) {
            if (tempOutputFile.length() < targetFile.length() || origFile != null) {
                val origTimestamp = targetFile.lastModified()
                tempOutputFile.copyTo(targetFile, overwrite = true)
                if (origTimestamp > 0L) {
                    targetFile.setLastModified(origTimestamp)
                }
                tempOutputFile.delete()
                true
            } else {
                tempOutputFile.delete()
                false
            }
        } else {
            tempOutputFile.delete()
            false
        }
    }

    @UnstableApi
    private suspend fun transcodeVideo(
        context: Context,
        inputFile: File,
        outputFile: File,
        targetHeight: Int,
        targetBitrate: Int,
        clipEndMs: Long? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!inputFile.exists() || inputFile.length() == 0L) return@withContext false

        val actualHeight = inputFile.readRotatedShortEdge()
        if (actualHeight <= 0) return@withContext false
        val effectiveTargetHeight = if (actualHeight in 1..targetHeight) actualHeight else targetHeight

        try {
            val presentationEffect = Presentation.createForHeight(effectiveTargetHeight)
            val effects = Effects(emptyList(), listOf(presentationEffect))

            val mediaItem = if (clipEndMs != null && clipEndMs > 0L) {
                val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                    .setEndPositionMs(clipEndMs)
                    .build()
                MediaItem.Builder()
                    .setUri(Uri.fromFile(inputFile))
                    .setClippingConfiguration(clippingConfig)
                    .build()
            } else {
                MediaItem.fromUri(Uri.fromFile(inputFile))
            }

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(effects)
                .build()

            @Suppress("DEPRECATION")
            val sequence = EditedMediaItemSequence.Builder(editedMediaItem).build()
            val composition = Composition.Builder(sequence)
                .setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL)
                .build()

            val videoEncoderSettings = VideoEncoderSettings.Builder()
                .setBitrate(targetBitrate)
                .build()
            val encoderFactory = DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(videoEncoderSettings)
                .build()

            val success = suspendCancellableCoroutine { continuation ->
                var finished = false
                val transformer = Transformer.Builder(context)
                    .setEncoderFactory(encoderFactory)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
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
                        transformer.start(composition, outputFile.absolutePath)
                    } catch (_: Exception) {
                        if (!finished) {
                            finished = true
                            if (continuation.isActive) continuation.resume(false)
                        }
                    }
                }
            }

            success && outputFile.exists() && outputFile.length() > 0L
        } catch (_: Exception) {
            outputFile.delete()
            false
        }
    }
}
