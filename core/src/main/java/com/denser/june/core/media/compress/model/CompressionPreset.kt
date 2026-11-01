package com.denser.june.core.media.compress.model

enum class CompressionPreset(val label: String, val stepIndex: Int) {
    LOW("Low", 0),
    MEDIUM("Medium", 1),
    HIGH("High", 2),
    ORIGINAL("Original", 3);

    companion object {
        fun fromStepIndex(index: Int): CompressionPreset = entries.firstOrNull { it.stepIndex == index } ?: MEDIUM
    }
}
