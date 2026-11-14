package com.denser.june.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class JuneCheckboxTilePosition {
    Leading,
    Trailing
}

@Composable
fun JuneCheckboxTile(
    checked: Boolean,
    title: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    badge: String? = null,
    badgeContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    checkboxPosition: JuneCheckboxTilePosition = JuneCheckboxTilePosition.Trailing,
    shape: Shape? = null,
    titleTextStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
    titleColor: Color? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    index: Int? = null,
    count: Int? = null,
    pressedRadius: Dp = 28.dp,
    selectedRadius: Dp = 28.dp
) {
    val resolvedTitleColor = titleColor ?: if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    val checkboxComposable = @Composable {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                modifier = if (subtitle != null && checkboxPosition == JuneCheckboxTilePosition.Leading) {
                    Modifier.padding(top = 2.dp)
                } else Modifier,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }

    val resolvedLeadingContent: (@Composable () -> Unit)? = when {
        checkboxPosition == JuneCheckboxTilePosition.Leading -> checkboxComposable
        leadingContent != null -> leadingContent
        else -> null
    }

    val resolvedTrailingContent: (@Composable () -> Unit)? = when {
        checkboxPosition == JuneCheckboxTilePosition.Trailing -> checkboxComposable
        else -> null
    }

    JuneConnectedRowItem(
        modifier = modifier,
        leadingContent = resolvedLeadingContent,
        trailingContent = resolvedTrailingContent,
        enabled = enabled,
        shape = shape,
        containerColor = containerColor,
        contentPadding = contentPadding,
        onClick = { onCheckedChange(!checked) },
        index = index,
        count = count,
        pressedRadius = pressedRadius,
        selected = checked,
        selectedRadius = selectedRadius
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = titleTextStyle,
                    color = resolvedTitleColor
                )
                if (badgeContent != null) {
                    badgeContent()
                } else if (badge != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (enabled) 0.5f else 0.25f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
    }
}

