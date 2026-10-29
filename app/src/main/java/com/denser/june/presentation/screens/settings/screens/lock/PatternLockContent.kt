package com.denser.june.presentation.screens.settings.screens.lock
import com.denser.june.presentation.components.JuneBackButton

import android.view.SoundEffectConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sqrt

private const val MIN_PATTERN_LENGTH = 4
private const val ERROR_DISPLAY_DURATION = 500L

@Composable
fun PatternLockContent(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.enter_pattern),
    isError: Boolean = false,
    errorText: String = stringResource(R.string.wrong_pin_try_again),
    onErrorDismissed: () -> Unit = {},
    onForgotPattern: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onPatternChange: ((List<Int>) -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    onPatternSubmitted: (List<Int>) -> Unit
) {
    var selectedDots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPos by remember { mutableStateOf<Offset?>(null) }
    var localErrorText by remember { mutableStateOf<String?>(null) }
    var isLocalError by remember { mutableStateOf(false) }

    val shakeOffset = rememberShakeOffset()
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val showingError = isError || isLocalError
    val currentErrorText = localErrorText ?: errorText

    fun clearAllErrors() {
        if (isLocalError) {
            isLocalError = false
            localErrorText = null
        }
        if (isError) {
            onErrorDismissed()
        }
    }

    LaunchedEffect(title) {
        selectedDots = emptyList()
        currentTouchPos = null
        onPatternChange?.invoke(emptyList())
    }

    LaunchedEffect(isError) {
        if (isError) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
            shakeOffset.animateLockShake()
            delay(ERROR_DISPLAY_DURATION)
            selectedDots = emptyList()
            currentTouchPos = null
            onPatternChange?.invoke(emptyList())
            onErrorDismissed()
        }
    }

    LaunchedEffect(isLocalError) {
        if (isLocalError) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
            shakeOffset.animateLockShake()
            delay(ERROR_DISPLAY_DURATION)
            selectedDots = emptyList()
            currentTouchPos = null
            onPatternChange?.invoke(emptyList())
            isLocalError = false
            localErrorText = null
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isSmallScreen = maxHeight < 640.dp
        val gridWidth = if (maxWidth < 360.dp) 260.dp else if (maxWidth < 420.dp) 300.dp else 320.dp

        val topSpacing = if (isSmallScreen) 16.dp else 40.dp
        val iconSpacing = if (isSmallScreen) 12.dp else 24.dp
        val headerToIndicatorSpacing = if (isSmallScreen) 16.dp else 36.dp
        val indicatorToInputSpacing = if (isSmallScreen) 28.dp else 56.dp

        if (onBackClick != null) {
            JuneBackButton(
                onClick = onBackClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 16.dp)
                    .align(Alignment.TopStart)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(topSpacing))

            LockHeader(
                title = title,
                isError = showingError,
                errorText = currentErrorText,
                iconSpacing = iconSpacing
            )

            Spacer(modifier = Modifier.height(headerToIndicatorSpacing))

            Column(
                modifier = Modifier
                    .offset(x = shakeOffset.value.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PatternIndicator(
                    selectedDots = selectedDots,
                    isError = showingError
                )

                Spacer(modifier = Modifier.height(indicatorToInputSpacing))

                Box(modifier = Modifier.size(gridWidth)) {
                    PatternGrid(
                        gridSize = gridWidth,
                        selectedDots = selectedDots,
                        currentTouchPos = currentTouchPos,
                        isError = showingError,
                        onStartTouch = {
                            if (showingError) {
                                clearAllErrors()
                                selectedDots = emptyList()
                                onPatternChange?.invoke(emptyList())
                            }
                        },
                        onPatternUpdated = { updatedList ->
                            selectedDots = updatedList
                            onPatternChange?.invoke(updatedList)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onTouchMove = { offset ->
                            currentTouchPos = offset
                        },
                        onTouchRelease = { finalPattern ->
                            currentTouchPos = null
                            if (finalPattern.isEmpty()) return@PatternGrid

                            if (bottomBar == null) {
                                if (finalPattern.size < MIN_PATTERN_LENGTH) {
                                    localErrorText = "Connect at least $MIN_PATTERN_LENGTH dots"
                                    isLocalError = true
                                } else {
                                    onPatternSubmitted(finalPattern)
                                }
                            } else {
                                if (finalPattern.size < MIN_PATTERN_LENGTH) {
                                    localErrorText = "Connect at least $MIN_PATTERN_LENGTH dots"
                                    isLocalError = true
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(topSpacing))
            Spacer(modifier = Modifier.weight(1f, fill = false))
        }

        if (onBackClick != null) {
            JuneBackButton(
                onClick = onBackClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 16.dp)
                    .align(Alignment.TopStart)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            if (bottomBar != null) {
                bottomBar()
            } else if (onForgotPattern != null) {
                LockBottomBar(
                    onLeftClick = onForgotPattern,
                    leftIcon = R.drawable.passkey_24px,
                    leftText = null
                )
            }
        }
    }
}

@Composable
private fun PatternGrid(
    gridSize: Dp,
    selectedDots: List<Int>,
    currentTouchPos: Offset?,
    isError: Boolean,
    onStartTouch: () -> Unit,
    onPatternUpdated: (List<Int>) -> Unit,
    onTouchMove: (Offset) -> Unit,
    onTouchRelease: (List<Int>) -> Unit
) {
    val density = LocalDensity.current
    val sizePx = with(density) { gridSize.toPx() }
    val paddingPx = with(density) { 36.dp.toPx() }
    val stepPx = (sizePx - 2 * paddingPx) / 2f
    val hitRadiusPx = with(density) { 36.dp.toPx() }

    val dotCenters = remember(sizePx, paddingPx, stepPx) {
        (0..8).map { index ->
            val row = index / 3
            val col = index % 3
            Offset(paddingPx + col * stepPx, paddingPx + row * stepPx)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val lineColor = if (isError) errorColor else primaryColor
    val strokeWidthPx = with(density) { 24.dp.toPx() }

    Box(
        modifier = Modifier
            .size(gridSize)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onStartTouch()
                    val activePattern = mutableListOf<Int>()
                    onTouchMove(down.position)

                    val hitIndex = findDotIndex(down.position, dotCenters, hitRadiusPx)
                    if (hitIndex != null) {
                        activePattern.add(hitIndex)
                        onPatternUpdated(activePattern.toList())
                    }

                    do {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (pointer.pressed) {
                            onTouchMove(pointer.position)
                            val moveHitIndex = findDotIndex(pointer.position, dotCenters, hitRadiusPx)
                            if (moveHitIndex != null && moveHitIndex !in activePattern) {
                                if (activePattern.isNotEmpty()) {
                                    val lastDot = activePattern.last()
                                    val bridge = getIntermediateDot(lastDot, moveHitIndex, activePattern)
                                    if (bridge != null && bridge !in activePattern) {
                                        activePattern.add(bridge)
                                    }
                                }
                                activePattern.add(moveHitIndex)
                                onPatternUpdated(activePattern.toList())
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    onTouchRelease(activePattern.toList())
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (selectedDots.size > 1) {
                for (i in 0 until selectedDots.size - 1) {
                    val p1 = dotCenters[selectedDots[i]]
                    val p2 = dotCenters[selectedDots[i + 1]]
                    drawLine(
                        color = lineColor,
                        start = p1,
                        end = p2,
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }
            }

            if (selectedDots.isNotEmpty() && currentTouchPos != null) {
                val lastCenter = dotCenters[selectedDots.last()]
                drawLine(
                    color = lineColor.copy(alpha = 0.65f),
                    start = lastCenter,
                    end = currentTouchPos,
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            for (idx in selectedDots) {
                drawCircle(
                    color = lineColor,
                    radius = strokeWidthPx / 2f,
                    center = dotCenters[idx]
                )
            }
        }

        for (index in 0..8) {
            val center = dotCenters[index]
            val isSelected = index in selectedDots
            val isLastSelected = selectedDots.lastOrNull() == index
            val dotCenterDpX = with(density) { center.x.toDp() }
            val dotCenterDpY = with(density) { center.y.toDp() }

            Box(
                modifier = Modifier
                    .offset(x = dotCenterDpX - 24.dp, y = dotCenterDpY - 24.dp)
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                PatternDot(
                    isSelected = isSelected,
                    isLastSelected = isLastSelected,
                    isError = isError
                )
            }
        }
    }
}

@Composable
private fun PatternDot(
    isSelected: Boolean,
    isLastSelected: Boolean,
    isError: Boolean
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            if (isLastSelected) {
                scale.animateTo(1.35f, tween(100, easing = FastOutSlowInEasing))
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            } else {
                scale.snapTo(1f)
            }
        } else {
            scale.snapTo(1f)
        }
    }

    val nodeCenterColor = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(scale.value),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(nodeCenterColor)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                )
            }
        }
    }
}

private fun findDotIndex(pos: Offset, dotCenters: List<Offset>, hitRadiusPx: Float): Int? {
    for (i in dotCenters.indices) {
        val center = dotCenters[i]
        val dx = pos.x - center.x
        val dy = pos.y - center.y
        if (sqrt((dx * dx + dy * dy).toDouble()) <= hitRadiusPx) {
            return i
        }
    }
    return null
}

private fun getIntermediateDot(from: Int, to: Int, selected: List<Int>): Int? {
    val row1 = from / 3
    val col1 = from % 3
    val row2 = to / 3
    val col2 = to % 3

    val dRow = row2 - row1
    val dCol = col2 - col1

    if (abs(dRow) % 2 == 0 && abs(dCol) % 2 == 0) {
        val midRow = (row1 + row2) / 2
        val midCol = (col1 + col2) / 2
        val midIndex = midRow * 3 + midCol
        if (midIndex !in selected) {
            return midIndex
        }
    }
    return null
}
