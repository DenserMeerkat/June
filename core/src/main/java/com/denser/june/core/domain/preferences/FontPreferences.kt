package com.denser.june.core.domain.preferences

import com.denser.june.core.domain.model.FontVariationConfig
import kotlinx.coroutines.flow.Flow

interface FontPreferences {
    fun getAppFont(): Flow<String>

    suspend fun updateAppFont(fontName: String)

    fun getAppFontVariation(): Flow<FontVariationConfig?>

    suspend fun updateAppFontVariation(variation: FontVariationConfig?)
}
