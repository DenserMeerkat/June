package com.denser.june.presentation.screens.settings.screens.lock

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.graphics.shapes.RoundedPolygon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PinIndicatorRow(
    pinLength: Int,
    maxPinLength: Int,
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
            MaterialShapes.Clover4Leaf
        ).map { RoundedPolygonShape(it) }
    }

    val shapesForIndices = remember(maxPinLength) {
        List(maxPinLength) { expressiveShapes.random() }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(64.dp)
    ) {
        repeat(maxPinLength) { index ->
            PinDot(
                isFilled = index < pinLength,
                isLastInput = index == pinLength - 1,
                popShape = shapesForIndices[index],
                isError = isError
            )
        }
    }
}

@Composable
fun PinDot(
    isFilled: Boolean,
    isLastInput: Boolean,
    popShape: Shape,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var shapeState by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(1f) }

    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(isFilled) {
        if (isFilled) {
            isInitialized = true
            if (isLastInput) {
                shapeState = 1
                scale.animateTo(1.5f, tween(150, easing = FastOutSlowInEasing))
                scale.animateTo(0.8f, tween(100))
                shapeState = 0
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            } else {
                shapeState = 0
                scale.snapTo(1f)
            }
        } else {
            if (isInitialized) {
                scale.animateTo(0.5f, tween(50))
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            shapeState = 0
            isInitialized = true
        }
    }

    val currentShape = if (shapeState == 1) popShape else CircleShape
    val animatedSize by animateDpAsState(
        targetValue = if (shapeState == 1) 24.dp else 16.dp,
        label = "pin_dot_size"
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .scale(scale.value),
        contentAlignment = Alignment.Center
    ) {
        if (isFilled) {
            val fillColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            Box(
                modifier = Modifier
                    .size(animatedSize)
                    .clip(currentShape)
                    .background(fillColor)
            )
        } else {
            val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .border(2.dp, borderColor, CircleShape)
            )
        }
    }
}

