package com.denser.june.presentation.screens.settings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.core.domain.ai.AiManager
import com.denser.june.core.domain.model.enums.AiModel
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsPage(
    onNavigateBack: () -> Unit,
    viewModel: SettingsVM = koinViewModel()
) {
    val aiManager: AiManager = koinInject()
    val isModelDownloaded by aiManager.isModelDownloaded.collectAsState()
    val state by viewModel.state.collectAsState()

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
                .verticalScroll(rememberScrollState())
        ) {
            Text("Local AI Integration", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Model Status: ${if (isModelDownloaded) "Ready" else "Downloading / Unavailable"}")
            Spacer(modifier = Modifier.height(24.dp))

            Text("Select AI Model", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            AiModel.entries.forEach { model ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onAction(SettingsAction.OnAiModelChange(model)) }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = state.aiModel == model,
                        onClick = { viewModel.onAction(SettingsAction.OnAiModelChange(model)) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(model.displayName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = model.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("This device uses Google ML Kit's Generative AI to analyze your journal entries automatically. Here's what AI handles:")
            Spacer(modifier = Modifier.height(8.dp))
            Text("• Formatting messy thoughts into clean paragraphs.")
            Text("• Extracting recurring self-doubt into the Improvement Bank.")
            Text("• Collecting actions and proofs for your Micro-Wins dashboard.")
            Text("• Automatically coloring your entries and calendar via Semantic Emotional Weight.")
        }
    }
}
