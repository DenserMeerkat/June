package com.denser.june.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import coil.ImageLoader
import com.denser.june.core.domain.model.JournalTheme
import com.denser.june.core.media.color.ExtractedCardColors
import com.denser.june.core.media.color.MediaColorEngine
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class DynamicThemeColors(
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color
)

private val dynamicThemeColorsCache = ConcurrentHashMap<Any, DynamicThemeColors>()

@Composable
fun rememberDynamicThemeColors(
    model: Any?,
    theme: JournalTheme? = null
): DynamicThemeColors {
    val context = LocalContext.current

    val fallbackColors = remember {
        MediaColorEngine.extractCardColors(theme = null, palette = null).toDynamicThemeColors()
    }

    if (theme != null) {
        val style = theme.style?.let {
            runCatching { PaletteStyle.valueOf(it) }.getOrNull()
        } ?: if (theme.secondaryColor != null || theme.neutralColor != null) {
            PaletteStyle.Content
        } else {
            PaletteStyle.TonalSpot
        }

        val scheme = rememberDynamicColorScheme(
            primary = Color(theme.primaryColor),
            secondary = theme.secondaryColor?.let { Color(it) },
            tertiary = theme.tertiaryColor?.let { Color(it) },
            neutral = theme.neutralColor?.let { Color(it) },
            style = style,
            isDark = true,
            isAmoled = false
        )

        return remember(scheme) {
            DynamicThemeColors(
                surface = scheme.surfaceContainerLow,
                onSurface = scheme.onSurface,
                onSurfaceVariant = scheme.onSurfaceVariant,
                primaryContainer = scheme.primaryContainer,
                onPrimaryContainer = scheme.onPrimaryContainer,
                secondaryContainer = scheme.secondaryContainer,
                onSecondaryContainer = scheme.onSecondaryContainer
            )
        }
    }

    if (model == null) {
        return fallbackColors
    }

    val cached = remember(model) { dynamicThemeColorsCache[model] }
    var themeColors by remember(model) { mutableStateOf(cached ?: fallbackColors) }

    LaunchedEffect(model) {
        val existing = dynamicThemeColorsCache[model]
        if (existing != null) {
            themeColors = existing
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            val bitmap = loadBitmapForPalette(context, loader, model) ?: return@withContext
            val palette = buildPalette(bitmap)
            val cardColors = MediaColorEngine.extractCardColors(theme = null, palette = palette)
            val extracted = cardColors.toDynamicThemeColors()

            dynamicThemeColorsCache[model] = extracted
            themeColors = extracted
        }
    }

    return themeColors
}

private fun ExtractedCardColors.toDynamicThemeColors() = DynamicThemeColors(
    surface = Color(surface),
    onSurface = Color(onSurface),
    onSurfaceVariant = Color(onSurfaceVariant),
    primaryContainer = Color(primaryContainer),
    onPrimaryContainer = Color(onPrimaryContainer),
    secondaryContainer = Color(secondaryContainer),
    onSecondaryContainer = Color(onSecondaryContainer)
)