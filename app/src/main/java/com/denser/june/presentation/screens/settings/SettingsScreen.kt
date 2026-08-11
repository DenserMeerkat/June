package com.denser.june.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneConfirmationDialog
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.components.JunePlaceholderPage
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.components.ColorPickerSheet
import com.denser.june.presentation.screens.settings.components.*
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.utils.InternetDisabledException
import com.denser.june.presentation.utils.UpdateChecker
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen() {
    val settingsVM: SettingsVM = koinViewModel()
    val state by settingsVM.state.collectAsStateWithLifecycle()
    val onAction = settingsVM::onAction
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColorPickerSheet by remember { mutableStateOf(false) }
    var showLicenseSheet by remember { mutableStateOf(false) }
    var showMapCreditsSheet by remember { mutableStateOf(false) }
    var showAboutLibrariesSheet by remember { mutableStateOf(false) }
    var showChangelogSheet by remember { mutableStateOf(false) }
    var showCheckingUpdatesDialog by remember { mutableStateOf(false) }

    val updateChecker = koinInject<UpdateChecker>()
    var updateInfo by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var showNoUpdateDialog by remember { mutableStateOf(false) }
    var updateErrorMsg by remember { mutableStateOf<String?>(null) }
    var showInternetDisabledDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val searchableSettings = SettingsTileRegistry.getTiles()

    val filteredSettings = remember(searchQuery, searchableSettings, state) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            emptyList()
        } else {
            searchableSettings.filter { setting ->
                setting.key != "ABOUT_HEADER" && setting.key != "DEVELOPER" && (
                    setting.title.lowercase().contains(query) ||
                    setting.subtitle(context, state)?.lowercase()?.contains(query) == true ||
                    setting.category.lowercase().contains(query) ||
                    setting.keywords.any { keyword -> keyword.lowercase().contains(query) }
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = "Settings") },
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
        val triggers = remember(navigator) {
            SettingsTriggers(
                onDeleteAllJournals = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    showDeleteDialog = true
                },
                onColorPickerClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    showColorPickerSheet = true
                },
                onLicenseClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    showLicenseSheet = true
                },
                onMapAttributionsClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    showMapCreditsSheet = true
                },
                onAboutLibrariesClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    showAboutLibrariesSheet = true
                },
                onChangelogClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    showChangelogSheet = true
                },
                onCheckForUpdatesClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search_settings_placeholder)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.search_24px),
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    painter = painterResource(R.drawable.close_24px),
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (searchQuery.isEmpty()) {
                        item {
                            SettingSection {
                                CategorySettingsItem(
                                    title = stringResource(R.string.general),
                                    subtitle = stringResource(R.string.general_desc),
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.category_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        navigator.navigateTo(Route.GeneralSettings)
                                    }
                                )
                                CategorySettingsItem(
                                    title = stringResource(R.string.editor),
                                    subtitle = stringResource(R.string.editor_desc),
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.article_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        navigator.navigateTo(Route.EditorSettings)
                                    }
                                )
                                CategorySettingsItem(
                                    title = stringResource(R.string.appearance),
                                    subtitle = stringResource(R.string.appearance_desc),
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.format_paint_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        navigator.navigateTo(Route.AppearanceSettings)
                                    }
                                )
                                CategorySettingsItem(
                                    title = stringResource(R.string.privacy_and_security),
                                    subtitle = stringResource(R.string.privacy_and_security_desc),
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.lock_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        navigator.navigateTo(Route.PrivacySecuritySettings)
                                    }
                                )
                                CategorySettingsItem(
                                    title = stringResource(R.string.sync_and_backup),
                                    subtitle = stringResource(R.string.sync_and_backup_desc),
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.home_storage_gear_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        navigator.navigateTo(Route.SyncBackupSettings)
                                    }
                                )
                                CategorySettingsItem(
                                    title = stringResource(R.string.bin),
                                    subtitle = stringResource(R.string.bin_desc),
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.delete_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        navigator.navigateTo(Route.Bin)
                                    }
                                )
                                CategorySettingsItem(
                                    title = stringResource(R.string.about),
                                    subtitle = stringResource(R.string.about_desc),
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.info_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        navigator.navigateTo(Route.AboutSettings)
                                    }
                                )
                            }
                        }
                    } else {
                        if (filteredSettings.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxWidth()
                                        .padding(vertical = 64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    JunePlaceholderPage(
                                        icon = R.drawable.search_off_24px,
                                        title = stringResource(R.string.no_settings_found),
                                        subtitle = stringResource(R.string.no_settings_found_desc, searchQuery)
                                    )
                                }
                            }
                        } else {
                            val grouped = filteredSettings.groupBy { it.category }
                            grouped.forEach { (category, tiles) ->
                                item {
                                    SettingSection(title = category) {
                                        tiles.forEach { tile ->
                                            Box(
                                                modifier = Modifier.clickable {
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                }
                                            ) {
                                                tile.content()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        JuneConfirmationDialog(
            title = stringResource(R.string.move_all_to_bin_title),
            description = stringResource(R.string.move_all_to_bin_desc),
            confirmText = stringResource(R.string.delete),
            confirmButtonText = stringResource(R.string.move_all_to_bin),
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onAction(SettingsAction.OnDeleteJournals)
                showDeleteDialog = false
            }
        )
    }

    if (showColorPickerSheet) {
        ColorPickerSheet(
            initialColor = Color(state.appTheme.seedColor),
            onSelect = { onAction(SettingsAction.OnSeedColorChange(it.toArgb())) },
            onDismiss = { showColorPickerSheet = false }
        )
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
}
