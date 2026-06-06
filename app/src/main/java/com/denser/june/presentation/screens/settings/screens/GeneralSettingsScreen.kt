package com.denser.june.presentation.screens.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.components.JuneConfirmationDialog
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.*
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.denser.june.core.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen() {
    val settingsVM: SettingsVM = koinViewModel()
    val onAction = settingsVM::onAction
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = "General") },
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
                onDeleteAllJournals = { showDeleteDialog = true }
            )
        }

        CompositionLocalProvider(LocalSettingsTriggers provides triggers) {
            val generalTiles = SettingsTileRegistry.getTilesForCategory("General").associateBy { it.key }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val timeGroup = listOfNotNull(
                    generalTiles["START_OF_WEEK"],
                    generalTiles["TIME_FORMAT"],
                    generalTiles["INCLUDE_TIME"]
                )
                if (timeGroup.isNotEmpty()) {
                    item {
                        SettingSection {
                            timeGroup.forEach { it.content() }
                        }
                    }
                }

                val editorGroup = listOfNotNull(
                    generalTiles["REMINDERS"],
                    generalTiles["MARKDOWN_EDITOR"]
                )
                if (editorGroup.isNotEmpty()) {
                    item {
                        SettingSection {
                            editorGroup.forEach { it.content() }
                        }
                    }
                }

                val mapTile = generalTiles["MAP_SETTINGS"]
                if (mapTile != null) {
                    item {
                        SettingSection {
                            mapTile.content()
                        }
                    }
                }

                val deleteTile = generalTiles["DELETE_ALL_JOURNALS"]
                if (deleteTile != null) {
                    item {
                        SettingSection {
                            deleteTile.content()
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
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
}
