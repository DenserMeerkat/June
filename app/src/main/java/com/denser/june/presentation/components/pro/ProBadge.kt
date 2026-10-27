package com.denser.june.presentation.components.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.denser.june.presentation.utils.UiUtils

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProBadge(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    iconSize: Dp = 10.dp,
    shape: Shape = UiUtils.proShape,
    containerColor: Color = UiUtils.proContainerColor,
    contentColor: Color = UiUtils.proContentColor
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
                painter = painterResource(UiUtils.PRO_ICON_RES),
                contentDescription = "Pro",
                modifier = Modifier.size(iconSize),
                tint = contentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProLeadingIconContainer(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shape: Shape = UiUtils.proShape,
    containerColor: Color = UiUtils.proContainerColor,
    contentColor: Color = UiUtils.proContentColor,
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

@Composable
fun ProBadgeWithLabel(
    modifier: Modifier = Modifier,
    label: String = "PRO",
    shape: Shape = RoundedCornerShape(8.dp),
    containerColor: Color = UiUtils.proContainerColor,
    contentColor: Color = UiUtils.proContentColor
) {
    Surface(
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(UiUtils.PRO_ICON_RES),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = contentColor
            )
        }
    }
}

