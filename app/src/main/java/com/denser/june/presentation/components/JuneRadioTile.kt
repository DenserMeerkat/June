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
    enabled: Boolean = true,
    radioPosition: JuneRadioTilePosition = JuneRadioTilePosition.Leading,
    shape: Shape = RoundedCornerShape(12.dp),
    titleTextStyle: TextStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleColor: Color? = null,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
    unselectedContainerColor: Color = Color.Transparent,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
) {
    val resolvedTitleColor = titleColor ?: if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = if (selected) selectedContainerColor else unselectedContainerColor,
        border = border,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalAlignment = if (subtitle != null && radioPosition == JuneRadioTilePosition.Leading) {
                Alignment.Top
            } else {
                Alignment.CenterVertically
            }
        ) {
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

            if (radioPosition == JuneRadioTilePosition.Leading) {
                radioComposable()
                Spacer(modifier = Modifier.width(10.dp))
            }

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
                    if (badge != null) {
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

            if (radioPosition == JuneRadioTilePosition.Trailing) {
                Spacer(modifier = Modifier.width(8.dp))
                radioComposable()
            }
        }
    }
}
