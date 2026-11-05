package com.denser.june.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class JournalAudio(
    val id: String,
    val path: String,
    val durationMs: Long,
    val title: String? = null,
    val format: String = "m4a",
    val size: Long = 0L,
    val waveform: List<Float> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

