package com.denser.june.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.sync.SyncStatus

@Composable
fun SyncIndicator(
    modifier: Modifier = Modifier,
    status: SyncStatus,
    onClick: (() -> Unit)? = null,
    size: Dp = 24.dp
) {
    val isRunning = status is SyncStatus.Syncing || status is SyncStatus.Preparing
    val progress = if (status is SyncStatus.Syncing) status.progress else 0f

    val iconRes = when (status) {
        is SyncStatus.Error -> R.drawable.sync_problem_24px
        is SyncStatus.Success -> R.drawable.cloud_done_24px
        is SyncStatus.Dirty -> R.drawable.cloud_upload_24px
        else -> R.drawable.cloud_sync_24px
    }

    val iconTint = when (status) {
        is SyncStatus.Error -> MaterialTheme.colorScheme.error
        is SyncStatus.Success -> MaterialTheme.colorScheme.primary
        is SyncStatus.Dirty -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    JuneStatusIndicator(
        iconRes = iconRes,
        modifier = modifier,
        iconTint = iconTint,
        contentDescription = "Sync Status: $status",
        isRunning = isRunning,
        progress = progress,
        size = size,
        onClick = onClick
    )
}
