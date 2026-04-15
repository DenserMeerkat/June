package com.denser.june.presentation.screens.settings.screens.trash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JunePlaceholderPage
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.screens.home.components.JournalCard
import com.denser.june.presentation.screens.home.components.RecentJournalCard
import com.denser.june.presentation.components.JuneConfirmationDialog
import com.denser.june.presentation.screens.home.components.JournalOptionsSheet
import com.denser.june.presentation.screens.home.components.DeleteConfirmationSheet
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinScreen() {
    val binVM: BinVM = koinViewModel()
    val timeFormat by binVM.timeFormat.collectAsStateWithLifecycle()
    val is24Hour = timeFormat == TimeFormat.TWENTY_FOUR_HOUR
    val deletedJournals by binVM.deletedJournals.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()

    var showMenu by remember { mutableStateOf(false) }
    var showEmptyBinDialog by remember { mutableStateOf(false) }
    var showRestoreAllDialog by remember { mutableStateOf(false) }
    var showPermanentDeleteConfirmation by remember { mutableStateOf(false) }
    var journalToDeletePermanently by remember { mutableStateOf<Journal?>(null) }

    var selectedJournalForOptions by remember { mutableStateOf<Journal?>(null) }
    val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismissOptionsSheet(action: () -> Unit) {
        action()
        scope.launch { optionsSheetState.hide() }.invokeOnCompletion {
            selectedJournalForOptions = null
        }
    }


    Scaffold(
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.CenterAligned,
                title = { Text("Bin", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FilledIconButton(
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (deletedJournals.isNotEmpty()) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    painterResource(R.drawable.more_vert_24px),
                                    contentDescription = "Options"
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = RoundedCornerShape(24.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 3.dp,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                offset = DpOffset(x = 0.dp, y = 4.dp)
                            ) {
                                DropdownMenuItem(
                                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                                    text = { Text("Restore All") },
                                    onClick = {
                                        showMenu = false
                                        showRestoreAllDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painterResource(R.drawable.restore_from_trash_24px),
                                            contentDescription = null
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                                    text = {
                                        Text(
                                            "Empty Bin",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showEmptyBinDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painterResource(R.drawable.delete_24px),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (deletedJournals.isEmpty()) {
                JunePlaceholderPage(
                    icon = R.drawable.delete_24px,
                    title = "Bin is empty",
                    subtitle = "Journals you delete will appear here for 30 days before being permanently removed."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Items in Bin will be permanently deleted after 30 days.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(deletedJournals, key = { it.id }) { journal ->
                        if (journal.images.isNotEmpty()) {
                            RecentJournalCard(
                                journal = journal,
                                modifier = Modifier,
                                actionIcon = R.drawable.restore_from_trash_24px,
                                onActionClick = { binVM.restoreJournal(journal.id) },
                                onLongClick = { selectedJournalForOptions = journal }
                            )
                        } else {
                            JournalCard(
                                journal = journal,
                                modifier = Modifier,
                                actionIcon = R.drawable.restore_from_trash_24px,
                                onActionClick = { binVM.restoreJournal(journal.id) },
                                onLongClick = { selectedJournalForOptions = journal }
                            )
                        }
                    }
                }
            }
        }

        if (showEmptyBinDialog) {
            JuneConfirmationDialog(
                onDismiss = { showEmptyBinDialog = false },
                onConfirm = { binVM.emptyBin() },
                title = "Empty Bin?",
                description = "This will permanently delete all journals in the bin. This action cannot be undone.",
                confirmText = "Delete",
                confirmButtonText = "Permanently Delete All",
                isDestructive = true,
                icon = R.drawable.delete_24px
            )
        }

        if (showRestoreAllDialog) {
            JuneConfirmationDialog(
                onDismiss = { showRestoreAllDialog = false },
                onConfirm = { binVM.restoreAll() },
                title = "Restore All?",
                description = "This will restore all deleted entries back to your timeline.",
                confirmText = null,
                confirmButtonText = "Restore All",
                isDestructive = false,
                icon = R.drawable.restore_from_trash_24px
            )
        }

        val currentJournalForOptions = remember(selectedJournalForOptions, deletedJournals) {
            val id = selectedJournalForOptions?.id ?: return@remember null
            deletedJournals.find { it.id == id }
        }

        if (currentJournalForOptions != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedJournalForOptions = null },
                sheetState = optionsSheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                JournalOptionsSheet(
                    journal = currentJournalForOptions,
                    is24Hour = is24Hour,
                    onToggleBookmark = { dismissOptionsSheet { binVM.toggleBookmark(currentJournalForOptions.id) } },
                    onDeleteOrRestore = { dismissOptionsSheet { binVM.restoreJournal(currentJournalForOptions.id) } },
                    onPermanentDelete = {
                        dismissOptionsSheet {
                            journalToDeletePermanently = currentJournalForOptions
                            showPermanentDeleteConfirmation = true
                        }
                    }
                )
            }
        }

        if (showPermanentDeleteConfirmation && journalToDeletePermanently != null) {
            DeleteConfirmationSheet(
                onDismissRequest = {
                    showPermanentDeleteConfirmation = false
                    journalToDeletePermanently = null
                },
                onConfirm = {
                    binVM.deletePermanently(journalToDeletePermanently!!.id)
                    showPermanentDeleteConfirmation = false
                    journalToDeletePermanently = null
                },
                message = "Permanently delete this journal? This action cannot be undone.",
                confirmText = "Permanently Delete"
            )
        }

    }
}