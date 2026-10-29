package com.denser.june.presentation.components.plus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.presentation.utils.UiUtils

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlusBadge(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    iconSize: Dp = 14.dp,
    shape: Shape = UiUtils.plusShape,
    containerColor: Color = UiUtils.plusContainerColor,
    contentColor: Color = UiUtils.plusContentColor
) {
    Surface(
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(UiUtils.PLUS_ICON_RES),
                contentDescription = "Plus",
                modifier = Modifier.size(iconSize),
                tint = contentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlusLeadingIconContainer(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shape: Shape = UiUtils.plusShape,
    containerColor: Color = UiUtils.plusContainerColor,
    contentColor: Color = UiUtils.plusContentColor,
    content: @Composable () -> Unit
) {
    Surface(
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
