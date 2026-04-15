package com.denser.june.presentation.screens.settings.section

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsState
import com.denser.june.presentation.screens.settings.components.ColorPickerSheet
import com.denser.june.presentation.screens.settings.components.ThemePickerDialog
import com.denser.june.presentation.screens.settings.components.FontPickerDialog
import com.denser.june.presentation.screens.settings.components.PaletteSelectionSettingsItem
import com.denser.june.presentation.theme.LocalAppTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    var showColorPickerSheet by remember { mutableStateOf(false) }
    var showThemePickerDialog by remember { mutableStateOf(false) }
    var showFontPickerDialog by remember { mutableStateOf(false) }
    val currentTheme = LocalAppTheme.current.themeMode
    val systemDark = isSystemInDarkTheme()
    val isDarkMode = remember(currentTheme, systemDark) {
        when (currentTheme) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
    }
    SettingSection(
        title = "Appearance"
    ) {
        SettingsItem(
            title = stringResource(R.string.app_theme),
            subtitle = stringResource(state.appTheme.themeMode.stringRes),
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.routine_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            onClick = { showThemePickerDialog = true }
        )
        SettingsItem(
            title = stringResource(R.string.font),
            subtitle = state.appTheme.font.fullName,
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.format_size_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            onClick = { showFontPickerDialog = true }
        )
        SettingsItem(
            title = stringResource(R.string.amoled),
            subtitle = stringResource(R.string.amoled_desc),
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.partly_cloudy_night_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            trailingContent = {
                Switch(
                    checked = state.appTheme.withAmoled,
                    onCheckedChange = {
                        onAction(
                            SettingsAction.OnAmoledSwitch(it)
                        )
                    }
                )
            },
            onClick = {
                onAction(
                    SettingsAction.OnAmoledSwitch(!state.appTheme.withAmoled)
                )
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingsItem(
                title = stringResource(R.string.material_theme),
                subtitle = stringResource(R.string.material_theme_desc),
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.imagesearch_roller_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                trailingContent = {
                    Switch(
                        checked = state.appTheme.materialTheme,
                        onCheckedChange = {
                            onAction(
                                SettingsAction.OnMaterialThemeToggle(it)
                            )
                        }
                    )
                },
                onClick = {
                    onAction(
                        SettingsAction.OnMaterialThemeToggle(!state.appTheme.materialTheme)
                    )
                }
            )
        }
        AnimatedVisibility(
            visible = !state.appTheme.materialTheme,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SettingsItem(
                title = stringResource(R.string.seed_color),
                subtitle = stringResource(R.string.seed_color_desc),
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.colors_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                onClick = { showColorPickerSheet = true }
            )
        }
        PaletteSelectionSettingsItem(
            state = state,
            onAction = onAction,
            isDarkTheme = isDarkMode
        )
    }

    if (showColorPickerSheet) {
        ColorPickerSheet(
            initialColor = Color(state.appTheme.seedColor),
            onSelect = { onAction(SettingsAction.OnSeedColorChange(it.toArgb())) },
            onDismiss = { showColorPickerSheet = false }
        )
    }
    if (showThemePickerDialog) {
        ThemePickerDialog(
            state = state,
            onAction = onAction,
            onDismiss = { showThemePickerDialog = false }
        )
    }
    if (showFontPickerDialog) {
        FontPickerDialog(
            state = state,
            onAction = onAction,
            onDismiss = { showFontPickerDialog = false }
        )
    }
}