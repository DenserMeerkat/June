package com.denser.june.presentation.components.plus

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.denser.june.presentation.plus.LocalIsPlus
import com.denser.june.presentation.screens.settings.components.SettingsItem
import com.denser.june.presentation.utils.UiUtils

@Composable
fun PlusSettingsItem(
    title: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val isPlus = LocalIsPlus.current
    val resolvedPadding = if (!isPlus) {
        PaddingValues(
            start = (contentPadding.calculateLeftPadding(LayoutDirection.Ltr) - 6.dp).coerceAtLeast(0.dp),
            top = contentPadding.calculateTopPadding(),
            end = contentPadding.calculateRightPadding(LayoutDirection.Ltr),
            bottom = contentPadding.calculateBottomPadding()
        )
    } else {
        contentPadding
    }

    SettingsItem(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        enabled = enabled,
        containerColor = containerColor,
        contentPadding = resolvedPadding,
        trailingContent = trailingContent,
        leadingContent = {
            if (!isPlus) {
                PlusLeadingIconContainer {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = UiUtils.plusContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        },
        onClick = onClick,
        content = content
    )
}
