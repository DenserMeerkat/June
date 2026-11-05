package com.denser.june.core.media.storage

import android.content.Context
import android.media.*
import android.net.Uri
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.media.compress.AudioCompressor
import com.denser.june.core.media.exif.AudioMetadataParser
import com.denser.june.core.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object AudioStorageManager {

    fun getAudioDir(context: Context): File {
        return File(context.filesDir, "journal_audio").apply {
            if (!exists()) mkdirs()
        }
    }

    fun createTempAudioFile(context: Context, extension: String = "m4a"): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(java.util.Date())
        val fileName = "REC_${timeStamp}_"
        return File.createTempFile(fileName, ".$extension", context.cacheDir)
    }

    fun resolveAudioFile(context: Context, path: String?): File {
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

        val fileName = direct.name.substringAfterLast('/').substringAfterLast('\\')
        val internalAudio = File(getAudioDir(context), fileName)
        if (internalAudio.exists() && internalAudio.length() > 0L) return internalAudio

        val inFilesDir = File(context.filesDir, fileName)
        if (inFilesDir.exists() && inFilesDir.length() > 0L) return inFilesDir

        val inCacheDir = File(context.cacheDir, fileName)
        if (inCacheDir.exists() && inCacheDir.length() > 0L) return inCacheDir

        return direct
    }

    suspend fun persistAudio(
        context: Context,
        sourceUri: Uri,
        title: String? = null,
        waveform: List<Float> = emptyList(),
        targetBitrate: Int? = null,
        existingId: String? = null,
        fallbackDateTime: Long? = null
    ): JournalAudio? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(sourceUri)
            val displayName = FileUtils.getDisplayName(context, sourceUri)
            val extension = when {
                displayName?.endsWith(".m4a", ignoreCase = true) == true || mimeType?.contains("m4a") == true -> "m4a"
                displayName?.endsWith(".mp3", ignoreCase = true) == true || mimeType?.contains("mp3") == true -> "mp3"
                displayName?.endsWith(".aac", ignoreCase = true) == true || mimeType?.contains("aac") == true -> "aac"
                displayName?.endsWith(".wav", ignoreCase = true) == true || mimeType?.contains("wav") == true -> "wav"
                else -> "m4a"
            }
            val audioDir = getAudioDir(context)
            val fileName = "audio_${System.currentTimeMillis()}_${(0..999).random()}.$extension"
            val destFile = File(audioDir, fileName)

            contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            var finalFile = destFile
            if (targetBitrate != null && AudioCompressor.shouldCompressAudio(destFile, targetBitrate)) {
                val compressedFile = File(audioDir, "audio_${System.currentTimeMillis()}_${(0..999).random()}.m4a")
                val ok = AudioCompressor.compressAudio(context, destFile, compressedFile, targetBitrate)
                if (ok && compressedFile.exists() && compressedFile.length() > 0L) {
                    destFile.delete()
                    finalFile = compressedFile
                }
            }

            val originalTimestamp = fallbackDateTime?.takeIf { it > 946684800000L }
                ?: FileUtils.getDocumentLastModified(context, sourceUri)
            val fileSize = finalFile.length()
            val metadata = extractMetadata(context, finalFile, originalTimestamp)
            val resolvedTitle = title
                ?: metadata.title
                ?: displayName?.substringBeforeLast('.')
            val extractedWaveform = if (waveform.isNotEmpty()) waveform else extractWaveform(finalFile, 80)

            JournalAudio(
                id = existingId ?: UUID.randomUUID().toString(),
                path = finalFile.absolutePath,
                durationMs = metadata.durationMs,
                title = resolvedTitle,
                format = finalFile.extension.ifBlank { "m4a" },
                size = fileSize,
                waveform = extractedWaveform,
                createdAt = metadata.createdAt ?: originalTimestamp ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveRecordedAudio(
        context: Context,
        tempFile: File,
        title: String? = null,
        waveform: List<Float> = emptyList()
    ): JournalAudio? {
        return try {
            if (!tempFile.exists() || tempFile.length() == 0L) return null
            val audioDir = getAudioDir(context)
            val extension = tempFile.extension.ifBlank { "m4a" }
            val fileName = "audio_${System.currentTimeMillis()}_${(0..999).random()}.$extension"
            val destFile = File(audioDir, fileName)

            tempFile.copyTo(destFile, overwrite = true)
            tempFile.delete()

            val fileSize = destFile.length()
            val metadata = extractMetadata(context, destFile)
            val resolvedTitle = title ?: metadata.title

            JournalAudio(
                id = UUID.randomUUID().toString(),
                path = destFile.absolutePath,
                durationMs = metadata.durationMs,
                title = resolvedTitle,
                format = extension,
                size = fileSize,
                waveform = waveform,
                createdAt = metadata.createdAt ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteAudio(path: String?): Boolean {
        if (path == null) return false
        return try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun cleanOrphanedAudioFiles(context: Context, activePaths: List<String>) {
        val audioDir = getAudioDir(context)
        if (!audioDir.exists()) return

        val activeFileNames = activePaths.map { File(it).name }.toSet()

        audioDir.listFiles()?.forEach { file ->
            if (file.name !in activeFileNames) {
                file.delete()
            }
        }
    }

    private fun extractMetadata(context: Context, file: File, fallbackDateTime: Long? = null): ExtractedAudioMeta {
        val meta = AudioMetadataParser.parseAudioMetadata(context, file, fallbackDateTime)
        return ExtractedAudioMeta(
            durationMs = meta.durationMs,
            createdAt = meta.dateTakenMillis,
            title = meta.title,
            waveform = emptyList()
        )
    }

    fun extractWaveform(file: File, targetSamples: Int = 80): List<Float> {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            var audioTrack = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    format = trackFormat
                    break
                }
            }
            if (audioTrack >= 0 && format != null) {
                extractor.selectTrack(audioTrack)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                codec = MediaCodec.createDecoderByType(mime)
                codec.configure(format, null, null, 0)
                codec.start()

                val info = MediaCodec.BufferInfo()
                val pcmAmplitudes = ArrayList<Float>()
                var isEOS = false
                val timeoutUs = 5000L
                var framesProcessed = 0
                val maxFrames = 2500

                while (!isEOS && framesProcessed < maxFrames) {
                    val inIndex = codec.dequeueInputBuffer(timeoutUs)
                    if (inIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isEOS = true
                            } else {
                                codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    var outIndex = codec.dequeueOutputBuffer(info, timeoutUs)
                    while (outIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outIndex)
                        if (outputBuffer != null && info.size > 0) {
                            outputBuffer.position(info.offset)
                            outputBuffer.limit(info.offset + info.size)
                            val shortBuffer = outputBuffer.asShortBuffer()
                            var maxAmp = 0
                            while (shortBuffer.hasRemaining()) {
                                val sample = kotlin.math.abs(shortBuffer.get().toInt())
                                if (sample > maxAmp) maxAmp = sample
                            }
                            pcmAmplitudes.add((maxAmp.toFloat() / 32767f).coerceIn(0.05f, 1f))
                            framesProcessed++
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        outIndex = codec.dequeueOutputBuffer(info, 0)
                    }
                }

                if (pcmAmplitudes.isNotEmpty()) {
                    return downsample(pcmAmplitudes, targetSamples)
                }
            }
        } catch (_: Exception) {
        } finally {
            try { codec?.stop(); codec?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
        return generateFallbackWaveform(file, targetSamples)
    }

    private fun downsample(amplitudes: List<Float>, targetSize: Int): List<Float> {
        if (amplitudes.isEmpty()) return emptyList()
        if (amplitudes.size <= targetSize) return amplitudes
        val result = ArrayList<Float>(targetSize)
        val chunkSize = amplitudes.size.toFloat() / targetSize.toFloat()
        for (i in 0 until targetSize) {
            val start = (i * chunkSize).toInt()
            val end = ((i + 1) * chunkSize).toInt().coerceAtMost(amplitudes.size)
            var max = 0.05f
            for (j in start until end) {
                val a = amplitudes[j]
                if (a > max) max = a
            }
            result.add(max.coerceIn(0.05f, 1f))
        }
        return result
    }

    private fun generateFallbackWaveform(file: File, targetSamples: Int): List<Float> {
        val bytes = try {
            if (file.exists()) file.inputStream().use { stream ->
                val buf = ByteArray(1024)
                val read = stream.read(buf)
                if (read > 0) buf.copyOf(read) else ByteArray(0)
            } else ByteArray(0)
        } catch (_: Exception) { ByteArray(0) }

        val seed = if (bytes.isNotEmpty()) bytes.fold(0L) { acc, b -> acc * 31 + b } else file.name.hashCode().toLong()
        val random = java.util.Random(seed)
        return List(targetSamples) {
            (0.12f + random.nextFloat() * 0.76f).coerceIn(0.08f, 1f)
        }
    }

    private data class ExtractedAudioMeta(
        val durationMs: Long,
        val createdAt: Long?,
        val title: String?,
        val waveform: List<Float>
    )
}
