package com.denser.june.core.domain.model

data class SyncStatusUpdate(
    val id: String,
    val cloudId: String,
    val syncedAt: Long
)
