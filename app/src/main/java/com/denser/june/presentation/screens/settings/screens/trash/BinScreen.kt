package com.denser.june.presentation.screens.settings.screens.trash
import com.denser.june.presentation.components.JuneBackButton
import com.denser.june.presentation.components.JuneMoreOptionsButton

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.utils.toLocalDate
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneDropdownMenu
import com.denser.june.presentation.components.JuneDropdownMenuItem
import com.denser.june.presentation.components.JunePlaceholderPage
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.components.journal.JournalCard
import com.denser.june.presentation.components.journal.HighlightJournalCard
import com.denser.june.presentation.components.JuneConfirmationDialog
import com.denser.june.presentation.components.journal.DayJournalGroupData
import com.denser.june.presentation.components.journal.ExportJournalSheet
import com.denser.june.presentation.screens.home.components.DayJournalGroup
import com.denser.june.presentation.components.journal.JournalOptionsSheet
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
    val isSyncEnabled by binVM.isSyncEnabled.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()

    val dayGroups = remember(deletedJournals) {
        deletedJournals
            .groupBy { it.dateTime.toLocalDate() }
            .map { (date, journalsOnDay) -> DayJournalGroupData(date, journalsOnDay) }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showEmptyBinDialog by remember { mutableStateOf(false) }
    var showRestoreAllDialog by remember { mutableStateOf(false) }
    var journalToExport by remember { mutableStateOf<Journal?>(null) }

    var selectedJournalForOptions by remember { mutableStateOf<Journal?>(null) }
    val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismissOptionsSheet(action: () -> Unit) {
        action()
        scope.launch { optionsSheetState.hide() }.invokeOnCompletion {
            selectedJournalForOptions = null
        }
    }


    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.CenterAligned,
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.bin), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    JuneBackButton(onClick = { navigator.navigateBack() })
                },
                actions = {
                    if (deletedJournals.isNotEmpty()) {
                        Box {
                            JuneMoreOptionsButton(
                                onClick = { showMenu = true }
                            )
                            JuneDropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                offset = DpOffset(x = 0.dp, y = 4.dp)
                            ) {
                                JuneDropdownMenuItem(
                                    text = { Text(stringResource(R.string.restore_all)) },
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
                                JuneDropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.empty_bin),
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
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            if (deletedJournals.isEmpty()) {
                JunePlaceholderPage(
                    icon = R.drawable.delete_24px,
                    title = stringResource(R.string.bin_is_empty),
                    subtitle = stringResource(R.string.bin_empty_desc),
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp + innerPadding.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.bin_retention_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    dayGroups.forEach { dayGroup ->
                        if (dayGroup.journals.size > 1) {
                            item(key = "day_group_${dayGroup.date}") {
                                DayJournalGroup(
                                    date = dayGroup.date,
                                    journals = dayGroup.journals,
                                    is24Hour = is24Hour,
                                    allowMediaHighlight = true,
                                    actionIcon = R.drawable.restore_from_trash_24px,
                                    onActionClick = { journal -> binVM.restoreJournal(journal.id) },
                                    onJournalClick = null,
                                    onLongClick = { journal -> selectedJournalForOptions = journal },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        } else {
                            val singleJournal = dayGroup.journals.first()
                            item(key = "single_journal_${singleJournal.id}") {
                                HighlightJournalCard(
                                    journal = singleJournal,
                                    is24Hour = is24Hour,
                                    actionIcon = R.drawable.restore_from_trash_24px,
                                    onActionClick = { binVM.restoreJournal(singleJournal.id) },
                                    onLongClick = { selectedJournalForOptions = singleJournal },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showEmptyBinDialog) {
            JuneConfirmationDialog(
                onDismiss = { showEmptyBinDialog = false },
                onConfirm = { binVM.emptyBin() },
                title = stringResource(R.string.empty_bin_title),
                description = if (isSyncEnabled) {
                    stringResource(R.string.empty_bin_sync_desc)
                } else {
                    stringResource(R.string.empty_bin_local_desc)
                },
                confirmText = stringResource(R.string.delete),
                confirmButtonText = stringResource(R.string.permanently_delete_all),
                isDestructive = true,
                icon = R.drawable.delete_24px
            )
        }

        if (showRestoreAllDialog) {
            JuneConfirmationDialog(
                onDismiss = { showRestoreAllDialog = false },
                onConfirm = { binVM.restoreAll() },
                title = stringResource(R.string.restore_all_title),
                description = stringResource(R.string.restore_all_desc),
                confirmText = null,
                confirmButtonText = stringResource(R.string.restore_all),
                isDestructive = false,
                icon = R.drawable.restore_from_trash_24px
            )
        }

        ExportJournalSheet(
            journal = journalToExport,
            onDismiss = { journalToExport = null }
        )

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
                    onDismiss = { selectedJournalForOptions = null },
                    onToggleBookmark = { binVM.toggleBookmark(currentJournalForOptions.id) },
                    onExportMarkdown = {
                        journalToExport = currentJournalForOptions
                        selectedJournalForOptions = null
                    },
                    onDeleteOrRestore = {
                        binVM.restoreJournal(currentJournalForOptions.id)
                    },
                    onPermanentDelete = {
                        binVM.deletePermanently(currentJournalForOptions.id)
                    }
                )
            }
        }

    }
}
