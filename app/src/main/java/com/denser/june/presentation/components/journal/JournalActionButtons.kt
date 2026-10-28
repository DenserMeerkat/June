package com.denser.june.presentation.components.journal

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R

import com.denser.june.presentation.components.pro.ProLeadingIconContainer
import com.denser.june.presentation.pro.LocalIsPro
import com.denser.june.presentation.utils.UiUtils

@Composable
fun JournalLockActionButton(
    isLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPro = LocalIsPro.current
    val containerColor = if (isLocked) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isLocked) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        if (!isPro && !isLocked) {
            ProLeadingIconContainer(size = 36.dp) {
                Icon(
                    painter = painterResource(R.drawable.lock_open_right_24px),
                    contentDescription = stringResource(R.string.lock),
                    tint = UiUtils.proContentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Icon(
                painter = painterResource(if (isLocked) R.drawable.lock_24px else R.drawable.lock_open_right_24px),
                contentDescription = stringResource(if (isLocked) R.string.unlock else R.string.lock),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun JournalDeleteActionButton(
    isDeleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (isDeleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
        )
    ) {
        Icon(
            painter = painterResource(if (isDeleted) R.drawable.restore_from_trash_24px else R.drawable.delete_24px),
            contentDescription = stringResource(if (isDeleted) R.string.restore else R.string.delete),
            modifier = Modifier.size(24.dp)
        )
    }
}
