package com.denser.june.core.domain.model

data class SongFetchProgress(
    val step: Int,
    val totalSteps: Int,
    val label: String,
    val isError: Boolean = false,
    val errorMessage: String? = null
) {
    val fraction: Float get() = (step.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
}
