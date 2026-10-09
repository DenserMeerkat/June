package com.denser.june.presentation.components.lightbox

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.denser.june.core.media.compress.CompressUtils
import com.denser.june.core.media.compress.model.CompressionPreset
import com.denser.june.core.media.compress.model.CompressionPreviewResult
import com.denser.june.core.media.exif.ExifUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LightboxCompressionState(
    val currentPath: String?,
    private val context: Context,
    private val photoQualityPref: Int,
    private val videoQualityPref: Int
) {
    var isCompressMode by mutableStateOf(false)
    var isCompressing by mutableStateOf(false)
    var selectedPreset by mutableStateOf<CompressionPreset?>(null)
    var appliedPreset by mutableStateOf<CompressionPreset?>(null)
    var availablePresets by mutableStateOf(setOf(CompressionPreset.ORIGINAL))
    var isGeneratingPreview by mutableStateOf(false)
    val sessionPreviewCache = mutableMapOf<CompressionPreset, CompressionPreviewResult>()

    val currentPreview: CompressionPreviewResult?
        get() = selectedPreset?.let { sessionPreviewCache[it] }

    fun pickDefaultPreset(): CompressionPreset {
        return appliedPreset ?: run {
            if (currentPath != null && ExifUtils.isVideoPath(currentPath)) {
                when {
                    videoQualityPref <= 480 && availablePresets.contains(CompressionPreset.LOW) -> CompressionPreset.LOW
                    videoQualityPref <= 720 && availablePresets.contains(CompressionPreset.MEDIUM) -> CompressionPreset.MEDIUM
                    videoQualityPref > 720 && availablePresets.contains(CompressionPreset.HIGH) -> CompressionPreset.HIGH
                    availablePresets.contains(CompressionPreset.MEDIUM) -> CompressionPreset.MEDIUM
                    availablePresets.contains(CompressionPreset.LOW) -> CompressionPreset.LOW
                    availablePresets.contains(CompressionPreset.HIGH) -> CompressionPreset.HIGH
                    else -> CompressionPreset.ORIGINAL
                }
            } else {
                when {
                    photoQualityPref <= 70 && availablePresets.contains(CompressionPreset.LOW) -> CompressionPreset.LOW
                    photoQualityPref <= 80 && availablePresets.contains(CompressionPreset.MEDIUM) -> CompressionPreset.MEDIUM
                    photoQualityPref > 80 && availablePresets.contains(CompressionPreset.HIGH) -> CompressionPreset.HIGH
                    availablePresets.contains(CompressionPreset.MEDIUM) -> CompressionPreset.MEDIUM
                    availablePresets.contains(CompressionPreset.LOW) -> CompressionPreset.LOW
                    availablePresets.contains(CompressionPreset.HIGH) -> CompressionPreset.HIGH
                    else -> CompressionPreset.ORIGINAL
                }
            }
        }
    }

    fun startCompression() {
        if (currentPath != null && !isCompressing) {
            selectedPreset = pickDefaultPreset()
            isCompressMode = true
        }
    }

    fun cancelCompression() {
        CompressUtils.cleanupPreviewCache(sessionPreviewCache)
        sessionPreviewCache.clear()
        isCompressMode = false
    }

    fun cleanup() {
        CompressUtils.cleanupPreviewCache(sessionPreviewCache)
        sessionPreviewCache.clear()
    }
}

@Composable
fun rememberLightboxCompressionState(
    currentPath: String?,
    context: Context,
    refreshTrigger: Int,
    photoQualityPref: Int,
    videoQualityPref: Int
): LightboxCompressionState {
    val state = remember(currentPath) {
        LightboxCompressionState(
            currentPath = currentPath,
            context = context,
            photoQualityPref = photoQualityPref,
            videoQualityPref = videoQualityPref
        )
    }

    DisposableEffect(currentPath) {
        onDispose {
            state.cleanup()
        }
    }

    LaunchedEffect(currentPath, refreshTrigger) {
        if (currentPath != null) {
            val applied = withContext(Dispatchers.IO) {
                CompressUtils.getAppliedPreset(context, currentPath)
            }
            val available = withContext(Dispatchers.IO) {
                CompressUtils.getAvailablePresets(context, currentPath)
            }
            state.appliedPreset = applied
            state.availablePresets = available
            if (!state.isCompressMode) {
                state.selectedPreset = state.pickDefaultPreset()
            }
        } else {
            state.appliedPreset = null
            state.availablePresets = setOf(CompressionPreset.ORIGINAL)
            state.selectedPreset = null
        }
    }

    LaunchedEffect(state.isCompressMode, state.selectedPreset, currentPath) {
        val preset = state.selectedPreset
        if (!state.isCompressMode || currentPath == null || preset == null) return@LaunchedEffect
        if (preset == CompressionPreset.ORIGINAL) {
            state.isGeneratingPreview = false
            return@LaunchedEffect
        }
        if (state.sessionPreviewCache.containsKey(preset)) {
            state.isGeneratingPreview = false
            return@LaunchedEffect
        }

        state.isGeneratingPreview = true
        val result = withContext(Dispatchers.IO) {
            if (ExifUtils.isVideoPath(currentPath)) {
                CompressUtils.estimateVideoCompression(context, currentPath, preset)
            } else {
                CompressUtils.generatePhotoPreview(context, currentPath, preset)
            }
        }
        state.sessionPreviewCache[preset] = result
        state.isGeneratingPreview = false
    }

    return state
}
