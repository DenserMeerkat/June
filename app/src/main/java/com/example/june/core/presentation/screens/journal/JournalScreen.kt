package com.example.june.core.presentation.screens.journal

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.june.R
import com.example.june.core.domain.utils.FileUtils
import com.example.june.core.domain.utils.toDateWithDay
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.Route
import com.example.june.core.presentation.components.JuneTopAppBar
import com.example.june.core.presentation.screens.journal.components.AddItemSheet
import com.example.june.core.presentation.screens.journal.components.AddLocationDialog
import com.example.june.core.presentation.screens.journal.components.AddSongSheet
import com.example.june.core.presentation.screens.journal.components.JournalDatePickerDialog
import com.example.june.core.presentation.screens.journal.components.JournalItemsPreview
import com.example.june.core.presentation.screens.journal.components.MediaOperations
import com.example.june.viewmodels.JournalVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JournalScreen() {
    val viewModel: JournalVM = koinViewModel()
    val navigator = koinInject<AppNavigator>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val contentFocusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    var showExitDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddItemSheet by remember { mutableStateOf(false) }
    var showCameraSelectionDialog by remember { mutableStateOf(false) }
    var showSongSheet by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris.forEach { uri ->
            val internalPath = FileUtils.persistMedia(context, uri)
            if (internalPath != null) {
                viewModel.onAction(JournalAction.AddImage(internalPath))
            }
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val internalPath = FileUtils.persistMedia(context, tempCameraUri!!)
            if (internalPath != null) {
                viewModel.onAction(JournalAction.AddImage(internalPath))
            }
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && tempVideoUri != null) {
            val internalPath = FileUtils.persistMedia(context, tempVideoUri!!)
            if (internalPath != null) {
                viewModel.onAction(JournalAction.AddImage(internalPath))
            }
        }
    }

    val formattedDate = remember(state.dateTime) {
        state.dateTime.toDateWithDay()
    }

    val onBack = {
        if (state.isEditMode && !state.isDraft && state.isDirty) {
            showExitDialog = true
        } else {
            viewModel.onAction(JournalAction.NavigateBack)
        }
    }

    BackHandler { onBack() }

    val mediaOperations = remember(state.isEditMode, state.images) {
        MediaOperations(
            onItemSheetToggle = { showAddItemSheet = it },
            onRemoveMedia = { viewModel.onAction(JournalAction.RemoveImage(it)) },
            onMoveToFront = { viewModel.onAction(JournalAction.MoveImageToFront(it)) },
            onMediaClick = { path ->
                navigator.navigateTo(
                    Route.JournalMediaDetail(
                        journalId = state.journalId ?: 0L,
                        initialIndex = state.images.reversed().indexOf(path)
                    )
                )
            },
            frontMediaPath = state.images.lastOrNull(),
            onRemoveSong = { viewModel.onAction(JournalAction.RemoveSong) },
            onSongSheetToggle = { showSongSheet = true },
            onRemoveLocation = { viewModel.onAction(JournalAction.RemoveLocation) },
            onLocationDialogToggle = { showLocationDialog = true },
            isEditMode = state.isEditMode,
        )
    }

    Scaffold(
        topBar = {
            JuneTopAppBar(
                title = {},
                navigationIcon = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = { onBack() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close_24px),
                                contentDescription = "Close",

                                )
                        }
                        if (state.isEditMode) {
                            Spacer(modifier = Modifier.width(2.dp))
                            FilledIconButton(
                                onClick = { showAddItemSheet = true },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.75F
                                    )
                                ),
                            ) {
                                Icon(
                                    painter = painterResource(if (showAddItemSheet) R.drawable.add_circle_24px_fill else R.drawable.add_circle_24px),
                                    contentDescription = "Add Attachment"
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (!state.isEditMode) {
                        IconButton(
                            onClick = { viewModel.onAction(JournalAction.ToggleBookmark) },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            ),
                        ) {
                            Icon(
                                painter = painterResource(if (state.isBookmarked) R.drawable.bookmark_added_24px_fill else R.drawable.bookmark_24px),
                                contentDescription = "Toggle Bookmark"
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (state.isEditMode) {
                        Button(
                            onClick = {
                                viewModel.onAction(JournalAction.SaveJournal)
                            },
                            enabled = !state.isLoading,
                        ) {
                            Text("Save")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert_24px,),
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
                                onClick = {
                                    showMenu = false
                                    viewModel.onAction(JournalAction.DeleteJournal)
                                },
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.delete_24px),
                                        null
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !state.isEditMode,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                MediumFloatingActionButton(
                    onClick = {
                        viewModel.onAction(JournalAction.SetEditMode(!state.isEditMode))
                        contentFocusRequester.requestFocus()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.edit_24px_fill),
                        contentDescription = "Edit"
                    )
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
                    enabled = state.isEditMode,
                    onClick = { contentFocusRequester.requestFocus() }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding()
            ) {
                if (state.images.isNotEmpty() || state.songDetails != null || state.location != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    JournalItemsPreview(
                        mediaPaths = state.images,
                        mediaOperations = mediaOperations,
                        songDetails = state.songDetails,
                        location = state.location,
                        onShowAllClick = {
                            navigator.navigateTo(
                                Route.JournalMedia(journalId = state.journalId ?: 0L)
                            )
                        }
                    )
                }

                TextField(
                    value = state.title,
                    onValueChange = { viewModel.onAction(JournalAction.ChangeTitle(it)) },
                    readOnly = !state.isEditMode,
                    enabled = state.isEditMode,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = if (state.isEditMode) {
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
                            if (state.isEditMode) {
                                Modifier.clickable { showDatePicker = true }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.today_24px),
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
                    readOnly = !state.isEditMode,
                    enabled = state.isEditMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(contentFocusRequester),
                    placeholder = if (state.isEditMode) {
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

    if (showCameraSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraSelectionDialog = false },
            icon = {
                Icon(
                    painterResource(R.drawable.add_a_photo_24px),
                    null
                )
            },
            title = { Text("Capture Media") },
            text = { Text("Would you like to take a photo or record a video?") },
            confirmButton = {
                TextButton(onClick = {
                    showCameraSelectionDialog = false
                    val uri = FileUtils.createTempVideoUri(context)
                    tempVideoUri = uri
                    videoLauncher.launch(uri)
                }) { Text("Record Video") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCameraSelectionDialog = false
                    val uri = FileUtils.createTempPictureUri(context)
                    tempCameraUri = uri
                    photoLauncher.launch(uri)
                }) { Text("Take Photo") }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = {
                Icon(
                    painterResource(R.drawable.file_save_24px),
                    null
                )
            },
            title = { Text("Save Entry?") },
            text = { Text("Save this journal to revisit these thoughts anytime") },
            confirmButton = {
                Button(onClick = {
                    showExitDialog = false
                    viewModel.onAction(JournalAction.SaveJournal)
                }) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showExitDialog = false
                    viewModel.onAction(JournalAction.NavigateBack)
                }) { Text("No Thanks") }
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

    if (showSongSheet) {
        AddSongSheet(
            songDetails = state.songDetails,
            isFetching = state.isFetchingSong,
            onFetchDetails = { link ->
                viewModel.onAction(JournalAction.FetchSong(link))
            },
            onRemoveSong = {
                viewModel.onAction(JournalAction.RemoveSong)
            },
            onDismiss = { showSongSheet = false }
        )
    }

    if (showLocationDialog) {
        AddLocationDialog(
            existingLocation = state.location,
            onLocationSelected = { loc ->
                viewModel.onAction(JournalAction.SetLocation(loc))
            },
            onDismiss = { showLocationDialog = false },
            isEditMode = state.isEditMode
        )
    }

    if (showAddItemSheet) {
        AddItemSheet(
            onDismiss = { showAddItemSheet = false },
            onTakePhotoClick = {
                showCameraSelectionDialog = true
            },
            onAddPhotoClick = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onAddSongClick = {
                showAddItemSheet = false
                showSongSheet = true
            },
            onAddLocationClick = {
                showAddItemSheet = false
                showLocationDialog = true
            }
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