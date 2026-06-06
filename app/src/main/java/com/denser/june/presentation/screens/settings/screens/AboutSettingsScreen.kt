package com.denser.june.presentation.screens.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.*
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen() {
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showLicenseSheet by remember { mutableStateOf(false) }
    var showMapCreditsSheet by remember { mutableStateOf(false) }
    var showAboutLibrariesSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = "About") },
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
                onLicenseClick = { showLicenseSheet = true },
                onMapAttributionsClick = { showMapCreditsSheet = true },
                onAboutLibrariesClick = { showAboutLibrariesSheet = true }
            )
        }

        CompositionLocalProvider(LocalSettingsTriggers provides triggers) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            SettingsTileRegistry.getTile("ABOUT_HEADER")?.content?.invoke()
                        }

                        SettingSection {
                            SettingsTileRegistry.getTile("DEVELOPER")?.content?.invoke()
                        }

                        SettingSection {
                            SettingsTileRegistry.getTile("LICENSE")?.content?.invoke()
                            SettingsTileRegistry.getTile("ABOUT_LIBRARIES")?.content?.invoke()
                            SettingsTileRegistry.getTile("MAP_CREDITS")?.content?.invoke()
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showLicenseSheet) {
        LicenseBottomSheet(
            setShowSheet = { showLicenseSheet = it }
        )
    }

    if (showMapCreditsSheet) {
        MapCreditsBottomSheet(
            setShowSheet = { showMapCreditsSheet = it }
        )
    }

    if (showAboutLibrariesSheet) {
        AboutLibrariesBottomSheet(
            setShowSheet = { showAboutLibrariesSheet = it }
        )
    }
}
