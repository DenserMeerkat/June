package com.example.june.core.presentation.screens.settings.section

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ImagesearchRoller
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.june.R
import com.example.june.core.presentation.screens.settings.SettingsAction
import com.example.june.core.presentation.screens.settings.SettingsState
import com.example.june.core.presentation.screens.settings.components.ColorPickerSheet
import com.example.june.core.presentation.screens.settings.components.ThemePickerDialog
import com.example.june.core.presentation.screens.settings.components.FontPickerDialog
import com.example.june.core.presentation.screens.settings.components.PaletteSelectionSettingsItem
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Font

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearnceSection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    var showColorPickerSheet by remember { mutableStateOf(false) }
    var showThemePickerDialog by remember { mutableStateOf(false) }
    var showFontPickerDialog by remember { mutableStateOf(false) }
    SettingSection(
        title = "Appearance"
    ) {
        SettingsItem(
            title = stringResource(R.string.app_theme),
            subtitle = stringResource(state.theme.appTheme.stringRes),
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.Brightness4,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            trailingContent = {
                FilledTonalIconButton(
                    onClick = { showThemePickerDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = "Pick Theme",
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            onClick = { showThemePickerDialog = true }
        )
        SettingsItem(
            title = stringResource(R.string.font),
            subtitle = state.theme.font.fullName,
            leadingContent = {
                Icon(
                    imageVector = FontAwesomeIcons.Solid.Font,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            trailingContent = {
                FilledTonalIconButton(
                    onClick = { showFontPickerDialog = true },
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = "Pick Font",
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            onClick = { showFontPickerDialog = true }
        )
        SettingsItem(
            title = stringResource(R.string.amoled),
            subtitle = stringResource(R.string.amoled_desc),
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            trailingContent = {
                Switch(
                    checked = state.theme.withAmoled,
                    onCheckedChange = {
                        onAction(
                            SettingsAction.OnAmoledSwitch(it)
                        )
                    }
                )
            },
            onClick = {
                onAction(
                    SettingsAction.OnAmoledSwitch(!state.theme.withAmoled)
                )
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingsItem(
                title = stringResource(R.string.material_theme),
                subtitle = stringResource(R.string.material_theme_desc),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.ImagesearchRoller,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                trailingContent = {
                    Switch(
                        checked = state.theme.materialTheme,
                        onCheckedChange = {
                            onAction(
                                SettingsAction.OnMaterialThemeToggle(it)
                            )
                        }
                    )
                },
                onClick = {
                    onAction(
                        SettingsAction.OnMaterialThemeToggle(!state.theme.materialTheme)
                    )
                }
            )
        }
        AnimatedVisibility(
            visible = !state.theme.materialTheme,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SettingsItem(
                title = stringResource(R.string.seed_color),
                subtitle = stringResource(R.string.seed_color_desc),
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Colorize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                trailingContent = {
                    FilledTonalIconButton(
                        onClick = { showColorPickerSheet = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Pick Color",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                onClick = { showColorPickerSheet = true }
            )
        }
        PaletteSelectionSettingsItem(
            state = state,
            onAction = onAction,
            isDarkTheme = isSystemInDarkTheme()
        )
    }

    if (showColorPickerSheet) {
        ColorPickerSheet(
            initialColor = Color(state.theme.seedColor),
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