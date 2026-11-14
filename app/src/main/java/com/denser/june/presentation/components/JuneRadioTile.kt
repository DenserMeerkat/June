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

enum class JuneRadioTilePosition {
    Leading,
    Trailing
}

@Composable
fun JuneRadioTile(
    selected: Boolean,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    badgeContent: (@Composable () -> Unit)? = null,
    prefixContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    radioPosition: JuneRadioTilePosition = JuneRadioTilePosition.Leading,
    shape: Shape? = null,
    titleTextStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
    titleColor: Color? = null,
    selectedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    index: Int? = null,
    count: Int? = null,
    pressedRadius: Dp = 28.dp,
    selectedRadius: Dp = 28.dp
) {
    val resolvedTitleColor = titleColor ?: if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
    }

    val radioComposable = @Composable {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                modifier = if (subtitle != null && radioPosition == JuneRadioTilePosition.Leading) {
                    Modifier.padding(top = 2.dp)
                } else Modifier,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }

    val leadingContent: (@Composable () -> Unit)? = when {
        radioPosition == JuneRadioTilePosition.Leading -> radioComposable
        prefixContent != null -> prefixContent
        else -> null
    }

    val trailingContent: (@Composable () -> Unit)? = when {
        radioPosition == JuneRadioTilePosition.Trailing -> radioComposable
        else -> null
    }

    JuneConnectedRowItem(
        modifier = modifier,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        enabled = enabled,
        shape = shape,
        containerColor = if (selected) selectedContainerColor else unselectedContainerColor,
        contentPadding = contentPadding,
        onClick = onClick,
        index = index,
        count = count,
        pressedRadius = pressedRadius,
        selected = selected,
        selectedRadius = selectedRadius
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (radioPosition == JuneRadioTilePosition.Leading) {
                    prefixContent?.invoke()
                }
                Text(
                    text = title,
                    style = titleTextStyle,
                    color = resolvedTitleColor
                )
                if (badgeContent != null) {
                    badgeContent()
                } else if (badge != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
