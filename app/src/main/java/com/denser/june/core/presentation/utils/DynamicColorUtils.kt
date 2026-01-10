package com.denser.june.core.presentation.utils

import android.graphics.drawable.BitmapDrawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.toColorInt

data class DynamicThemeColors(
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color
)

@Composable
fun rememberDynamicThemeColors(
    model: Any?
): DynamicThemeColors {
    val context = LocalContext.current

    val defaultColors = DynamicThemeColors(
        surface = MaterialTheme.colorScheme.surface,
        onSurface = MaterialTheme.colorScheme.onSurface,
        onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant,
        primaryContainer = MaterialTheme.colorScheme.primaryContainer,
        onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer,
        secondaryContainer = MaterialTheme.colorScheme.secondaryContainer,
        onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer
    )

    var themeColors by remember { mutableStateOf(defaultColors) }

    LaunchedEffect(model) {
        if (model == null) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(model)
                .allowHardware(false)
                .build()

            val bitmap = when (val result = loader.execute(request)) {
                is SuccessResult -> (result.drawable as? BitmapDrawable)?.bitmap
                else -> null
            }

            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()

                val dominantInt = palette.getDominantColor(android.graphics.Color.DKGRAY)
                val surfaceInt = ColorUtils.blendARGB(dominantInt, android.graphics.Color.BLACK, 0.85f)

                val vibrantSwatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.darkVibrantSwatch

                val accentInt = if (vibrantSwatch != null) {
                    val hsl = floatArrayOf(0f, 0f, 0f)
                    ColorUtils.colorToHSL(vibrantSwatch.rgb, hsl)
                    if (hsl[1] < 0.25f) "#E0E0E0".toColorInt()
                    else vibrantSwatch.rgb
                } else {
                    "#E0E0E0".toColorInt()
                }

                val onAccentInt = if (ColorUtils.calculateLuminance(accentInt) > 0.5) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }

                themeColors = DynamicThemeColors(
                    surface = Color(surfaceInt),
                    onSurface = Color.White, 
                    onSurfaceVariant = Color(0xFFD1D1D1), 
                    primaryContainer = Color(accentInt),
                    onPrimaryContainer = Color(onAccentInt),
                    secondaryContainer = Color(0x33FFFFFF), 
                    onSecondaryContainer = Color.White
                )
            }
        }
    }

    return themeColors
}