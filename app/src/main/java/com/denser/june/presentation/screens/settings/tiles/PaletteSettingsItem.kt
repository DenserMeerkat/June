package com.denser.june.presentation.screens.settings.tiles

import android.R.color.system_accent1_200
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.color.ColorTab
import com.denser.june.presentation.components.color.PaletteCard
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.LocalSettingsTriggers
import com.denser.june.presentation.theme.LocalAppTheme
import com.materialkolor.rememberDynamicColorScheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PaletteSettingsItem() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val triggers = LocalSettingsTriggers.current

    val currentTheme = LocalAppTheme.current.themeMode
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = remember(currentTheme, systemDark) {
        when (currentTheme) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
    }

    val effectiveSeedColor = if (state.appTheme.materialTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorResource(system_accent1_200)
    } else {
        Color(state.appTheme.seedColor)
    }

    val scheme = rememberDynamicColorScheme(
        seedColor = effectiveSeedColor,
        isDark = when (state.appTheme.themeMode) {
            ThemeMode.SYSTEM -> isDarkTheme
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        },
        isAmoled = state.appTheme.withAmoled,
        style = state.appTheme.style
    )

    val paletteColors = listOf(
        scheme.primary,
        scheme.primaryContainer,
        scheme.secondary,
        scheme.secondaryContainer,
        scheme.tertiary,
        scheme.tertiaryContainer,
        scheme.surfaceContainerHighest
    )

    val displayName = remember(state.appTheme.style) {
        state.appTheme.style.name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
    }

    JuneConnectedItem(
        title = stringResource(R.string.palette_style),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.palette_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        onClick = { triggers.onThemeSheetClick(ColorTab.PALETTE) }
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        PaletteCard(
            name = displayName,
            colors = paletteColors,
            isSelected = false,
            onClick = { triggers.onThemeSheetClick(ColorTab.PALETTE) },
            modifier = Modifier.padding(start = 40.dp)
        )
    }
}
