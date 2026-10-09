package com.denser.june.presentation.components.lightbox

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import coil.ImageLoader
import com.denser.june.core.media.storage.MediaStorageManager
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import kotlin.math.abs

@Composable
fun LightboxZoomableImage(
    path: String,
    imageLoader: ImageLoader,
    refreshKey: Int,
    onTap: () -> Unit,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    key(path, refreshKey) {
        val context = LocalContext.current
        val zoomableState = rememberZoomableState()
        val zoomState = rememberZoomableImageState(zoomableState)
        val resolvedFile = remember(path) { MediaStorageManager.resolveMediaFile(context, path) }
        val viewConfig = LocalViewConfiguration.current
        val touchSlop = viewConfig.touchSlop

        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(zoomableState.zoomFraction, touchSlop) {
                    if (zoomableState.zoomFraction == 0f) {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial)
                            val startX = down.position.x
                            val startY = down.position.y
                            val verticalThreshold = touchSlop * 0.4f
                            val horizontalBreakoutThreshold = touchSlop * 2.5f

                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break

                                val totalDx = change.position.x - startX
                                val totalDy = change.position.y - startY

                                if (totalDy < -verticalThreshold && abs(totalDy) > abs(totalDx) * 0.7f) {
                                    onSwipeUp()
                                    change.consume()
                                    break
                                }

                                if (abs(totalDx) > horizontalBreakoutThreshold && abs(totalDx) > abs(totalDy) * 1.5f && totalDy >= -verticalThreshold) {
                                    break
                                }
                            }
                        }
                    }
                }
        ) {
            ZoomableAsyncImage(
                model = resolvedFile,
                imageLoader = imageLoader,
                state = zoomState,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                onClick = { onTap() }
            )
        }
    }
}
