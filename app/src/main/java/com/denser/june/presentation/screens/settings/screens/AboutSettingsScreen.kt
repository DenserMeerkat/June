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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutSettingsScreen() {
    val navigator = koinInject<AppNavigator>()
    val updateChecker = koinInject<UpdateChecker>()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
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

                        SettingSection {
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

    if (showCheckingUpdatesDialog) {
        JuneDialog(
            onDismissRequest = { showCheckingUpdatesDialog = false },
            title = "Checking for updates",
            confirmButton = {},
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularWavyProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    updateInfo?.let { (versionName, changelog, downloadUrl) ->
        JuneDialog(
            onDismissRequest = { updateInfo = null },
            title = "Update Available ($versionName)",
            text = {
                Column {
                    Text(
                        text = "A new version of June is available. Here is what's new:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = changelog,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        updateInfo = null
                        if (downloadUrl.isNotBlank()) {
                            uriHandler.openUri(downloadUrl)
                        }
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) {
                    Text("Later")
                }
            }
        )
    }

    if (showNoUpdateDialog) {
        JuneDialog(
            onDismissRequest = { showNoUpdateDialog = false },
            title = "Up to date",
            text = { Text("You are already using the latest version of June.") },
            confirmButton = {
                Button(onClick = { showNoUpdateDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showInternetDisabledDialog) {
        JuneDialog(
            onDismissRequest = { showInternetDisabledDialog = false },
            title = "Internet Access Disabled",
            text = { Text("Internet access is disabled in June's privacy settings. Please enable it under Permissions to check for updates.") },
            confirmButton = {
                Button(
                    onClick = {
                        showInternetDisabledDialog = false
                        navigator.navigateTo(Route.Permissions)
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInternetDisabledDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    updateErrorMsg?.let { errorMsg ->
        JuneDialog(
            onDismissRequest = { updateErrorMsg = null },
            title = "Update Check Failed",
            text = { Text(errorMsg) },
            confirmButton = {
                Button(onClick = { updateErrorMsg = null }) {
                    Text("Okay")
                }
            }
        )
    }

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
            
            Device: $deviceModel
            Android OS: $androidOS
            Supported ABIs: $supportedAbis
            """.trimIndent()
        }

        JuneDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = "App Diagnostics",
            text = {
                SelectionContainer {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DiagnosticRow(label = "Package Name", value = context.packageName)
                        DiagnosticRow(label = "Version Name", value = versionName)
                        DiagnosticRow(label = "Version Code", value = versionCode.toString())
                        DiagnosticRow(label = "Signing Key SHA-256", value = sha256)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        DiagnosticRow(label = "Device", value = deviceModel)
                        DiagnosticRow(label = "Android OS", value = androidOS)
                        DiagnosticRow(label = "Supported ABIs", value = supportedAbis)
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
                    Text("Copy")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiagnosticsDialog = false }) {
                    Text("Close")
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
