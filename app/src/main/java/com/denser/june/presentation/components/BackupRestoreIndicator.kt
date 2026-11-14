package com.denser.june.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.backup.BackupOperationStatus

@Composable
fun BackupRestoreIndicator(
    backupStatus: BackupOperationStatus,
    autoBackupStatus: BackupOperationStatus = BackupOperationStatus.Idle,
    restoreStatus: BackupOperationStatus,
    isExporting: Boolean,
    isRestoring: Boolean,
    isExportingMarkdown: Boolean,
    isImportingMarkdown: Boolean,
    isAppImporting: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val isAutoBackupRunning = autoBackupStatus is BackupOperationStatus.InProgress
    val isBackupRunning = isExporting || backupStatus is BackupOperationStatus.InProgress || isAutoBackupRunning
    val isRestoreRunning = isRestoring || restoreStatus is BackupOperationStatus.InProgress
    val isMarkdownExportRunning = isExportingMarkdown
    val isMarkdownImportRunning = isImportingMarkdown
    val isAppImportRunning = isAppImporting

    val isRunning = isBackupRunning || isRestoreRunning || isMarkdownExportRunning || isMarkdownImportRunning || isAppImportRunning

    AnimatedVisibility(
        visible = isRunning,
        enter = fadeIn(tween(250)) + scaleIn(tween(250)),
        exit = fadeOut(tween(250)) + scaleOut(tween(250)),
        modifier = modifier
    ) {
        val progress = when {
            autoBackupStatus is BackupOperationStatus.InProgress && autoBackupStatus.percentage > 0f -> autoBackupStatus.percentage
            backupStatus is BackupOperationStatus.InProgress && backupStatus.percentage > 0f -> backupStatus.percentage
            restoreStatus is BackupOperationStatus.InProgress && restoreStatus.percentage > 0f -> restoreStatus.percentage
            else -> 0f
        }

        val iconRes = when {
            isBackupRunning -> R.drawable.upload_24px
            isRestoreRunning || isAppImportRunning || isMarkdownImportRunning -> R.drawable.download_24px
            isMarkdownExportRunning -> R.drawable.file_save_24px
            else -> R.drawable.settings_backup_restore
        }

        JuneStatusIndicator(
            iconRes = iconRes,
            isRunning = true,
            progress = progress,
            size = size,
            iconTint = MaterialTheme.colorScheme.primary
        )
    }
}
