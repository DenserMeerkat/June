package com.denser.june.presentation.screens.settings.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.*
import com.denser.june.presentation.theme.LocalAppTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.denser.june.core.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen() {
    val settingsVM: SettingsVM = koinViewModel()
    val state by settingsVM.state.collectAsStateWithLifecycle()
    val onAction = settingsVM::onAction
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showColorPickerSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = "Appearance") },
                navigationIcon = {
                    FilledIconButton(
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Back",
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val triggers = remember {
            SettingsTriggers(
                onColorPickerClick = { showColorPickerSheet = true }
            )
        }

        CompositionLocalProvider(LocalSettingsTriggers provides triggers) {
            val appearanceTiles = SettingsTileRegistry.getTilesForCategory("Appearance").associateBy { it.key }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val themeGroup = listOfNotNull(
                    appearanceTiles["APP_THEME"],
                    appearanceTiles["AMOLED"]
                )
                if (themeGroup.isNotEmpty()) {
                    item {
                        SettingSection {
                            themeGroup.forEach { it.content() }
                        }
                    }
                }

                val colorGroupKeys = listOf("MATERIAL_THEME", "SEED_COLOR", "PALETTE_SELECTION")
                val colorGroup = colorGroupKeys.mapNotNull { appearanceTiles[it] }
                if (colorGroup.isNotEmpty()) {
                    item {
                        SettingSection {
                            colorGroup.forEach { tile ->
                                if (tile.key == "SEED_COLOR") {
                                    AnimatedVisibility(
                                        visible = !state.appTheme.materialTheme,
                                        enter = expandVertically(),
                                        exit = shrinkVertically()
                                    ) {
                                        tile.content()
                                    }
                                } else {
                                    tile.content()
                                }
                            }
                        }
                    }
                }

                val fontTile = appearanceTiles["APP_FONT"]
                if (fontTile != null) {
                    item {
                        SettingSection {
                            fontTile.content()
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showColorPickerSheet) {
        ColorPickerSheet(
            initialColor = Color(state.appTheme.seedColor),
            onSelect = { onAction(SettingsAction.OnSeedColorChange(it.toArgb())) },
            onDismiss = { showColorPickerSheet = false }
        )
    }
}
