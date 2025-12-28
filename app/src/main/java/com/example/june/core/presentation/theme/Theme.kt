package com.example.june.core.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.example.june.core.domain.data_classes.Theme
import com.example.june.core.domain.enums.AppTheme
import com.materialkolor.DynamicMaterialTheme


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JuneTheme(
    theme: Theme,
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val isDarkMode = when (theme.appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as android.app.Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            insetsController.isAppearanceLightStatusBars = !isDarkMode
        }
    }
    DynamicMaterialTheme(
        seedColor = if (theme.materialTheme && Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            colorResource(android.R.color.system_accent1_200)
        } else {
            Color(theme.seedColor)
        },
        isDark = when (theme.appTheme) {
            AppTheme.SYSTEM -> isSystemInDarkTheme()
            AppTheme.LIGHT -> false
            AppTheme.DARK -> true
        },
        isAmoled = theme.withAmoled,
        style = theme.style,
        typography = provideTypography(
            font = theme.font.font,
            scale = fontScale
        ),
        content = content
    )
}