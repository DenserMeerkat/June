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
import com.denser.june.presentation.components.JuneDialog
import com.denser.june.presentation.screens.editor.components.AddItemSheet
import com.denser.june.presentation.screens.editor.components.AddLocationDialog
import com.denser.june.presentation.screens.editor.components.AddSongSheet
import com.denser.june.presentation.screens.editor.components.JournalDatePickerDialog
import com.denser.june.presentation.screens.editor.components.JournalEmojiPickerDialog
import com.denser.june.presentation.screens.editor.components.JournalTagsDialog
import com.denser.june.presentation.screens.home.components.DeleteConfirmationSheet

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
        JuneDialog(
            onDismissRequest = { dialogState.showCameraSelectionDialog = false },
            title = "Capture Media",
            icon = R.drawable.add_a_photo_24px,
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
            },
            text = { Text("Would you like to take a photo or record a video?") }
        )
    }

    if (dialogState.showExitDialog) {
        JuneDialog(
            onDismissRequest = { dialogState.showExitDialog = false },
            title = "Save Entry?",
            icon = R.drawable.file_save_24px,
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
            },
            text = { Text("Would you like to save your progress before leaving?") }
        )
    }

    if (dialogState.showDeleteConfirmation) {
        DeleteConfirmationSheet(
            onDismissRequest = { dialogState.showDeleteConfirmation = false },
            onConfirm = {
                dialogState.showDeleteConfirmation = false
                onAction(EditorAction.DeleteJournal)
            }
        )
    }

    if (dialogState.showDatePicker) {
        JournalDatePickerDialog(
            initialDateMillis = editorState.dateTime,
            startOfWeek = editorState.startOfWeek,
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