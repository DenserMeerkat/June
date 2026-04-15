package com.denser.june.presentation.screens.settings.screens.sync

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.screens.settings.section.SettingSection
import com.denser.june.presentation.screens.settings.section.SettingsItem
import com.denser.june.presentation.screens.settings.screens.sync.sections.SyncAdvancedSection
import com.denser.june.presentation.screens.settings.screens.sync.sections.SyncGeneralSettings
import com.denser.june.presentation.screens.settings.screens.sync.sections.SyncStatusCard
import com.denser.june.presentation.screens.settings.screens.sync.sections.WebDavConfigSection
import com.denser.june.presentation.components.InternetRestrictedBanner
import com.denser.june.presentation.screens.settings.screens.sync.components.SyncDetailsDialog
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.denser.june.core.R

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
                title = { Text("Cloud Sync") },
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
                },
                actions = {
                    AnimatedVisibility(
                        visible = state.isEnabled,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f)
                    ) {
                        Switch(
                            checked = state.isEnabled,
                            onCheckedChange = { syncVM.toggleSync(it) },
                            enabled = state.isInternetAllowed,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .scale(0.85f),
                            thumbContent = {
                                Icon(
                                    painterResource(if (state.isEnabled) R.drawable.cloud_24px else R.drawable.cloud_off_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
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
            if (!state.isInternetAllowed) {
                item {
                    InternetRestrictedBanner(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        description = "Enable internet access to use cloud sync."
                    )
                }
            }
            item {
                Text(
                    text = "Keep your journals in sync across all your devices using a cloud storage provider.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
                )
            }

            item {
                AnimatedVisibility(
                    visible = !state.isEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SettingSection {
                        SettingsItem(
                            title = "Enable Cloud Sync",
                            leadingContent = {
                                Icon(
                                    painterResource(R.drawable.cloud_off_24px),
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = state.isEnabled,
                                    onCheckedChange = { syncVM.toggleSync(it) },
                                    enabled = state.isInternetAllowed
                                )
                            },
                            onClick = { if (state.isInternetAllowed) syncVM.toggleSync(true) }
                        )
                    }
                }
            }

            stickyHeader {
                SyncStatusCard(
                    status = state.status,
                    lastSyncTime = state.lastSyncTime,
                    isVisible = state.isEnabled && state.isInternetAllowed,
                    is24Hour = state.timeFormat == TimeFormat.TWENTY_FOUR_HOUR,
                    horizontalPadding = 16.dp,
                    cornerRadius = 24.dp,
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surface,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            item {
                SyncGeneralSettings(
                    isVisible = state.isEnabled && state.isInternetAllowed,
                    isAutoSyncOn = state.isAutoSyncOn,
                    syncOnlyOnWifi = state.syncOnlyOnWifi,
                    onToggleAutoSync = { syncVM.toggleAutoSync(it) },
                    onToggleWifiOnly = { syncVM.toggleSyncOnlyOnWifi(it) }
                )
            }

            item {
                WebDavConfigSection(
                    isVisible = state.isEnabled && state.isInternetAllowed,
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
                    onManualSync = { if (state.isInternetAllowed) syncVM.manualSync() }
                )
            }

            item {
                SyncAdvancedSection(
                    isVisible = state.isEnabled && state.isInternetAllowed,
                    showAdvancedOptions = state.showAdvancedOptions,
                    isAnalyzing = state.isAnalyzing,
                    status = state.status,
                    analysis = state.analysis,
                    rotationAngle = rotationAngle,
                    onToggleAdvanced = { syncVM.toggleAdvancedOptions() },
                    onAnalyze = { syncVM.analyzeSync() },
                    onRepair = { syncVM.revalidate() },
                    onViewDetails = { syncVM.setShowAnalysisDetails(true) }
                )
            }
        }

        if (state.showAnalysisDetails && state.analysis != null) {
            SyncDetailsDialog(
                analysis = state.analysis!!,
                onDismiss = { syncVM.setShowAnalysisDetails(false) }
            )
        }
    }
}



