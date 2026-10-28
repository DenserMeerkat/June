package com.denser.june.presentation.screens.settings.screens.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.utils.toLocalDate
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JunePlaceholderPage
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.components.journal.DayJournalGroupData
import com.denser.june.presentation.components.journal.ExportJournalSheet
import com.denser.june.presentation.components.journal.HighlightJournalCard
import com.denser.june.presentation.screens.settings.screens.lock.LockGate
import com.denser.june.presentation.screens.home.components.DayJournalGroup
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.components.journal.JournalOptionsSheet
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.runtime.DisposableEffect
import com.denser.june.presentation.pro.LocalIsPro
import com.denser.june.presentation.pro.LocalProSheetController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen() {
    val vaultVM: VaultVM = koinViewModel()
    val isVaultUnlocked by vaultVM.isVaultUnlocked.collectAsStateWithLifecycle()
    val lockedJournals by vaultVM.lockedJournals.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()
    val journalPrefs = koinInject<JournalPreferences>()
    val timeFormat by journalPrefs.timeFormat().collectAsStateWithLifecycle(initialValue = TimeFormat.TWELVE_HOUR)
    val is24Hour = timeFormat == TimeFormat.TWENTY_FOUR_HOUR

    var keepVaultSessionUnlocked by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            if (!keepVaultSessionUnlocked) {
                vaultVM.lockVault()
            }
        }
    }

    var selectedJournalForOptions by remember { mutableStateOf<Journal?>(null) }
    var journalToExport by remember { mutableStateOf<Journal?>(null) }
    val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (!isVaultUnlocked) {
        LockGate(
            onUnlocked = { vaultVM.unlockVault() },
            onDismiss = { navigator.navigateBack() }
        )
        return
    }

    val isPro = LocalIsPro.current
    val proSheetController = LocalProSheetController.current

    Scaffold(
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.CenterAligned,
                title = {
                    Text(stringResource(R.string.vault), fontWeight = FontWeight.Bold)
                },
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
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            keepVaultSessionUnlocked = true
                            navigator.navigateTo(Route.VaultLockSettings)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.settings_b_roll_24px),
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (lockedJournals.isEmpty()) {
            JunePlaceholderPage(
                modifier = Modifier.padding(paddingValues),
                icon = R.drawable.lock_24px,
                title = stringResource(R.string.vault_empty_title),
                subtitle = stringResource(R.string.vault_empty_desc),
                action = if (!isPro) {
                    {
                        Button(
                            onClick = {
                                proSheetController.show()
                            }
                        ) {
                            Text(stringResource(R.string.unlock_june_pro))
                        }
                    }
                } else null
            )
        } else {
            val dayGroups = remember(lockedJournals) {
                lockedJournals
                    .groupBy { it.dateTime.toLocalDate() }
                    .map { (date, journalsOnDay) -> DayJournalGroupData(date, journalsOnDay) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.vault_notice),
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
                                onToggleBookmark = vaultVM::toggleBookmark,
                                onJournalClick = { journal ->
                                    keepVaultSessionUnlocked = true
                                    navigator.navigateTo(Route.Editor(journalId = journal.id))
                                },
                                onLongClick = { journal ->
                                    selectedJournalForOptions = journal
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    } else {
                        val singleJournal = dayGroup.journals.first()
                        item(key = "single_journal_${singleJournal.id}") {
                            HighlightJournalCard(
                                journal = singleJournal,
                                is24Hour = is24Hour,
                                onToggleBookmark = {
                                    vaultVM.toggleBookmark(singleJournal.id)
                                },
                                onJournalClick = {
                                    keepVaultSessionUnlocked = true
                                    navigator.navigateTo(Route.Editor(journalId = singleJournal.id))
                                },
                                onLongClick = {
                                    selectedJournalForOptions = singleJournal
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }

        ExportJournalSheet(
            journal = journalToExport,
            onDismiss = { journalToExport = null }
        )

        val currentJournalForOptions = remember(selectedJournalForOptions, lockedJournals) {
            val id = selectedJournalForOptions?.id ?: return@remember null
            lockedJournals.find { it.id == id }
        }

        currentJournalForOptions?.let { currentJournal ->
            ModalBottomSheet(
                onDismissRequest = { selectedJournalForOptions = null },
                sheetState = optionsSheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                JournalOptionsSheet(
                    journal = currentJournal,
                    is24Hour = is24Hour,
                    onDismiss = { selectedJournalForOptions = null },
                    onToggleBookmark = {
                        vaultVM.toggleBookmark(currentJournal.id)
                    },
                    onToggleLock = {
                        vaultVM.unlockJournal(currentJournal.id)
                    },
                    onExportMarkdown = {
                        journalToExport = currentJournal
                        scope.launch { optionsSheetState.hide() }.invokeOnCompletion {
                            selectedJournalForOptions = null
                        }
                    },
                    onDeleteOrRestore = {
                        vaultVM.deleteJournal(currentJournal.id)
                    }
                )
            }
        }
    }
}
