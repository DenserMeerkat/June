package com.denser.june.presentation.components.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.denser.june.core.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorCircleItem(
    color: Int,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swatchShape = if (isSelected) {
        MaterialShapes.Cookie4Sided.toShape()
    } else {
        MaterialShapes.Cookie6Sided.toShape()
    }
    val swatchSize = if (isSelected) 44.dp else 40.dp

    Box(
        modifier = modifier
            .size(42.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(swatchSize)
                .clip(swatchShape)
                .background(Color(color)),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.check_24px),
                    contentDescription = null,
                    tint = if (ColorUtils.calculateLuminance(color) > 0.5) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomPaletteCircleItem(
    colors: List<Color>,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swatchShape = if (isSelected) {
        MaterialShapes.Cookie4Sided.toShape()
    } else {
        MaterialShapes.Cookie9Sided.toShape()
    }
    val swatchSize = if (isSelected) 42.dp else 36.dp

    Box(
        modifier = modifier
            .size(42.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(swatchSize)
                .clip(swatchShape),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.size(swatchSize)) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(colors.getOrElse(0) { Color.Transparent })
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(colors.getOrElse(1) { Color.Transparent })
                    )
                }
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(colors.getOrElse(2) { Color.Transparent })
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(colors.getOrElse(3) { Color.Transparent })
                    )
                }
            }

            if (isSelected) {
                val primaryColor = colors.firstOrNull() ?: Color.Black
                val luminance = (primaryColor.red * 0.299 + primaryColor.green * 0.587 + primaryColor.blue * 0.114)
                Icon(
                    painter = painterResource(R.drawable.check_24px),
                    contentDescription = null,
                    tint = if (luminance > 0.5) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
