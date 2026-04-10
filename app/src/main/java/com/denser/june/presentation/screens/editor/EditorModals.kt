package com.denser.june.presentation.screens.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.utils.FileUtils
import com.denser.june.presentation.screens.editor.components.AddItemSheet
import com.denser.june.presentation.screens.editor.components.AddLocationDialog
import com.denser.june.presentation.screens.editor.components.AddSongSheet
import com.denser.june.presentation.screens.editor.components.JournalDatePickerDialog
import com.denser.june.presentation.screens.editor.components.JournalEmojiPickerDialog
import com.denser.june.presentation.screens.editor.components.JournalTagsDialog

class EditorDialogState {
    var showExitDialog by mutableStateOf(false)
    var showDeleteConfirmation by mutableStateOf(false)
    var showDatePicker by mutableStateOf(false)
    var showAddItemSheet by mutableStateOf(false)
    var showEmojiPicker by mutableStateOf(false)
    var showCameraSelectionDialog by mutableStateOf(false)
    var showSongSheet by mutableStateOf(false)
    var showLocationDialog by mutableStateOf(false)
    var showTagsDialog by mutableStateOf(false)
}

@Composable
fun rememberEditorDialogState() = remember { EditorDialogState() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorModals(
    dialogState: EditorDialogState,
    editorState: EditorState,
    onAction: (EditorAction) -> Unit
) {
    val context = LocalContext.current

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val newPaths = uris.mapNotNull { FileUtils.persistMedia(context, it) }
        if (newPaths.isNotEmpty()) onAction(EditorAction.AddImages(newPaths))
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            FileUtils.persistMedia(context, tempCameraUri!!)
                ?.let { onAction(EditorAction.AddImage(it)) }
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && tempVideoUri != null) {
            FileUtils.persistMedia(context, tempVideoUri!!)
                ?.let { onAction(EditorAction.AddImage(it)) }
        }
    }

    if (dialogState.showCameraSelectionDialog) {
        AlertDialog(
            onDismissRequest = { dialogState.showCameraSelectionDialog = false },
            icon = { Icon(painterResource(R.drawable.add_a_photo_24px), null) },
            title = { Text("Capture Media") },
            text = { Text("Would you like to take a photo or record a video?") },
            confirmButton = {
                TextButton(onClick = {
                    dialogState.showCameraSelectionDialog = false
                    val uri = FileUtils.createTempVideoUri(context)
                    tempVideoUri = uri
                    videoLauncher.launch(uri)
                }) { Text("Record Video") }
            },
            dismissButton = {
                TextButton(onClick = {
                    dialogState.showCameraSelectionDialog = false
                    val uri = FileUtils.createTempPictureUri(context)
                    tempCameraUri = uri
                    photoLauncher.launch(uri)
                }) { Text("Take Photo") }
            }
        )
    }

    if (dialogState.showExitDialog) {
        AlertDialog(
            onDismissRequest = { dialogState.showExitDialog = false },
            icon = { Icon(painterResource(R.drawable.file_save_24px), null) },
            title = { Text("Save Entry?") },
            text = { Text("Would you like to save your progress before leaving?") },
            confirmButton = {
                Button(onClick = {
                    dialogState.showExitDialog = false
                    onAction(EditorAction.SaveJournal)
                    onAction(EditorAction.NavigateBack)
                }) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    dialogState.showExitDialog = false
                    onAction(EditorAction.NavigateBack)
                }) { Text("Discard") }
            }
        )
    }

    if (dialogState.showDeleteConfirmation) {
        ModalBottomSheet(
            onDismissRequest = { dialogState.showDeleteConfirmation = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = "Move to bin? It will be permanently deleted after 30 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    onClick = {
                        dialogState.showDeleteConfirmation = false
                        onAction(EditorAction.DeleteJournal)
                    },
                    color = androidx.compose.ui.graphics.Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.delete_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Move to bin",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (dialogState.showDatePicker) {
        JournalDatePickerDialog(
            initialDateMillis = editorState.dateTime,
            onDateSelected = { millis ->
                onAction(EditorAction.ChangeDateTime(millis))
                dialogState.showDatePicker = false
            },
            onDismiss = { dialogState.showDatePicker = false }
        )
    }

    if (dialogState.showSongSheet) {
        AddSongSheet(
            songDetails = editorState.songDetails,
            isFetching = editorState.isFetchingSong,
            onFetchDetails = { link -> onAction(EditorAction.FetchSong(link)) },
            onRemoveSong = { onAction(EditorAction.RemoveSong) },
            onDismiss = { dialogState.showSongSheet = false }
        )
    }

    if (dialogState.showLocationDialog) {
        AddLocationDialog(
            existingLocation = editorState.location,
            onLocationSelected = { loc -> onAction(EditorAction.SetLocation(loc)) },
            onDismiss = { dialogState.showLocationDialog = false }
        )
    }

    if (dialogState.showAddItemSheet) {
        AddItemSheet(
            onDismiss = { dialogState.showAddItemSheet = false },
            onTakePhotoClick = { dialogState.showCameraSelectionDialog = true },
            onAddPhotoClick = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            },
            onAddSongClick = {
                dialogState.showAddItemSheet = false
                dialogState.showSongSheet = true
            },
            onAddLocationClick = {
                dialogState.showAddItemSheet = false
                dialogState.showLocationDialog = true
            }
        )
    }

    if (dialogState.showEmojiPicker) {
        JournalEmojiPickerDialog(
            initialEmoji = editorState.emoji,
            onEmojiSelected = { emoji ->
                onAction(EditorAction.ChangeEmoji(emoji))
                dialogState.showEmojiPicker = false
            },
            onDismiss = { dialogState.showEmojiPicker = false }
        )
    }

    if (dialogState.showTagsDialog) {
        JournalTagsDialog(
            tags = editorState.tags,
            suggestions = editorState.tagSuggestions,
            onSaveTags = { newTags -> onAction(EditorAction.UpdateTags(newTags)) },
            onSearchTags = { onAction(EditorAction.SearchTags(it)) },
            onDismiss = { dialogState.showTagsDialog = false }
        )
    }
}