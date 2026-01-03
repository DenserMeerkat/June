package com.example.june.core.domain.backup

import com.example.june.core.domain.data_classes.Journal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Export")
data class ExportSchema(
    val schemaVersion: Int = 1,
    val journals: List<Journal>
)