package com.denser.june.presentation.screens.settings.screens.sync

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.screens.settings.section.SettingSection
import com.denser.june.presentation.screens.settings.section.SettingsItem
import com.denser.june.presentation.screens.settings.screens.sync.sections.SyncAdvancedSection
import com.denser.june.presentation.screens.settings.screens.sync.sections.SyncGeneralSettings
import com.denser.june.presentation.screens.settings.screens.sync.sections.SyncStatusCard
import com.denser.june.presentation.screens.settings.screens.sync.sections.WebDavConfigSection
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SyncScreen() {
    val syncVM: SyncVM = koinViewModel()
    val state by syncVM.state.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val lazyListState = rememberLazyListState()

    val rotationAngle by animateFloatAsState(
        targetValue = if (state.showAdvancedOptions) 180f else 0f,
        label = "Caret Rotation"
    )

    val isStuck by remember {
        derivedStateOf { 
            lazyListState.firstVisibleItemIndex > 1 || 
            (lazyListState.firstVisibleItemIndex == 1 && lazyListState.firstVisibleItemScrollOffset > 0)
        }
    }

    val hPadding by animateDpAsState(
        targetValue = if (isStuck) 0.dp else 16.dp,
        label = "Sticky Padding"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isStuck) 0.dp else 24.dp,
        label = "Sticky Corners"
    )

    LaunchedEffect(Unit) {
        syncVM.effect.collect { effect ->
            when (effect) {
                is SyncEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                scrollBehavior = scrollBehavior,
                title = { Text("Sync & Backup") },
                navigationIcon = {
                    FilledIconButton(
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    text = "Keep your journals in sync across all your devices using a cloud storage provider.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
                )
            }

            item {
                SettingSection {
                    SettingsItem(
                        title = "Cloud Sync",
                        subtitle = (if (state.isEnabled) "Enabled" else "Disabled"),
                        leadingContent = {
                            Icon(
                                painterResource(if (state.isEnabled) R.drawable.cloud_24px else R.drawable.cloud_off_24px),
                                null,
                                tint = if (state.isEnabled) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.isEnabled,
                                onCheckedChange = { syncVM.toggleSync(it) }
                            )
                        }
                    )
                }
            }

            stickyHeader {
                SyncStatusCard(
                    status = state.status,
                    lastSyncTime = state.lastSyncTime,
                    isVisible = state.isEnabled,
                    horizontalPadding = hPadding,
                    cornerRadius = cornerRadius,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                )
            }

            item {
                SyncGeneralSettings(
                    isVisible = state.isEnabled,
                    isAutoSyncOn = state.isAutoSyncOn,
                    syncOnlyOnWifi = state.syncOnlyOnWifi,
                    onToggleAutoSync = { syncVM.toggleAutoSync(it) },
                    onToggleWifiOnly = { syncVM.toggleSyncOnlyOnWifi(it) }
                )
            }

            item {
                WebDavConfigSection(
                    isVisible = state.isEnabled,
                    webDavUrl = state.webDavUrl,
                    webDavUser = state.webDavUser,
                    webDavPass = state.webDavPass,
                    isConfigLocked = state.isConfigLocked,
                    isTestingConnection = state.isTestingConnection,
                    status = state.status,
                    urlError = state.webDavUrlError,
                    userError = state.webDavUserError,
                    passError = state.webDavPassError,
                    onUrlChange = { syncVM.updateUrl(it) },
                    onUserChange = { syncVM.updateUser(it) },
                    onPassChange = { syncVM.updatePass(it) },
                    onToggleLock = { syncVM.toggleConfigLock() },
                    onTestConnection = { syncVM.testConnection() },
                    onManualSync = { syncVM.manualSync() }
                )
            }

            item {
                SyncAdvancedSection(
                    isVisible = state.isEnabled,
                    showAdvancedOptions = state.showAdvancedOptions,
                    isAnalyzing = state.isAnalyzing,
                    status = state.status,
                    analysis = state.analysis,
                    rotationAngle = rotationAngle,
                    onToggleAdvanced = { syncVM.toggleAdvancedOptions() },
                    onAnalyze = { syncVM.analyzeSync() },
                    onRepair = { syncVM.revalidate() }
                )
            }
        }
    }
}



