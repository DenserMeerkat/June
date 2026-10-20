package com.denser.june.core.media.color

import com.denser.june.core.domain.model.JournalTheme

data class ExtractedMediaColor(
    val color: Int,
    val sourceLabel: String,
    val mediaUri: Any? = null
)

data class ExtractedMediaTheme(
    val theme: JournalTheme,
    val sourceLabel: String,
    val variantLabel: String = "",
    val mediaUri: Any? = null
)

data class ExtractedCardColors(
    val surface: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val secondaryContainer: Int,
    val onSecondaryContainer: Int
)
