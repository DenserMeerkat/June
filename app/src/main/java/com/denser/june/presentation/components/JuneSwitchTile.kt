package com.denser.june.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun JuneSwitchTile(
    checked: Boolean,
    title: String,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes iconRes: Int? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentPadding: PaddingValues = if (subtitle == null) {
        PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    } else {
        PaddingValues(16.dp)
    },
    index: Int? = null,
    count: Int? = null,
    pressedRadius: Dp = 28.dp,
    selectedRadius: Dp = 28.dp,
    onClick: (() -> Unit)? = if (onCheckedChange != null && enabled) {
        { onCheckedChange(!checked) }
    } else null
) {
    val switchComposable = @Composable {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }

    val resolvedLeadingContent: (@Composable () -> Unit)? = leadingContent ?: if (iconRes != null) {
        {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    } else null

    JuneConnectedItem(
        title = title,
        subtitle = subtitle,
        leadingContent = resolvedLeadingContent,
        trailingContent = switchComposable,
        enabled = enabled,
        shape = shape,
        containerColor = containerColor,
        contentPadding = contentPadding,
        onClick = onClick,
        index = index,
        count = count,
        pressedRadius = pressedRadius,
        selectedRadius = selectedRadius,
        modifier = modifier
    )
}
