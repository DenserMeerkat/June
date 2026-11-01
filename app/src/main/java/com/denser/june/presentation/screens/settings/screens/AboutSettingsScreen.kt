package com.denser.june.presentation.screens.settings.screens
import com.denser.june.presentation.components.JuneBackButton

import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.components.JuneDialog
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.*
import com.denser.june.presentation.screens.settings.components.sheets.*
import com.denser.june.presentation.utils.InternetDisabledException
import com.denser.june.presentation.utils.UpdateChecker
import android.os.Build
import android.content.pm.PackageManager
import java.security.MessageDigest
import android.widget.Toast
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.text.selection.SelectionContainer
import org.koin.compose.koinInject
import com.denser.june.core.domain.preferences.SyncPreferences
import com.denser.june.BuildConfig

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutSettingsScreen() {
    val navigator = koinInject<AppNavigator>()
    val updateChecker = koinInject<UpdateChecker>()
    val syncPrefs = koinInject<SyncPreferences>()
    val context = LocalContext.current
    var showLicenseSheet by remember { mutableStateOf(false) }
    var showMapCreditsSheet by remember { mutableStateOf(false) }
    var showFontCreditsSheet by remember { mutableStateOf(false) }
    var showAboutLibrariesSheet by remember { mutableStateOf(false) }
    var showChangelogSheet by remember { mutableStateOf(false) }

    var showCheckingUpdatesDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var showNoUpdateDialog by remember { mutableStateOf(false) }
    var updateErrorMsg by remember { mutableStateOf<String?>(null) }
    var showInternetDisabledDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val isDevMode by syncPrefs.isDeveloperModeEnabled().collectAsState(initial = false)
            JuneTopAppBar(
                type = JuneAppBarType.Small,
                scrollBehavior = scrollBehavior,
                title = { Text(text = "About") },
                navigationIcon = {
                    JuneBackButton(onClick = { navigator.navigateBack() })
                },
                actions = {
                    AnimatedVisibility(
                        visible = isDevMode,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        FilledIconButton(
                            onClick = { navigator.navigateTo(Route.DeveloperSettings) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.code_24px),
                                contentDescription = stringResource(R.string.developer_options)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val triggers = remember {
            SettingsTriggers(
                onAboutHeaderClick = { showDiagnosticsDialog = true },
                onLicenseClick = { showLicenseSheet = true },
                onMapAttributionsClick = { showMapCreditsSheet = true },
                onFontCreditsClick = { showFontCreditsSheet = true },
                onAboutLibrariesClick = { showAboutLibrariesSheet = true },
                onChangelogClick = { showChangelogSheet = true },
                onCheckForUpdatesClick = {
                    showCheckingUpdatesDialog = true
                    updateChecker.checkForUpdates(
                        context = context,
                        onUpdateAvailable = { versionName, changelog, downloadUrl ->
                            showCheckingUpdatesDialog = false
                            updateInfo = Triple(versionName, changelog, downloadUrl)
                        },
                        onNoUpdate = {
                            showCheckingUpdatesDialog = false
                            showNoUpdateDialog = true
                        },
                        onError = { throwable ->
                            showCheckingUpdatesDialog = false
                            if (throwable is InternetDisabledException) {
                                showInternetDisabledDialog = true
                            } else {
                                updateErrorMsg = throwable.message ?: "An unknown error occurred"
                            }
                        }
                    )
                }
            )
        }

        CompositionLocalProvider(LocalSettingsTriggers provides triggers) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SettingsTileRegistry.getTile("ABOUT_HEADER")?.content?.invoke()

                        SettingSection {
                            SettingsTileRegistry.getTile("DEVELOPER")?.content?.invoke()
                        }



                        SettingSection {
                            SettingsTileRegistry.getTile("MAP_CREDITS")?.content?.invoke()
                            SettingsTileRegistry.getTile("FONT_CREDITS")?.content?.invoke()
                            SettingsTileRegistry.getTile("ABOUT_LIBRARIES")?.content?.invoke()
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

    if (showFontCreditsSheet) {
        FontCreditsBottomSheet(
            setShowSheet = { showFontCreditsSheet = it }
        )
    }

    if (showAboutLibrariesSheet) {
        AboutLibrariesBottomSheet(
            setShowSheet = { showAboutLibrariesSheet = it }
        )
    }

    if (showChangelogSheet) {
        ChangelogBottomSheet(
            setShowSheet = { showChangelogSheet = it }
        )
    }

    UpdateBottomSheet(
        state = UpdateSheetState(
            showChecking = showCheckingUpdatesDialog,
            updateInfo = updateInfo,
            showNoUpdate = showNoUpdateDialog,
            errorMsg = updateErrorMsg,
            showInternetDisabled = showInternetDisabledDialog
        ),
        onDismissChecking = { showCheckingUpdatesDialog = false },
        onDismissUpdateInfo = { updateInfo = null },
        onDismissNoUpdate = { showNoUpdateDialog = false },
        onDismissError = { updateErrorMsg = null },
        onDismissInternetDisabled = { showInternetDisabledDialog = false },
        navigator = navigator
    )

    if (showDiagnosticsDialog) {
        DiagnosticsBottomSheet(
            onDismissRequest = { showDiagnosticsDialog = false }
        )
    }
}
