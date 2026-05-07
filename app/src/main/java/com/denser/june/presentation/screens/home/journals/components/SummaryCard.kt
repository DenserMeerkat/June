package com.denser.june.presentation.screens.home.journals.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.ai.AiManager
import kotlinx.coroutines.launch

@Composable
fun SummaryCard(
    aiManager: AiManager,
    journalsContent: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var summaryText by remember { mutableStateOf("Generating summary...") }
    val scope = rememberCoroutineScope()
    val isModelDownloaded by aiManager.isModelDownloaded.collectAsState()

    LaunchedEffect(journalsContent) {
        if (!isModelDownloaded) {
            summaryText = "Model not downloaded."
            return@LaunchedEffect
        }
        if (journalsContent.isBlank()) {
            summaryText = "Not enough entries to generate a summary."
            return@LaunchedEffect
        }
        summaryText = "Generating summary..."
        scope.launch {
            summaryText = aiManager.generateContent(
                "Provide a brief, objective synthesis of these journal entries, focusing on emotional patterns, wins, and areas of friction. Journal entries:\n$journalsContent"
            )
        }
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.auto_stories_24px),
                        contentDescription = "AI Summary",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Weekly Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    painter = painterResource(if (expanded) R.drawable.keyboard_arrow_down_24px else R.drawable.chevron_right_24px),
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
