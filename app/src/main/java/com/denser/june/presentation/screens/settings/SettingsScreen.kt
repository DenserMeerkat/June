package com.denser.june.presentation.screens.settings

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
import androidx.compose.ui.res.painterResource
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

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColorPickerSheet by remember { mutableStateOf(false) }

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
                onDeleteAllJournals = { showDeleteDialog = true },
                onColorPickerClick = { showColorPickerSheet = true },
                onLicenseClick = { navigator.navigateTo(Route.AboutSettings) }
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
                    placeholder = { Text("Search settings...") },
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
                                    title = "General",
                                    subtitle = "Manage journaling preferences",
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.category_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = { navigator.navigateTo(Route.GeneralSettings) }
                                )
                                CategorySettingsItem(
                                    title = "Editor",
                                    subtitle = "Formatting and writing options",
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.article_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = { navigator.navigateTo(Route.EditorSettings) }
                                )
                                CategorySettingsItem(
                                    title = "Appearance",
                                    subtitle = "Theme, font, colors, palette",
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.format_paint_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = { navigator.navigateTo(Route.AppearanceSettings) }
                                )
                                CategorySettingsItem(
                                    title = "Privacy & Security",
                                    subtitle = "App lock, biometrics, data protection",
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.lock_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = { navigator.navigateTo(Route.PrivacySecuritySettings) }
                                )
                                CategorySettingsItem(
                                    title = "Sync & Backup",
                                    subtitle = "Cloud sync, export, import",
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.home_storage_gear_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = { navigator.navigateTo(Route.SyncBackupSettings) }
                                )
                                CategorySettingsItem(
                                    title = "Bin",
                                    subtitle = "View or restore deleted journals",
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.delete_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = { navigator.navigateTo(Route.Bin) }
                                )
                                CategorySettingsItem(
                                    title = "About",
                                    subtitle = "Version, licenses, libraries",
                                    leadingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.info_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    onClick = { navigator.navigateTo(Route.AboutSettings) }
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
                                        title = "No settings found",
                                        subtitle = "We couldn't find any settings matching \"$searchQuery\"."
                                    )
                                }
                            }
                        } else {
                            val grouped = filteredSettings.groupBy { it.category }
                            grouped.forEach { (category, tiles) ->
                                item {
                                    SettingSection(title = category) {
                                        tiles.forEach { tile ->
                                            tile.content()
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
            title = "Move all to Bin?",
            description = "This will move all your journal entries to the Bin. You can restore them within 30 days.",
            confirmText = "Delete",
            confirmButtonText = "Move All to Bin",
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
}
