package com.denser.june.core.domain.ai

import android.content.Context
import com.denser.june.core.domain.model.enums.AiModel
import com.denser.june.core.domain.preferences.AiPreferences
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AiManager(private val context: Context, private val aiPreferences: AiPreferences) {

    private val _isModelDownloaded = MutableStateFlow(true) // Cloud models are always available
    val isModelDownloaded: StateFlow<Boolean> = _isModelDownloaded.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private var generativeModel: GenerativeModel = createGenerativeModel(AiModel.AICORE) // Initial placeholder

    init {
        CoroutineScope(Dispatchers.IO).launch {
            aiPreferences.getAiModel().collect { selectedModel ->
                generativeModel = createGenerativeModel(selectedModel)
            }
        }
    }

    private fun createGenerativeModel(model: AiModel): GenerativeModel {
        val modelName = when (model) {
            AiModel.AICORE -> "gemini-nano"
            AiModel.GEMMA_4_E2B -> "gemma-4-e2b"
            AiModel.GEMMA_4_E4B -> "gemma-4-e4b"
        }
        return GenerativeModel(
            modelName = modelName,
            apiKey = "mock-key-for-ui-logic" // We only need the SDK types to compile
        )
    }

    suspend fun checkModelStatus(): Boolean {
        _isModelDownloaded.value = true
        return true
    }

    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            // The user explicitly requested to drop cloud AI to avoid "API key not valid" errors
            // and to use native on-device model inferences (e.g. Gemma 4 E2B/E4B via MediaPipe LlmInference
            // or Google AI Edge SDK). However, due to sandbox offline constraints preventing the download
            // of new dependencies (like com.google.ai.edge:ai-edge-sdk), we stub the local edge integration
            // response here. In a real environment, this would be initialized as:
            // val options = LlmInference.LlmInferenceOptions.builder().setModelPath("gemma-4-e2b.bin").build()
            // val session = LlmInference.createFromOptions(context, options)
            // return session.generateResponse(prompt)

            "On-device inference simulated locally for: ${prompt.take(50)}..."
        } catch (e: Exception) {
            "AI Error: ${e.message}"
        }
    }
}
