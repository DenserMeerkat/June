package com.denser.june.core.data.database.journal

data class JournalMetadataEntity(
    val id: String,
    val dateTime: Long,
    val title: String,
    val createdAt: Long = 0,
    val updatedAt: Long? = null,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val cloudId: String? = null
)
