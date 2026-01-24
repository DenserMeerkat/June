package com.denser.june.core.presentation.components

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.denser.june.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinLockScreen(
    modifier: Modifier = Modifier,
    title: String = "Enter PIN",
    isError: Boolean = false,
    maxPinLength: Int = 6,
    onPinSubmitted: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var waveTrigger by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isError) {
        if (isError) {
            pin = ""
            waveTrigger++
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
        }
    }

    LaunchedEffect(pin) {
        if (pin.length == maxPinLength) {
            delay(DELAY_AUTO_SUBMIT)
            onPinSubmitted(pin)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 48.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            AppIconDisplay()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isError) "Wrong PIN. Try again." else title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(48.dp))

            PinIndicatorRow(pinLength = pin.length, maxPinLength = maxPinLength)
        }

        NumberPad(pinLength = pin.length, waveTrigger = waveTrigger, onNumberClick = {
            if (pin.length < maxPinLength) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                pin += it
            }
        }, onDeleteClick = {
            if (pin.isNotEmpty()) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                pin = pin.dropLast(1)
            }
        }, onClearAll = {
            if (pin.isNotEmpty()) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                pin = ""
            }
        })
    }
}

@Composable
private fun AppIconDisplay() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "App Icon",
            modifier = Modifier
                .fillMaxSize()
                .scale(1.25f),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PinIndicatorRow(pinLength: Int, maxPinLength: Int) {
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
        modifier = Modifier.height(64.dp)
    ) {
        repeat(maxPinLength) { index ->
            PinDot(
                isFilled = index < pinLength,
                isLastInput = index == pinLength - 1,
                popShape = shapesForIndices[index]
            )
        }
    }
}

@Composable
private fun PinDot(isFilled: Boolean, isLastInput: Boolean, popShape: Shape) {
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
        targetValue = if (shapeState == 1) 24.dp else 16.dp, label = "size"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(scale.value),
        contentAlignment = Alignment.Center
    ) {
        if (isFilled) {
            Box(
                modifier = Modifier
                    .size(animatedSize)
                    .clip(currentShape)
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
            )
        }
    }
}

@Composable
private fun NumberPad(
    pinLength: Int,
    waveTrigger: Int,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearAll: () -> Unit,
) {
    val buttonScales = remember { List(10) { Animatable(1f) } }

    LaunchedEffect(waveTrigger) {
        if (waveTrigger > 0) {
            val sequence = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
            sequence.forEachIndexed { index, num ->
                launch {
                    delay(index * DELAY_WAVE_STAGGER)
                    buttonScales[num].animateTo(0.8f, tween(100))
                    buttonScales[num].animateTo(
                        1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        (1..9).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { number ->
                    NumberButton(number.toString(), buttonScales[number].value, onNumberClick)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            PadButton(
                onClick = onDeleteClick,
                onLongClick = onClearAll,
                backgroundColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                enabled = pinLength > 0
            ) {
                Icon(
                    painterResource(R.drawable.backspace_24px_fill),
                    contentDescription = "Delete",
                    modifier = Modifier.size(36.dp)
                )
            }
            NumberButton("0", buttonScales[0].value, onNumberClick)
            Spacer(modifier = Modifier.size(80.dp))
        }
    }
}

@Composable
private fun NumberButton(number: String, externalScale: Float, onClick: (String) -> Unit) {
    PadButton(
        onClick = { onClick(number) },
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        externalScale = externalScale
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge,
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PadButton(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    backgroundColor: Color,
    contentColor: Color,
    shape: Shape = CircleShape,
    enabled: Boolean = true,
    externalScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(if (isPressed) 0.85f else 1f, label = "pressScale")
    val alpha by animateFloatAsState(if (enabled) 1f else 0.3f, label = "alpha")

    Surface(
        modifier = Modifier
            .size(80.dp)
            .scale(pressScale * externalScale)
            .alpha(alpha)
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

private const val DELAY_AUTO_SUBMIT = 100L
private const val DELAY_WAVE_STAGGER = 30L

private class RoundedPolygonShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(
        size: Size, layoutDirection: LayoutDirection, density: Density
    ): Outline {
        val path = android.graphics.Path()
        polygon.toPath(path)
        val bounds = RectF()
        path.computeBounds(bounds, true)
        val matrix = Matrix()
        matrix.postTranslate(-bounds.left, -bounds.top)
        val scaleX = size.width / bounds.width()
        val scaleY = size.height / bounds.height()
        matrix.postScale(scaleX, scaleY)
        path.transform(matrix)
        return Outline.Generic(path.asComposePath())
    }
}