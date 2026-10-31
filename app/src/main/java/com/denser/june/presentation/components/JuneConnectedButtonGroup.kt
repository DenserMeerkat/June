package com.denser.june.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    itemEnabled: (T) -> Boolean = { true },
    equalWeight: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
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
    border: BorderStroke? = null,
    animateWidthOnPress: Boolean = true,
    content: @Composable RowScope.(item: T, isSelected: Boolean) -> Unit
) {
    val interactionSources = remember(options.size) { List(options.size) { MutableInteractionSource() } }

    ButtonGroup(
        overflowIndicator = {},
        horizontalArrangement = horizontalArrangement,
        modifier = modifier
    ) {
        options.forEachIndexed { index, item ->
            val isChecked = selected(item)
            val interactionSource = interactionSources[index]

            customItem(
                buttonGroupContent = {
                    val shape = when {
                        options.size == 1 -> ToggleButtonDefaults.shapes()
                        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        index == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }

                    val baseModifier = if (equalWeight) Modifier.weight(1f) else Modifier
                    val itemModifier = if (animateWidthOnPress) {
                        baseModifier.then(buttonModifier).animateWidth(interactionSource)
                    } else {
                        baseModifier.then(buttonModifier)
                    }

                    ToggleButton(
                        checked = isChecked,
                        onCheckedChange = { onSelect(item) },
                        enabled = enabled && itemEnabled(item),
                        shapes = shape,
                        modifier = itemModifier,
                        contentPadding = contentPadding,
                        colors = colors,
                        border = border,
                        interactionSource = interactionSource
                    ) {
                        content(item, isChecked)
                    }
                },
                menuContent = {}
            )
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
    itemEnabled: (T) -> Boolean = { true },
    equalWeight: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
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
    border: BorderStroke? = null,
    animateWidthOnPress: Boolean = true
) {
    JuneConnectedButtonGroup(
        options = options,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
        enabled = enabled,
        itemEnabled = itemEnabled,
        equalWeight = equalWeight,
        contentPadding = contentPadding,
        buttonModifier = buttonModifier,
        horizontalArrangement = horizontalArrangement,
        colors = colors,
        border = border,
        animateWidthOnPress = animateWidthOnPress
    ) { item, isSelected ->
        Text(
            text = label(item),
            style = textStyle,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
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
    iconOnlyWidth: Dp = 42.dp,
    iconSize: Dp = 18.dp,
    equalWeight: Boolean = false
) {
    val interactionSources = remember(actions.size) { List(actions.size) { MutableInteractionSource() } }

    ButtonGroup(
        overflowIndicator = {},
        horizontalArrangement = horizontalArrangement,
        modifier = modifier
    ) {
        actions.forEachIndexed { index, action ->
            val interactionSource = interactionSources[index]

            customItem(
                buttonGroupContent = {
                    val shape = action.shapes ?: when {
                        actions.size == 1 -> ToggleButtonDefaults.shapes()
                        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        index == actions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    val itemContentColor = when {
                        action.isDestructive -> destructiveColor
                        action.isActive -> action.activeContentColor ?: checkedContentColor
                        else -> contentColor
                    }
                    val itemCheckedContainerColor = action.activeContainerColor ?: checkedContainerColor
                    val buttonModifier = (if (equalWeight) {
                        Modifier.height(buttonHeight).weight(1f)
                    } else if (action.label != null) {
                        Modifier.height(buttonHeight)
                    } else {
                        Modifier.height(buttonHeight).width(iconOnlyWidth)
                    }).animateWidth(interactionSource)

                    ToggleButton(
                        checked = action.isActive,
                        onCheckedChange = { action.onClick() },
                        enabled = action.enabled,
                        shapes = shape,
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = containerColor,
                            contentColor = itemContentColor,
                            checkedContainerColor = itemCheckedContainerColor,
                            checkedContentColor = itemContentColor
                        ),
                        contentPadding = if (action.label != null) {
                            PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        } else {
                            PaddingValues(0.dp)
                        },
                        modifier = buttonModifier,
                        interactionSource = interactionSource
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
                        } else if (action.customContent != null) {
                            action.customContent.invoke()
                        } else if (action.iconRes != null) {
                            Icon(
                                painter = painterResource(action.iconRes),
                                contentDescription = desc,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                },
                menuContent = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
data class ConnectedBarAction(
    val iconRes: Int? = null,
    val label: String? = null,
    val contentDescription: String? = null,
    val contentDescriptionRes: Int? = null,
    val onClick: () -> Unit,
    val isActive: Boolean = false,
    val isDestructive: Boolean = false,
    val enabled: Boolean = true,
    val customContent: (@Composable () -> Unit)? = null,
    val shapes: ToggleButtonShapes? = null,
    val activeContainerColor: Color? = null,
    val activeContentColor: Color? = null
)
