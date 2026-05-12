package com.denser.june.core.domain.ai

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AiEngine {
    suspend fun generateContent(prompt: String): String
}

class MockAiEngine : AiEngine {
    override suspend fun generateContent(prompt: String): String {
        return "Mock AI response. AI Core is not properly implemented yet."
    }
}

class AiManager(private val context: Context) {

    private val _isModelDownloaded = MutableStateFlow(true) // Cloud models are always available
    val isModelDownloaded: StateFlow<Boolean> = _isModelDownloaded.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val aiEngine: AiEngine = MockAiEngine()

    suspend fun checkModelStatus(): Boolean {
        val hasAiCore = try {
            context.packageManager.getPackageInfo("com.google.android.aicore", 0)
            true
        } catch (e: Exception) {
            false
        }
        _isModelDownloaded.value = hasAiCore
        return hasAiCore
    }

    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            aiEngine.generateContent(prompt)
        } catch (e: Exception) {
            "AI Error: ${e.message}"
        }
    }
}
