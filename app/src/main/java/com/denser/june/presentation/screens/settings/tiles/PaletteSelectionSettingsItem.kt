package com.denser.june.presentation.screens.settings.tiles

import android.R.color.system_accent1_200
import android.os.Build
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SelectableMiniPalette
import com.denser.june.presentation.theme.LocalAppTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.ktx.from
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.rememberDynamicColorScheme
import org.koin.compose.viewmodel.koinViewModel
import com.denser.june.core.R

@Composable
fun PaletteSelectionSettingsItem() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val currentTheme = LocalAppTheme.current.themeMode
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = remember(currentTheme, systemDark) {
        when (currentTheme) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
    }

    val scrollState = rememberScrollState()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.palette_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    text = stringResource(R.string.palette_style),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                PaletteStyle.entries.forEach { style ->
                    val scheme = rememberDynamicColorScheme(
                        primary = if (state.appTheme.materialTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            colorResource(system_accent1_200)
                        } else {
                            Color(state.appTheme.seedColor)
                        },
                        isDark = when (state.appTheme.themeMode) {
                            ThemeMode.SYSTEM -> isDarkTheme
                            ThemeMode.LIGHT -> false
                            ThemeMode.DARK -> true
                        },
                        isAmoled = state.appTheme.withAmoled,
                        style = style
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectableMiniPalette(
                            selected = state.appTheme.style == style,
                            onClick = {
                                viewModel.onAction(
                                    SettingsAction.OnPaletteChange(style = style)
                                )
                            },
                            contentDescription = { style.name },
                            accents = listOf(
                                TonalPalette.from(scheme.primary),
                                TonalPalette.from(scheme.tertiary),
                                TonalPalette.from(scheme.secondary)
                            )
                        )

                        Text(
                            text = style.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.appTheme.style == style) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
