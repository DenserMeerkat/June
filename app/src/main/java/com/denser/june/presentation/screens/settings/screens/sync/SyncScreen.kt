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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.SettingsItem
import com.denser.june.presentation.screens.settings.screens.sync.sections.SyncAdvancedSection
import com.denser.june.presentation.screens.settings.screens.sync.sections.SyncGeneralSettings
import com.denser.june.presentation.screens.settings.screens.sync.sections.SyncStatusCard
import com.denser.june.presentation.screens.settings.screens.sync.sections.WebDavConfigSection
import com.denser.june.presentation.screens.settings.screens.sync.sections.GoogleDriveConfigSection
import com.denser.june.presentation.components.InternetRestrictedBanner
import com.denser.june.presentation.screens.settings.screens.sync.components.SyncDetailsDialog
import com.denser.june.presentation.theme.LocalInternetAllowed
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.denser.june.core.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.denser.june.presentation.components.JuneConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SyncScreen() {
    val syncVM: SyncVM = koinViewModel()
    val state by syncVM.state.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val lazyListState = rememberLazyListState()
    val isInternetAllowed = LocalInternetAllowed.current

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
                title = { Text(stringResource(R.string.cloud_sync)) },
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
                            enabled = isInternetAllowed,
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
                .padding(top = innerPadding.calculateTopPadding())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 24.dp + innerPadding.calculateBottomPadding())
        ) {
            if (!isInternetAllowed) {
                item {
                    InternetRestrictedBanner(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        description = stringResource(R.string.sync_internet_required_desc)
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.sync_header_desc),
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
                            title = stringResource(R.string.enable_cloud_sync),
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
                                    enabled = isInternetAllowed
                                )
                            },
                            onClick = { if (isInternetAllowed) syncVM.toggleSync(true) }
                        )
                    }
                }
            }

            stickyHeader {
                SyncStatusCard(
                    status = state.status,
                    lastSyncTime = state.lastSyncTime,
                    isVisible = state.isEnabled && isInternetAllowed,
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
                    isVisible = state.isEnabled && isInternetAllowed,
                    isAutoSyncOn = state.isAutoSyncOn,
                    syncOnlyOnWifi = state.syncOnlyOnWifi,
                    onToggleAutoSync = { syncVM.toggleAutoSync(it) },
                    onToggleWifiOnly = { syncVM.toggleSyncOnlyOnWifi(it) }
                )
            }

            if (state.isEnabled && isInternetAllowed && state.availableProviders.size > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.availableProviders.forEach { provider ->
                            val isSelected = state.selectedProvider == provider
                            val providerDisplayName = if (provider == "GoogleDrive") "Google Drive" else provider
                            val iconRes = if (provider == "GoogleDrive") R.drawable.drive_export_24px else R.drawable.cloud_24px

                            Surface(
                                onClick = { syncVM.selectProvider(provider) },
                                shape = RoundedCornerShape(24.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = providerDisplayName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                        )
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            painter = painterResource(R.drawable.check_circle_24px_fill),
                                            contentDescription = "Active",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.isEnabled && isInternetAllowed) {
                item {
                    AnimatedContent(
                        targetState = state.selectedProvider,
                        transitionSpec = {
                            val providers = state.availableProviders
                            val initialIndex = providers.indexOf(initialState)
                            val targetIndex = providers.indexOf(targetState)
                            
                            if (targetIndex < initialIndex) {
                                (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> width } + fadeOut())
                            } else {
                                (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> -width } + fadeOut())
                            }.using(
                                SizeTransform(clip = false)
                            )
                        },
                        label = "ConfigSectionTransition"
                    ) { provider ->
                        when (provider) {
                            "GoogleDrive" -> {
                                GoogleDriveConfigSection(
                                    isVisible = true,
                                    isConnected = state.isGoogleDriveConnected,
                                    status = state.status,
                                    isTestingConnection = state.isTestingConnection,
                                    onTestConnection = { syncVM.testConnection() },
                                    onManualSync = { if (isInternetAllowed) syncVM.manualSync() },
                                    onRefresh = { syncVM.analyzeSync() },
                                    onStopSync = { syncVM.stopSync() }
                                )
                            }
                            else -> {
                                WebDavConfigSection(
                                    isVisible = true,
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
                                    onManualSync = { if (isInternetAllowed) syncVM.manualSync() },
                                    onStopSync = { syncVM.stopSync() }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SyncAdvancedSection(
                    isVisible = state.isEnabled && isInternetAllowed,
                    showAdvancedOptions = state.showAdvancedOptions,
                    isAnalyzing = state.isAnalyzing,
                    status = state.status,
                    analysis = state.analysis,
                    rotationAngle = rotationAngle,
                    onToggleAdvanced = { syncVM.toggleAdvancedOptions() },
                    onAnalyze = { syncVM.analyzeSync() },
                    onRepair = { syncVM.requestRepairSync() },
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

        if (state.showProviderSwitchDialog) {
            val targetName = if (state.pendingProvider == "GoogleDrive") "Google Drive" else state.pendingProvider ?: "Cloud Target"
            JuneConfirmationDialog(
                onDismiss = { syncVM.dismissProviderSwitchDialog() },
                onConfirm = { syncVM.confirmProviderSwitch() },
                title = stringResource(R.string.switch_provider_title),
                description = stringResource(R.string.switch_provider_desc, targetName),
                confirmButtonText = stringResource(R.string.switch_provider),
                isDestructive = false,
                icon = R.drawable.cloud_sync_24px
            )
        }

        if (state.showRepairConfirmationDialog) {
            JuneConfirmationDialog(
                onDismiss = { syncVM.dismissRepairConfirmationDialog() },
                onConfirm = { syncVM.confirmRepairSync(context) },
                title = stringResource(R.string.repair_sync_title),
                description = stringResource(R.string.repair_sync_desc),
                confirmButtonText = stringResource(R.string.repair_sync),
                isDestructive = false,
                icon = R.drawable.reset_wrench_24px
            )
        }
    }
}



