package com.denser.june.core.domain.preferences

import com.denser.june.core.domain.model.enums.AiModel
import kotlinx.coroutines.flow.Flow

interface AiPreferences {
    fun getAiModel(): Flow<AiModel>
    suspend fun setAiModel(model: AiModel)
}
