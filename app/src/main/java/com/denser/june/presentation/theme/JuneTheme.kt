package com.denser.june.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.denser.june.core.domain.model.AppTheme
import com.denser.june.core.domain.model.enums.ThemeMode
import com.materialkolor.DynamicMaterialTheme


val LocalAppTheme = staticCompositionLocalOf<AppTheme> {
    error("No AppTheme provided")
}

val LocalInternetAllowed = staticCompositionLocalOf<Boolean> {
    true
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JuneTheme(
    appTheme: AppTheme,
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val isDarkMode = when (appTheme.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            insetsController.isAppearanceLightStatusBars = !isDarkMode
            insetsController.isAppearanceLightNavigationBars = !isDarkMode
        }
    }
    DynamicMaterialTheme(
        seedColor = if (appTheme.materialTheme && Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            colorResource(android.R.color.system_accent1_200)
        } else {
            Color(appTheme.seedColor)
        },
        isDark = when (appTheme.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        },
        isAmoled = appTheme.withAmoled,
        style = appTheme.style,
        typography = provideTypography(
            font = appTheme.appFont,
            scale = fontScale
        ),
        content = content
    )
}