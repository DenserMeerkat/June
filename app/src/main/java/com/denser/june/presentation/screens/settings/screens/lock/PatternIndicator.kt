package com.denser.june.presentation.screens.settings.screens.lock

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PatternIndicator(
    selectedDots: List<Int>,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    val expressiveShapes = remember {
        listOf(
            MaterialShapes.Cookie4Sided,
            MaterialShapes.Cookie9Sided,
            MaterialShapes.Pentagon,
            MaterialShapes.Pill,
            MaterialShapes.Ghostish,
            MaterialShapes.Arrow,
            MaterialShapes.Diamond,
            MaterialShapes.Clover4Leaf,
            MaterialShapes.Cookie4Sided
        ).map { RoundedPolygonShape(it) }
    }

    Box(
        modifier = modifier.height(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in 0..2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        val isSelected = index in selectedDots
                        val isLastSelected = selectedDots.lastOrNull() == index

                        PatternIndicatorDot(
                            isSelected = isSelected,
                            isLastSelected = isLastSelected,
                            isError = isError,
                            shape = expressiveShapes[index]
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternIndicatorDot(
    isSelected: Boolean,
    isLastSelected: Boolean,
    isError: Boolean,
    shape: Shape
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            if (isLastSelected) {
                scale.animateTo(1.4f, tween(100, easing = FastOutSlowInEasing))
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            } else {
                scale.snapTo(1f)
            }
        } else {
            scale.snapTo(1f)
        }
    }

    val animatedSize by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 10.dp,
        label = "indicator_dot_size"
    )

    val activeColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .size(16.dp)
            .scale(scale.value),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(animatedSize)
                .clip(shape)
                .background(if (isSelected) activeColor else inactiveColor)
        )
    }
}
