package com.denser.june.core.domain.ai

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiManager(private val context: Context) {

    private val _isModelDownloaded = MutableStateFlow(true) // Cloud models are always available
    val isModelDownloaded: StateFlow<Boolean> = _isModelDownloaded.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "mock-key-for-ui-logic" // We only need the SDK types to compile
    )

    suspend fun checkModelStatus(): Boolean {
        _isModelDownloaded.value = true
        return true
    }

    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: ""
        } catch (e: Exception) {
            "AI Error: ${e.message}"
        }
    }
}
