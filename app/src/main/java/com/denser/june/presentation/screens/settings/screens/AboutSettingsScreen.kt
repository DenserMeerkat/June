package com.denser.june.presentation.screens.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showLicenseSheet by remember { mutableStateOf(false) }
    var showMapCreditsSheet by remember { mutableStateOf(false) }
    var showAboutLibrariesSheet by remember { mutableStateOf(false) }
    var showChangelogSheet by remember { mutableStateOf(false) }

    var showCheckingUpdatesDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var showNoUpdateDialog by remember { mutableStateOf(false) }
    var updateErrorMsg by remember { mutableStateOf<String?>(null) }
    var showInternetDisabledDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

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
                onAboutHeaderClick = { showDiagnosticsDialog = true },
                onLicenseClick = { showLicenseSheet = true },
                onMapAttributionsClick = { showMapCreditsSheet = true },
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
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            SettingsTileRegistry.getTile("ABOUT_HEADER")?.content?.invoke()
                        }

                        SettingSection {
                            SettingsTileRegistry.getTile("DEVELOPER")?.content?.invoke()
                        }

                        val isDevMode by syncPrefs.isDeveloperModeEnabled().collectAsState(initial = false)
                        if (isDevMode) {
                            SettingSection {
                                SettingsItem(
                                    title = stringResource(R.string.developer_options),
                                    subtitle = stringResource(R.string.logging_settings_desc),
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.code_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = { navigator.navigateTo(Route.DeveloperSettings) }
                                )
                            }
                        }

                        SettingSection {
                            SettingsTileRegistry.getTile("TRANSLATE")?.content?.invoke()
                            SettingsTileRegistry.getTile("CHANGELOG")?.content?.invoke()
                            SettingsTileRegistry.getTile("CHECK_FOR_UPDATES")?.content?.invoke()
                        }

                        SettingSection {
                            SettingsTileRegistry.getTile("LICENSE")?.content?.invoke()
                            SettingsTileRegistry.getTile("MAP_CREDITS")?.content?.invoke()
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

    UpdateDialogs(
        state = UpdateDialogState(
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
        val packageInfo = remember {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        }
        val sha256 = remember {
            try {
                val signatures = packageInfo.signingInfo?.apkContentsSigners
                if (!signatures.isNullOrEmpty()) {
                    val certBytes = signatures[0].toByteArray()
                    val md = MessageDigest.getInstance("SHA-256")
                    val fingerprintBytes = md.digest(certBytes)
                    fingerprintBytes.joinToString(":") { byte ->
                        String.format("%02X", byte)
                    }
                } else {
                    "No signatures found"
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
        val versionName = remember { packageInfo.versionName ?: "Unknown" }
        val versionCode = remember { packageInfo.longVersionCode }
        val deviceModel = remember { "${Build.MANUFACTURER} ${Build.MODEL}" }
        val androidOS = remember { "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" }
        val supportedAbis = remember { Build.SUPPORTED_ABIS.joinToString(", ") }

        val diagnosticsText = remember(sha256, versionName, versionCode, deviceModel, androidOS, supportedAbis) {
            """
            App Name: June
            Package Name: ${context.packageName}
            Version Name: $versionName
            Version Code: $versionCode
            Signing Key SHA-256: $sha256
            Hyphen Version: ${BuildConfig.HYPHEN_VERSION}
            
            Device: $deviceModel
            Android OS: $androidOS
            Supported ABIs: $supportedAbis
            """.trimIndent()
        }

        JuneDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = stringResource(R.string.diagnostics),
            text = {
                SelectionContainer {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DiagnosticRow(label = stringResource(R.string.pkg_name), value = context.packageName)
                        DiagnosticRow(label = stringResource(R.string.version_name), value = versionName)
                        DiagnosticRow(label = stringResource(R.string.version_code), value = versionCode.toString())
                        DiagnosticRow(label = stringResource(R.string.signing_key_sha256), value = sha256)
                        DiagnosticRow(label = stringResource(R.string.hyphen_version), value = BuildConfig.HYPHEN_VERSION)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        DiagnosticRow(label = stringResource(R.string.device), value = deviceModel)
                        DiagnosticRow(label = stringResource(R.string.android_os), value = androidOS)
                        DiagnosticRow(label = stringResource(R.string.supported_abis), value = supportedAbis)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("June App Diagnostics", diagnosticsText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.content_copy_24px),
                        contentDescription = "Copy",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.copy))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiagnosticsDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
