package com.denser.june.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val JuneConnectedListShape = RoundedCornerShape(24.dp)
val JuneConnectedItemShape = RoundedCornerShape(8.dp)

@Composable
fun JuneConnectedList(
    modifier: Modifier = Modifier,
    shape: Shape = JuneConnectedListShape,
    spacing: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.clip(shape),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
fun JuneConnectedItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Surface(
        color = containerColor.copy(alpha = if (enabled) 1f else 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .clip(JuneConnectedItemShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (leadingContent != null) {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        leadingContent()
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (trailingContent != null) 8.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        trailingContent()
                    }
                }
            }
            content?.invoke(this)
        }
    }
}
