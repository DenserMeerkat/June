package com.denser.june.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
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
    val containerSize = size + 12.dp
    
    val animatedIconSize by animateDpAsState(
        targetValue = if (status is SyncStatus.Syncing || status is SyncStatus.Preparing) size * 0.75f else size,
        animationSpec = tween(durationMillis = 300),
        label = "icon_size"
    )

    Box(
        modifier = modifier
            .size(containerSize)
            .let { m ->
                if (onClick != null) {
                    m.clip(CircleShape)
                     .clickable(onClick = onClick)
                } else m
            },
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            is SyncStatus.Preparing -> {
                 CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            }
            is SyncStatus.Syncing -> {
                if (status.progress > 0) {
                    CircularProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                }
            }
            else -> {}
        }
        
        val iconRes = when (status) {
            is SyncStatus.Error -> R.drawable.sync_problem_24px
            is SyncStatus.Success -> R.drawable.cloud_done_24px
            else -> R.drawable.cloud_sync_24px
        }
        
        val iconTint = when (status) {
            is SyncStatus.Error -> MaterialTheme.colorScheme.error
            is SyncStatus.Success -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        }

        Icon(
            painter = painterResource(iconRes),
            contentDescription = "Sync Status: $status",
            modifier = Modifier.size(animatedIconSize),
            tint = iconTint
        )
    }
}
