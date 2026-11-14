package com.denser.june.presentation.screens.settings.screens.sync.sections

import androidx.compose.runtime.Composable
import com.denser.june.core.domain.sync.SyncStatus

@Composable
fun GoogleDriveConfigSection(
    isVisible: Boolean,
    isConnected: Boolean,
    onRefresh: () -> Unit
) {
    // Empty Composable for FOSS variant since Google Drive is not supported
}
