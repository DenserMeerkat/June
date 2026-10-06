package com.denser.june.core.media.compress.model

data class CompressionPreviewResult(
    val preset: CompressionPreset,
    val previewPath: String,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val savingsPercent: Int,
    val isAlreadyOptimal: Boolean,
    val isClipped: Boolean = false
)
