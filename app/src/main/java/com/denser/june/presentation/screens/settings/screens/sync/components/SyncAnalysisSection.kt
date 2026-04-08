package com.denser.june.presentation.screens.settings.screens.sync.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.sync.SyncAnalysis

@Composable
fun SyncAnalysisSection(
    analysis: SyncAnalysis?,
    isAnalyzing: Boolean
) {
    if (analysis == null && !isAnalyzing) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Sync Analysis", 
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalysisStatColumn("Local", analysis?.localJournals, analysis?.localMedia, Modifier.weight(1f))
                VerticalDivider(
                    modifier = Modifier.height(44.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                AnalysisStatColumn("Cloud", analysis?.remoteJournals, analysis?.remoteMedia, Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabelWithValue(
                    label = "Journals", 
                    value = analysis?.let { "${it.pendingUploadsCount} ↑ / ${it.pendingDownloadsCount} ↓" } ?: "- / -"
                )
                LabelWithValue(
                    label = "Media", 
                    value = analysis?.let { "${it.pendingMediaUploads} ↑ / ${it.pendingMediaDownloads} ↓" } ?: "- / -"
                )
            }
        }
    }
}

@Composable
private fun AnalysisStatColumn(title: String, journals: Int?, media: Int?, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text("${journals ?: "-"} Journals", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text("${media ?: "-"} Media", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LabelWithValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}
