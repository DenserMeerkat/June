package com.denser.june.presentation.screens.settings.screens.lock

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

val LockShakeKeyframes: AnimationSpec<Float> = keyframes {
    durationMillis = 350
    0f at 0
    (-16f) at 50
    16f at 100
    (-10f) at 150
    10f at 200
    (-5f) at 250
    0f at 350
}

@Composable
fun rememberShakeOffset(): Animatable<Float, AnimationVector1D> {
    return remember { Animatable(0f) }
}

suspend fun Animatable<Float, *>.animateLockShake() {
    snapTo(0f)
    animateTo(
        targetValue = 0f,
        animationSpec = LockShakeKeyframes
    )
}

class MorphShape(
    private val morph: Morph,
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = android.graphics.Path()
        morph.toPath(progress, path)
        val bounds = RectF()
        path.computeBounds(bounds, true)
        val matrix = Matrix()
        matrix.postTranslate(-bounds.left, -bounds.top)
        val scaleX = if (bounds.width() > 0f) size.width / bounds.width() else 1f
        val scaleY = if (bounds.height() > 0f) size.height / bounds.height() else 1f
        matrix.postScale(scaleX, scaleY)
        path.transform(matrix)
        return Outline.Generic(path.asComposePath())
    }
}

class RoundedPolygonShape(
    private val polygon: RoundedPolygon
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = android.graphics.Path()
        polygon.toPath(path)
        val bounds = RectF()
        path.computeBounds(bounds, true)
        val matrix = Matrix()
        matrix.postTranslate(-bounds.left, -bounds.top)
        val scaleX = if (bounds.width() > 0f) size.width / bounds.width() else 1f
        val scaleY = if (bounds.height() > 0f) size.height / bounds.height() else 1f
        matrix.postScale(scaleX, scaleY)
        path.transform(matrix)
        return Outline.Generic(path.asComposePath())
    }
}
