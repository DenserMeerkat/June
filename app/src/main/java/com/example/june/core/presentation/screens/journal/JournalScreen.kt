package com.example.june.core.presentation.screens.journal

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SaveAs
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.june.core.domain.utils.toDateWithDay
import com.example.june.core.presentation.screens.journal.components.JournalDatePickerDialog
import com.example.june.viewmodels.JournalVM
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JournalScreen() {
    val viewModel: JournalVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val contentFocusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    var showExitDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var isEditMode by remember { mutableStateOf(true) }

    var isInitialLoad by remember { mutableStateOf(true) }

    val formattedDate = remember(state.dateTime) {
        state.dateTime.toDateWithDay()
    }

    LaunchedEffect(state.isLoading, state.noteId) {
        if (isInitialLoad && !state.isLoading) {
            if (state.noteId != null) {
                isEditMode = false
            }
            isInitialLoad = false
        }
    }

    val onBack = {
        if (isEditMode && !state.isDraft && state.isDirty) {
            showExitDialog = true
        } else {
            viewModel.onAction(JournalAction.NavigateBack)
        }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = { onBack() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close"
                        )
                    }
                },
                actions = {
                    if (!isEditMode) {
                        IconButton(
                            onClick = { viewModel.onAction(JournalAction.ToggleBookmark) }
                        ) {
                            Icon(
                                imageVector = if (state.isBookmarked) Icons.Filled.BookmarkAdded else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark"
                            )
                        }
                    }

                    if (isEditMode) {
                        Button(
                            onClick = {
                                viewModel.onAction(JournalAction.SaveJournal)
                                isEditMode = false
                            },
                            enabled = !state.isLoading,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Save")
                            }
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
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
                            offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 4.dp)
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                                text = { Text("Delete") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete"
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.onAction(JournalAction.DeleteJournal)
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isEditMode,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                LargeFloatingActionButton(
                    onClick = {
                        isEditMode = true
                        contentFocusRequester.requestFocus()
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = isEditMode,
                    onClick = { contentFocusRequester.requestFocus() }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding()
            ) {
                TextField(
                    value = state.title,
                    onValueChange = { viewModel.onAction(JournalAction.ChangeTitle(it)) },
                    readOnly = !isEditMode,
                    enabled = isEditMode,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = if (isEditMode) {
                        {
                            Text(
                                "Add title",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    } else null,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    colors = transparentTextFieldColors()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isEditMode) {
                                Modifier.clickable { showDatePicker = true }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextField(
                    value = state.content,
                    onValueChange = { viewModel.onAction(JournalAction.ChangeContent(it)) },
                    readOnly = !isEditMode,
                    enabled = isEditMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(contentFocusRequester),
                    placeholder = if (isEditMode) {
                        {
                            Text(
                                "What's on your mind?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    } else null,
                    colors = transparentTextFieldColors()
                )
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = { Icon(imageVector = Icons.Outlined.SaveAs, contentDescription = "Save Entry") },
            title = { Text("Save Entry?") },
            text = { Text("Save this journal to revisit these thoughts anytime") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.onAction(JournalAction.SaveJournal)
                        isEditMode = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showExitDialog = false
                        viewModel.onAction(JournalAction.NavigateBack)
                    }
                ) { Text("No Thanks") }
            }
        )
    }

    if (showDatePicker) {
        JournalDatePickerDialog(
            initialDateMillis = state.dateTime,
            onDateSelected = { millis ->
                viewModel.onAction(JournalAction.ChangeDateTime(millis))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurface
)