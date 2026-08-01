package com.denser.june.presentation.screens.settings.screens.sync.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.sync.SyncAnalysis
import com.denser.june.core.domain.sync.SyncStatus
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.SettingsItem
import com.denser.june.presentation.screens.settings.screens.sync.components.SyncAnalysisSection

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    onRepair: () -> Unit,
    onViewDetails: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        SettingSection {
            SettingsItem(
                title = stringResource(R.string.advanced),
                subtitle = stringResource(R.string.advanced_sync_desc),
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
            )
            if (showAdvancedOptions && (analysis != null || isAnalyzing)) {
                SyncAnalysisSection(
                    analysis = analysis,
                    isAnalyzing = isAnalyzing,
                    onViewDetails = onViewDetails
                )
            }
            if (showAdvancedOptions) {
                val isBusy = isAnalyzing || status is SyncStatus.Syncing || status is SyncStatus.Preparing
                SettingsItem(
                    title = stringResource(R.string.analyze_sync),
                    subtitle = stringResource(R.string.analyze_sync_desc),
                    trailingContent = {
                        FilledIconButton(onClick = onAnalyze, enabled = !isBusy) {
                            Icon(
                                painterResource(R.drawable.track_changes_24px),
                                contentDescription = stringResource(R.string.analyze_sync)
                            )
                        }
                    }
                )
                SettingsItem(
                    title = stringResource(R.string.repair_sync),
                    subtitle = stringResource(R.string.repair_sync_short_desc),
                    trailingContent = {
                        FilledIconButton(onClick = onRepair, enabled = !isBusy) {
                            Icon(
                                painterResource(R.drawable.reset_wrench_24px),
                                contentDescription = stringResource(R.string.repair_sync)
                            )
                        }
                    }
                )
            }
        }
    }
}
