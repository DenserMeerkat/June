package com.denser.june.presentation.components.lightbox

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.media.exif.model.MediaExifInfo

@Composable
fun LightboxInfoBottomSheet(
    isInfoVisible: Boolean,
    onDismiss: () -> Unit,
    exifInfo: MediaExifInfo?,
    dateTime: Long?,
    parentJournal: Journal?,
    isJournalNavigationEnabled: Boolean,
    isMediaBackedUp: Boolean,
    isPendingSync: Boolean,
    syncProvider: String?,
    isCompressing: Boolean,
    canCompressFurther: Boolean,
    allowCompression: Boolean,
    onJournalClick: () -> Unit,
    onCompressClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenHeight = with(density) { windowInfo.containerSize.height.toDp() }
    val minSheetHeight = screenHeight * 0.65f
    val maxSheetHeight = screenHeight - 56.dp

    var measuredContentHeightPx by remember { mutableIntStateOf(0) }
    val measuredContentHeightDp = if (measuredContentHeightPx > 0) {
        with(density) { measuredContentHeightPx.toDp() }
    } else {
        minSheetHeight
    }

    val targetMaxHeight = minOf(maxSheetHeight, maxOf(minSheetHeight, measuredContentHeightDp))
    val maxExpansionPx = with(density) { (targetMaxHeight - minSheetHeight).toPx() }.coerceAtLeast(0f)

    var sheetExpansionPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(maxExpansionPx) {
        if (sheetExpansionPx > maxExpansionPx) {
            sheetExpansionPx = maxExpansionPx
        }
    }

    val infoScrollState = rememberScrollState()

    val nestedScrollConnection = remember(maxExpansionPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val deltaY = available.y
                if (deltaY < 0f && sheetExpansionPx < maxExpansionPx) {
                    val room = maxExpansionPx - sheetExpansionPx
                    val consumed = minOf(-deltaY, room)
                    sheetExpansionPx += consumed
                    return Offset(0f, -consumed)
                } else if (deltaY > 0f && infoScrollState.value == 0 && sheetExpansionPx > 0f) {
                    val consumed = minOf(deltaY, sheetExpansionPx)
                    sheetExpansionPx -= consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 12f && infoScrollState.value == 0 && sheetExpansionPx <= 0f) {
                    onDismiss()
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(isInfoVisible) {
        if (!isInfoVisible) {
            infoScrollState.scrollTo(0)
            sheetExpansionPx = 0f
        }
    }

    val sheetTop = screenHeight - minSheetHeight
    val currentSheetHeight = minSheetHeight + with(density) { sheetExpansionPx.toDp() }

    val sheetOffsetY by animateDpAsState(
        targetValue = if (isInfoVisible) 0.dp else screenHeight,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "sheetOffsetY"
    )

    if (isInfoVisible) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetTop)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial)
                        val startY = down.position.y
                        var hasMoved = false
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!hasMoved) {
                                    onDismiss()
                                    change.consume()
                                }
                                break
                            }
                            val deltaY = change.position.y - startY
                            if (deltaY > 15f) {
                                onDismiss()
                                change.consume()
                                break
                            }
                            if (deltaY > 8f || deltaY < -8f) {
                                hasMoved = true
                            }
                        }
                    }
                }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(currentSheetHeight)
            .offset { IntOffset(0, sheetOffsetY.roundToPx()) }
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        val handleDrag = remember { floatArrayOf(0f) }
        val sheetDragAccumulator = remember { floatArrayOf(0f) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(infoScrollState)
                .pointerInput(maxExpansionPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial)
                        sheetDragAccumulator[0] = 0f
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val deltaY = change.position.y - change.previousPosition.y
                            if (infoScrollState.value == 0 && deltaY > 0f) {
                                if (sheetExpansionPx > 0f) {
                                    val toShrink = minOf(deltaY, sheetExpansionPx)
                                    sheetExpansionPx -= toShrink
                                } else {
                                    sheetDragAccumulator[0] += deltaY
                                    if (sheetDragAccumulator[0] > 18f) {
                                        onDismiss()
                                        change.consume()
                                        break
                                    }
                                }
                            } else if (deltaY < 0f && sheetExpansionPx < maxExpansionPx) {
                                val room = maxExpansionPx - sheetExpansionPx
                                sheetExpansionPx += minOf(-deltaY, room)
                            }
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(maxExpansionPx) {
                        detectVerticalDragGestures(
                            onDragStart = { handleDrag[0] = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                if (dragAmount < 0f) {
                                    val room = maxExpansionPx - sheetExpansionPx
                                    sheetExpansionPx += minOf(-dragAmount, room)
                                } else if (dragAmount > 0f) {
                                    if (sheetExpansionPx > 0f) {
                                        val toShrink = minOf(dragAmount, sheetExpansionPx)
                                        sheetExpansionPx -= toShrink
                                    } else {
                                        handleDrag[0] += dragAmount
                                        if (handleDrag[0] > 20f) {
                                            onDismiss()
                                        }
                                    }
                                }
                            }
                        )
                    }
            ) {
                LightboxInfoSheetContent(
                    exifInfo = exifInfo,
                    dateTime = dateTime,
                    parentJournal = parentJournal,
                    isJournalNavigationEnabled = isJournalNavigationEnabled,
                    isMediaBackedUp = isMediaBackedUp,
                    isPendingSync = isPendingSync,
                    syncProvider = syncProvider,
                    isCompressing = isCompressing,
                    canCompressFurther = canCompressFurther,
                    allowCompression = allowCompression,
                    onJournalClick = onJournalClick,
                    onCompressClick = onCompressClick,
                    modifier = Modifier.onSizeChanged { size ->
                        if (size.height > 0) {
                            measuredContentHeightPx = size.height
                        }
                    }
                )
            }
        }
    }
}
