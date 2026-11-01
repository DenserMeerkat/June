package com.denser.june.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun JuneFloatingActionBar(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 8.dp,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun JuneFloatingAction(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    selected: Boolean? = null,
    enabled: Boolean = true,
    containerColor: Color = when (selected) {
        true -> MaterialTheme.colorScheme.surfaceBright
        false -> Color.Transparent
        else -> MaterialTheme.colorScheme.primary
    },
    contentColor: Color = when (selected) {
        true -> MaterialTheme.colorScheme.onSurface
        false -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onPrimary
    },
    disabledContainerColor: Color = Color.Transparent,
    disabledContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
) {
    ExtendedFloatingActionButton(
        onClick = { if (enabled) onClick() },
        expanded = true,
        icon = { icon?.invoke() },
        text = { Text(label, style = MaterialTheme.typography.labelLarge) },
        containerColor = if (enabled) containerColor else disabledContainerColor,
        contentColor = if (enabled) contentColor else disabledContentColor,
        shape = CircleShape,
        modifier = modifier.height(40.dp),
        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
    )
}
