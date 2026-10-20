package com.denser.june.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.denser.june.core.domain.model.FontVariationConfig
import com.denser.june.core.domain.model.JournalTheme
import com.denser.june.core.domain.model.enums.ThemeMode
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

@Composable
fun JuneThemeWrapper(
    theme: JournalTheme?,
    font: String? = null,
    fontVariation: FontVariationConfig? = null,
    content: @Composable () -> Unit
) {
    val appTheme = LocalAppTheme.current
    val effectiveSeedColor = theme?.primaryColor?.let { Color(it) } ?: if (appTheme.materialTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorResource(android.R.color.system_accent1_200)
    } else {
        Color(appTheme.seedColor)
    }

    val effectivePaletteStyle = theme?.style?.let {
        runCatching { PaletteStyle.valueOf(it) }.getOrNull()
    } ?: appTheme.style

    val isDark = when (appTheme.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    DynamicMaterialTheme(
        primary = effectiveSeedColor,
        secondary = theme?.secondaryColor?.let { Color(it) },
        tertiary = theme?.tertiaryColor?.let { Color(it) },
        neutral = theme?.neutralColor?.let { Color(it) },
        style = effectivePaletteStyle,
        isDark = isDark,
        isAmoled = appTheme.withAmoled,
        typography = provideTypography(
            font = font ?: appTheme.appFont,
            variation = fontVariation ?: appTheme.appFontVariation
        ),
        content = content
    )
}
