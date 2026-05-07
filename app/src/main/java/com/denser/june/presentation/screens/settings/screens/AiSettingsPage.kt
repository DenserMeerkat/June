package com.denser.june.presentation.screens.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.core.domain.ai.AiManager
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsPage(
    onNavigateBack: () -> Unit
) {
    val aiManager: AiManager = koinInject()
    val isModelDownloaded by aiManager.isModelDownloaded.collectAsState()

    LaunchedEffect(Unit) {
        aiManager.checkModelStatus()
    }

    Scaffold(
        topBar = {
            JuneTopAppBar(
                title = { Text("AI Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(painterResource(R.drawable.arrow_back_24px), contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Gemini Nano AI Integration", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Model Status: ${if (isModelDownloaded) "Ready" else "Downloading / Unavailable"}")
            Spacer(modifier = Modifier.height(8.dp))
            Text("This device uses Google ML Kit's Generative AI to analyze your journal entries automatically. Here's what AI handles:")
            Spacer(modifier = Modifier.height(8.dp))
            Text("• Formatting messy thoughts into clean paragraphs.")
            Text("• Extracting recurring self-doubt into the Improvement Bank.")
            Text("• Collecting actions and proofs for your Micro-Wins dashboard.")
            Text("• Automatically coloring your entries and calendar via Semantic Emotional Weight.")
        }
    }
}
