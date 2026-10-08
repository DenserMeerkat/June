package com.denser.june.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

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
