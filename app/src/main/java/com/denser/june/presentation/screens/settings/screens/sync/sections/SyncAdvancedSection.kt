package com.denser.june.presentation.screens.settings.screens.sync.sections

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.sync.SyncAnalysis
import com.denser.june.core.domain.sync.SyncStatus
import com.denser.june.presentation.screens.settings.screens.sync.components.SyncAnalysisSection
import com.denser.june.presentation.screens.settings.section.SettingsItem

@Composable
fun SyncAdvancedSection(
    isVisible: Boolean,
    showAdvancedOptions: Boolean,
    isAnalyzing: Boolean,
    status: SyncStatus,
    analysis: SyncAnalysis?,
    rotationAngle: Float,
    onToggleAdvanced: () -> Unit,
    onAnalyze: () -> Unit,
    onRepair: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsItem(
                    title = "Advanced",
                    subtitle = "Analysis & repair tools",
                    leadingContent = {
                        Icon(painterResource(R.drawable.settings_24px), null)
                    },
                    trailingContent = {
                        Icon(
                            painterResource(R.drawable.keyboard_arrow_down_24px),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(rotationAngle),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = onToggleAdvanced
                ) {
                    AnimatedVisibility(
                        visible = showAdvancedOptions,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (analysis != null || isAnalyzing) {
                                    SyncAnalysisSection(
                                        analysis = analysis,
                                        isAnalyzing = isAnalyzing
                                    )
                                }
                                
                                Button(
                                    onClick = onAnalyze,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isAnalyzing && status !is SyncStatus.Syncing && status !is SyncStatus.Preparing,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.track_changes_24px),
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyze Sync")
                                }
                                
                                OutlinedButton(
                                    onClick = onRepair,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isAnalyzing && status !is SyncStatus.Syncing && status !is SyncStatus.Preparing,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.reset_wrench_24px),
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Repair Sync")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
