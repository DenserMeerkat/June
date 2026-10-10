package com.denser.june.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> JuneConnectedButtonGroup(
    options: List<T>,
    selected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    equalWeight: Boolean = false,
    contentPadding: PaddingValues = ToggleButtonDefaults.ContentPadding,
    buttonModifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(
        ButtonGroupDefaults.ConnectedSpaceBetween,
        Alignment.End
    ),
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ),
    content: @Composable RowScope.(item: T, isSelected: Boolean) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        options.forEachIndexed { index, item ->
            val isChecked = selected(item)
            val shape = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }

            val itemModifier = if (equalWeight) Modifier.weight(1f).then(buttonModifier) else buttonModifier

            ToggleButton(
                checked = isChecked,
                onCheckedChange = { onSelect(item) },
                enabled = enabled,
                shapes = shape,
                modifier = itemModifier,
                contentPadding = contentPadding,
                colors = colors
            ) {
                content(item, isChecked)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> JuneConnectedButtonGroup(
    options: List<T>,
    selected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    equalWeight: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    contentPadding: PaddingValues = ToggleButtonDefaults.ContentPadding,
    buttonModifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(
        ButtonGroupDefaults.ConnectedSpaceBetween,
        Alignment.End
    ),
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    )
) {
    JuneConnectedButtonGroup(
        options = options,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
        enabled = enabled,
        equalWeight = equalWeight,
        contentPadding = contentPadding,
        buttonModifier = buttonModifier,
        horizontalArrangement = horizontalArrangement,
        colors = colors
    ) { item, isSelected ->
        Text(
            text = label(item),
            style = textStyle,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JuneConnectedActionBar(
    actions: List<ConnectedBarAction>,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    checkedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    checkedContentColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    destructiveColor: Color = MaterialTheme.colorScheme.error,
    buttonHeight: Dp = 36.dp,
    iconOnlyWidth: Dp = 42.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEachIndexed { index, action ->
            val shape = when {
                actions.size == 1 -> ToggleButtonDefaults.shapes()
                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                index == actions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            val itemContentColor = when {
                action.isDestructive -> destructiveColor
                action.isActive -> checkedContentColor
                else -> contentColor
            }
            val buttonModifier = if (action.label != null) {
                Modifier.height(buttonHeight)
            } else {
                Modifier.height(buttonHeight).width(iconOnlyWidth)
            }

            ToggleButton(
                checked = action.isActive,
                onCheckedChange = { action.onClick() },
                shapes = shape,
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = containerColor,
                    contentColor = itemContentColor,
                    checkedContainerColor = checkedContainerColor,
                    checkedContentColor = checkedContentColor
                ),
                contentPadding = if (action.label != null) {
                    PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                } else {
                    PaddingValues(0.dp)
                },
                modifier = buttonModifier
            ) {
                val desc = action.contentDescription ?: action.contentDescriptionRes?.let { stringResource(it) }
                if (action.label != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (action.iconRes != null) {
                            Icon(
                                painter = painterResource(action.iconRes),
                                contentDescription = desc,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else if (action.iconRes != null) {
                    Icon(
                        painter = painterResource(action.iconRes),
                        contentDescription = desc,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

data class ConnectedBarAction(
    val iconRes: Int? = null,
    val label: String? = null,
    val contentDescription: String? = null,
    val contentDescriptionRes: Int? = null,
    val onClick: () -> Unit,
    val isActive: Boolean = false,
    val isDestructive: Boolean = false
)
