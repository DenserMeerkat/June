package com.denser.june.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@Composable
fun JuneFullScreenDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false
    ),
    isDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    windowBackgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    val backgroundColorArgb = windowBackgroundColor.toArgb()
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(backgroundColorArgb))
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDarkTheme
            controller.isAppearanceLightNavigationBars = !isDarkTheme
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.isNavigationBarContrastEnforced = false
        }
        content()
    }
}
