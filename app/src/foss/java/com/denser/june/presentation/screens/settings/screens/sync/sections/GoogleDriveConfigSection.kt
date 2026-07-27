package com.denser.june.presentation.screens.settings.screens.sync.sections

import androidx.compose.runtime.Composable
import com.denser.june.core.domain.sync.SyncStatus

@Composable
fun GoogleDriveConfigSection(
    isVisible: Boolean,
    isConnected: Boolean,
    status: SyncStatus,
    isTestingConnection: Boolean,
    onTestConnection: () -> Unit,
    onManualSync: () -> Unit,
    onRefresh: () -> Unit,
    onStopSync: (() -> Unit)? = null
) {
    // Empty Composable for FOSS variant since Google Drive is not supported
}
