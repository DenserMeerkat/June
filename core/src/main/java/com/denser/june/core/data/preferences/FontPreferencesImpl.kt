package com.denser.june.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.denser.june.core.domain.model.FontVariationConfig
import com.denser.june.core.domain.preferences.FontPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class FontPreferencesImpl(
    private val dataStore: DataStore<Preferences>
) : FontPreferences {

    companion object {
        private val appFont = stringPreferencesKey("app_font")
        private val appFontVariation = stringPreferencesKey("app_font_variation")
        private const val DEFAULT_FONT = "Google Sans Flex"
    }

    override fun getAppFont(): Flow<String> = dataStore.data.map { it[appFont] ?: DEFAULT_FONT }

    override suspend fun updateAppFont(fontName: String) {
        dataStore.edit { it[appFont] = fontName }
    }

    override fun getAppFontVariation(): Flow<FontVariationConfig?> = dataStore.data.map { prefs ->
        val json = prefs[appFontVariation]
        if (json != null) {
            try {
                Json.decodeFromString<FontVariationConfig>(json)
            } catch (e: Exception) {
                FontVariationConfig.DEFAULT
            }
        } else {
            FontVariationConfig.DEFAULT
        }
    }

    override suspend fun updateAppFontVariation(variation: FontVariationConfig?) {
        dataStore.edit { prefs ->
            if (variation == null) {
                prefs.remove(appFontVariation)
            } else {
                prefs[appFontVariation] = Json.encodeToString(FontVariationConfig.serializer(), variation)
            }
        }
    }
}
