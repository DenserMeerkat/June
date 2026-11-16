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

        val isHdrSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            context.resources.configuration.isScreenHdr

        val originalColorMode = activity?.window?.colorMode ?: ActivityInfo.COLOR_MODE_DEFAULT
        if (isHdrSupported) {
            try {
                activity?.window?.colorMode = ActivityInfo.COLOR_MODE_HDR
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    activity?.window?.desiredHdrHeadroom = 3.0f
                }
            } catch (_: Exception) {}
        }

        onDispose {
            insetsController?.isAppearanceLightStatusBars = prevLightStatus
            insetsController?.isAppearanceLightNavigationBars = prevLightNav
            if (isHdrSupported) {
                try {
                    activity?.window?.colorMode = originalColorMode
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        activity?.window?.desiredHdrHeadroom = 0f
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
