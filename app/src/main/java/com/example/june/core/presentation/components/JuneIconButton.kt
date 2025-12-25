package com.example.june.core.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class JuneIconButtonType {
    Ghost,
    Outline,
    Surface,
    Filled,
    FilledTonal,
}

@Composable
fun JuneIconButton(
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: JuneIconButtonType = JuneIconButtonType.Surface,
    contentDescription: String? = null,
    buttonSize: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
    colors: IconButtonColors? = null
) {
    val sizingModifier = modifier.size(buttonSize)

    val iconContent: @Composable () -> Unit = {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }

    when (type) {
        JuneIconButtonType.Ghost -> {
            IconButton(
                onClick = onClick,
                modifier = sizingModifier,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75F)
                ),
                content = iconContent
            )
        }

        JuneIconButtonType.Outline -> {
            OutlinedIconButton(
                onClick = onClick,
                modifier = sizingModifier,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.outlinedIconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75F)
                ),
                content = iconContent
            )
        }

        JuneIconButtonType.Surface -> {
            IconButton(
                onClick = onClick,
                modifier = sizingModifier,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75F)
                ),
                content = iconContent
            )
        }

        JuneIconButtonType.Filled -> {
            FilledIconButton(
                onClick = onClick,
                modifier = sizingModifier,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.filledIconButtonColors(),
                content = iconContent
            )
        }

        JuneIconButtonType.FilledTonal -> {
            FilledTonalIconButton(
                onClick = onClick,
                modifier = sizingModifier,
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.filledTonalIconButtonColors(),
                content = iconContent
            )
        }
    }
}