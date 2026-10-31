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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.components.JuneBackButton
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.*
import com.denser.june.presentation.screens.settings.components.sheets.*
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.denser.june.core.R
import com.denser.june.presentation.components.color.ColorTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen() {
    val settingsVM: SettingsVM = koinViewModel()
    val state by settingsVM.state.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var themeSheetTab by remember { mutableStateOf<ColorTab?>(null) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.appearance)) },
                navigationIcon = {
                    JuneBackButton(onClick = { navigator.navigateBack() })
                }
            )
        }
    ) { innerPadding ->
        val triggers = remember {
            SettingsTriggers(
                onThemeSheetClick = { themeSheetTab = it }
            )
        }

        CompositionLocalProvider(LocalSettingsTriggers provides triggers) {
            val appearanceTiles = SettingsTileRegistry.getTilesForCategory("Appearance").associateBy { it.key }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
            ) {
                item {
                    val isDarkTheme = when (state.appTheme.themeMode) {
                        ThemeMode.DARK -> true
                        ThemeMode.LIGHT -> false
                        ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    }

                    SettingSection {
                        appearanceTiles["APP_THEME"]?.content?.invoke()
                        AnimatedVisibility(
                            visible = isDarkTheme,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            appearanceTiles["AMOLED"]?.content?.invoke()
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
                val fontAndLangGroup = listOfNotNull(appearanceTiles["APP_FONT"], appearanceTiles["LANGUAGE"], appearanceTiles["FORCE_LTR_APP_LAYOUT"])
                if (fontAndLangGroup.isNotEmpty()) {
                    item {
                        SettingSection {
                            fontAndLangGroup.forEach { it.content() }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    themeSheetTab?.let { tab ->
        ThemeCustomizationSheet(
            initialTab = tab,
            onDismiss = { themeSheetTab = null }
        )
    }
}
