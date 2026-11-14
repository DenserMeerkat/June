package com.denser.june.presentation.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.denser.june.core.media.storage.AudioStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioRecordManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var tickerJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    fun start(): Boolean {
        return try {
            cancel()
            val outputFile = AudioStorageManager.createTempAudioFile(context)
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _isRecording.value = true
            _isPaused.value = false
            _elapsedMillis.value = 0L
            _amplitudes.value = emptyList()

            startTicker()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            cancel()
            false
        }
    }

    fun pause() {
        if (!_isRecording.value || _isPaused.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                _isPaused.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resume() {
        if (!_isRecording.value || !_isPaused.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                _isPaused.value = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop(): File? {
        if (!_isRecording.value) return null
        tickerJob?.cancel()
        tickerJob = null

        return try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            _isRecording.value = false
            _isPaused.value = false

            val result = currentOutputFile
            currentOutputFile = null
            result
        } catch (e: Exception) {
            e.printStackTrace()
            cancel()
            null
        }
    }

    fun cancel() {
        tickerJob?.cancel()
        tickerJob = null
        try {
            mediaRecorder?.apply {
                reset()
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null

        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        currentOutputFile = null

        _isRecording.value = false
        _isPaused.value = false
        _elapsedMillis.value = 0L
        _amplitudes.value = emptyList()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch(Dispatchers.Default) {
            var startTime = System.currentTimeMillis() - _elapsedMillis.value
            while (isActive && _isRecording.value) {
                if (!_isPaused.value) {
                    val now = System.currentTimeMillis()
                    _elapsedMillis.value = (now - startTime).coerceAtLeast(0L)

                    val rawAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (_: Exception) { 0 }

                    val normalizedAmp = (rawAmp.toFloat() / 32767f).coerceIn(0.05f, 1f)
                    val currentList = _amplitudes.value
                    _amplitudes.value = currentList + normalizedAmp
                } else {
                    startTime = System.currentTimeMillis() - _elapsedMillis.value
                }
                delay(60)
            }
        }
    }
}

