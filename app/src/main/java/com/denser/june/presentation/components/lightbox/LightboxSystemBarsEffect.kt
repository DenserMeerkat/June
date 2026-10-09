package com.denser.june.presentation.components.lightbox

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat

@Composable
fun LightboxSystemBarsEffect(
    context: Context,
    isDarkTheme: Boolean
) {
    val activity = remember(context) {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) return@remember current
            current = current.baseContext
        }
        null
    }

    DisposableEffect(activity, isDarkTheme) {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val prevLightStatus = insetsController?.isAppearanceLightStatusBars ?: false
        val prevLightNav = insetsController?.isAppearanceLightNavigationBars ?: false

        insetsController?.isAppearanceLightStatusBars = !isDarkTheme
        insetsController?.isAppearanceLightNavigationBars = !isDarkTheme

        val originalColorMode = activity?.window?.colorMode ?: ActivityInfo.COLOR_MODE_DEFAULT
        activity?.window?.colorMode = ActivityInfo.COLOR_MODE_HDR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            try {
                activity?.window?.desiredHdrHeadroom = 10000.0f
            } catch (_: Exception) {}
        }
        onDispose {
            insetsController?.isAppearanceLightStatusBars = prevLightStatus
            insetsController?.isAppearanceLightNavigationBars = prevLightNav
            activity?.window?.colorMode = originalColorMode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                try {
                    activity?.window?.desiredHdrHeadroom = 0f
                } catch (_: Exception) {}
            }
        }
    }
}
