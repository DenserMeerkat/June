package com.denser.june.presentation.screens.settings.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.JournalTheme
import com.denser.june.presentation.components.color.ColorCustomizationPanel
import com.denser.june.presentation.components.color.ColorScreenTab
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.materialkolor.PaletteStyle
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizationSheet(
    initialTab: ColorScreenTab = ColorScreenTab.COLOR,
    onDismiss: () -> Unit
) {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentJournalTheme = if (state.appTheme.materialTheme && isDynamicSupported) {
        null
    } else {
        JournalTheme(
            primaryColor = state.appTheme.seedColor,
            style = state.appTheme.style.name
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )

            ColorCustomizationPanel(
                journalTheme = currentJournalTheme,
                onThemeChange = { newTheme ->
                    if (newTheme != null) {
                        val isMaterial = state.appTheme.materialTheme
                        val colorChanged = if (isMaterial) {
                            newTheme.secondaryColor != null || newTheme.tertiaryColor != null
                        } else {
                            newTheme.primaryColor != state.appTheme.seedColor
                        }

                        if (colorChanged && isMaterial) {
                            viewModel.onAction(SettingsAction.OnMaterialThemeToggle(false))
                        }
                        if (!isMaterial && newTheme.primaryColor != state.appTheme.seedColor) {
                            viewModel.onAction(SettingsAction.OnSeedColorChange(newTheme.primaryColor))
                        }
                        newTheme.style?.let { styleName ->
                            runCatching { PaletteStyle.valueOf(styleName) }.getOrNull()?.let { style ->
                                if (style != state.appTheme.style) {
                                    viewModel.onAction(SettingsAction.OnPaletteChange(style))
                                }
                            }
                        }
                    }
                },
                contentHeight = 400.dp,
                initialTab = initialTab,
                toggleTitle = if (isDynamicSupported) stringResource(R.string.material_theme) else null,
                toggleChecked = if (isDynamicSupported) state.appTheme.materialTheme else null,
                onToggleChange = if (isDynamicSupported) {
                    { enabled -> viewModel.onAction(SettingsAction.OnMaterialThemeToggle(enabled)) }
                } else null,
                allowPaletteWhenDefault = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
