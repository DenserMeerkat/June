package com.denser.june.core.domain.model.enums

enum class AiModel(val displayName: String, val description: String) {
    AICORE("AI Core", "Default for Pixel phones. Native on-device integration."),
    GEMMA_4_E2B("Gemma 4 E2B", "Optimized for Speed/Battery."),
    GEMMA_4_E4B("Gemma 4 E4B", "Optimized for Accuracy/Reasoning.")
}
