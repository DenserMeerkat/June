package com.denser.june.presentation.screens.trash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JunePlaceholderPage
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.screens.home.components.JournalCard
import com.denser.june.presentation.screens.home.components.RecentJournalCard
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen() {
    val trashVM: TrashVM = koinViewModel()
    val deletedJournals by trashVM.deletedJournals.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()
    
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var confirmText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.CenterAligned,
                title = { Text("Trash", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FilledIconButton(
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(painterResource(R.drawable.arrow_back_24px), contentDescription = "Back")
                    }
                },
                actions = {
                    if (deletedJournals.isNotEmpty()) {
                        TextButton(
                            onClick = { showEmptyTrashDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Empty Trash")
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
                    title = "Trash is empty",
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
                            text = "Items in Trash will be permanently deleted after 30 days.",
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
                                actionIcon = R.drawable.history_24px,
                                onActionClick = { trashVM.restoreJournal(journal.id) }
                            )
                        } else {
                            JournalCard(
                                journal = journal,
                                modifier = Modifier,
                                actionIcon = R.drawable.history_24px,
                                onActionClick = { trashVM.restoreJournal(journal.id) }
                            )
                        }
                    }
                }
            }
        }

        if (showEmptyTrashDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showEmptyTrashDialog = false
                    confirmText = ""
                },
                title = { Text("Empty Trash?") },
                text = {
                    Column {
                        Text("This will permanently delete all journals in the trash. This action cannot be undone.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Type 'Delete' to confirm:", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = confirmText,
                            onValueChange = { confirmText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Delete") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            trashVM.emptyTrash()
                            showEmptyTrashDialog = false
                            confirmText = ""
                        },
                        enabled = confirmText.equals("Delete", ignoreCase = true),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Permanently Delete All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showEmptyTrashDialog = false
                        confirmText = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
