package com.denser.june.core.data.preferences

import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.denser.june.core.domain.model.enums.AiModel
import com.denser.june.core.domain.preferences.AiPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AiPreferencesImpl(
    private val dataStore: DataStore<Preferences>
) : AiPreferences {

    private companion object {
        val AI_MODEL = stringPreferencesKey("ai_model")
    }

    override fun getAiModel(): Flow<AiModel> = dataStore.data
        .map { preferences ->
            val value = preferences[AI_MODEL]
            if (value != null) {
                try {
                    AiModel.valueOf(value)
                } catch (e: Exception) {
                    getDefaultModel()
                }
            } else {
                getDefaultModel()
            }
        }

    override suspend fun setAiModel(model: AiModel) {
        dataStore.edit { preferences ->
            preferences[AI_MODEL] = model.name
        }
    }

    private fun getDefaultModel(): AiModel {
        return if (Build.MANUFACTURER.equals("Google", ignoreCase = true)) {
            AiModel.AICORE
        } else {
            AiModel.GEMMA_4_E2B
        }
    }
}
