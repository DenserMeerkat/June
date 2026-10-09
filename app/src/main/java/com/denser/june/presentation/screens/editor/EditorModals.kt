package com.denser.june.presentation.screens.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.media.compress.CompressUtils
import com.denser.june.core.media.compress.model.CompressionPreset
import com.denser.june.core.media.exif.ExifUtils
import com.denser.june.core.media.storage.MediaStorageManager
import com.denser.june.presentation.components.JuneDialog
import com.denser.june.presentation.components.JuneDateTimePicker
import com.denser.june.presentation.components.JuneDateTimePickerMode
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.editor.components.AddItemSheet
import com.denser.june.presentation.screens.editor.components.AddLocationDialog
import com.denser.june.presentation.screens.editor.components.emoji.JournalEmojiPickerSheet
import com.denser.june.presentation.screens.editor.components.emoji.loadEmojiAssets
import com.denser.june.presentation.screens.editor.components.JournalTagsDialog
import com.denser.june.presentation.screens.home.components.DeleteConfirmationSheet
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

class EditorDialogState {
    var showExitDialog by mutableStateOf(false)
    var showDeleteConfirmation by mutableStateOf(false)
    var showDatePicker by mutableStateOf(false)
    var datePickerInitialTab by mutableIntStateOf(0)
    var showAddItemSheet by mutableStateOf(false)
    var showEmojiPicker by mutableStateOf(false)
    var showCameraSelectionDialog by mutableStateOf(false)
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
    val navigator = koinInject<AppNavigator>()
    val journalPrefs = koinInject<JournalPreferences>()
    val isCompressPhotosEnabled by journalPrefs.isCompressPhotosEnabled().collectAsStateWithLifecycle(initialValue = false)
    val photoQuality by journalPrefs.getPhotoCompressionQuality().collectAsStateWithLifecycle(initialValue = 80)
    val isCompressVideosEnabled by journalPrefs.isCompressVideosEnabled().collectAsStateWithLifecycle(initialValue = false)
    val videoQuality by journalPrefs.getVideoCompressionQuality().collectAsStateWithLifecycle(initialValue = 1080)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loadEmojiAssets(context.resources)
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }

    fun processImportedMedia(uri: Uri, isVideoHint: Boolean? = null) {
        val uriString = uri.toString()
        onAction(EditorAction.AddImage(uriString))
        onAction(EditorAction.SetMediaProcessing(uriString, true))
        coroutineScope.launch {
            val persistedPath = withContext(Dispatchers.IO) {
                MediaStorageManager.persistMedia(context, uri)
            }
            if (persistedPath != null) {
                onAction(EditorAction.ReplaceImage(uriString, persistedPath))
                val isVideo = isVideoHint ?: ExifUtils.isVideoPath(persistedPath)
                val needsCompression = ((isVideo && isCompressVideosEnabled) || (!isVideo && isCompressPhotosEnabled)) &&
                    CompressUtils.shouldCompressMedia(context, persistedPath, targetVideoHeight = videoQuality, maxPhotoDimension = 2048)
                if (needsCompression) {
                    withContext(Dispatchers.IO) {
                        try {
                            CompressUtils.saveUncompressedOriginalFile(context, persistedPath)
                            if (isVideo) {
                                val preset = when {
                                    videoQuality <= 480 -> CompressionPreset.LOW
                                    videoQuality <= 720 -> CompressionPreset.MEDIUM
                                    else -> CompressionPreset.HIGH
                                }
                                CompressUtils.compressVideo(context, persistedPath, preset)
                            } else {
                                val preset = when {
                                    photoQuality <= 70 -> CompressionPreset.LOW
                                    photoQuality <= 80 -> CompressionPreset.MEDIUM
                                    else -> CompressionPreset.HIGH
                                }
                                CompressUtils.compressPhoto(context, persistedPath, preset)
                            }
                        } finally {
                            onAction(EditorAction.SetMediaProcessing(persistedPath, false))
                        }
                    }
                } else {
                    onAction(EditorAction.SetMediaProcessing(persistedPath, false))
                }
            } else {
                onAction(EditorAction.RemoveImage(uriString))
                onAction(EditorAction.SetMediaProcessing(uriString, false))
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris.forEach { processImportedMedia(it) }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            processImportedMedia(tempCameraUri!!, isVideoHint = false)
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && tempVideoUri != null) {
            processImportedMedia(tempVideoUri!!, isVideoHint = true)
        }
    }

    if (dialogState.showCameraSelectionDialog) {
        JuneDialog(
            onDismissRequest = { dialogState.showCameraSelectionDialog = false },
            title = stringResource(R.string.capture_media),
            icon = R.drawable.add_a_photo_24px,
            confirmButton = {
                TextButton(onClick = {
                    dialogState.showCameraSelectionDialog = false
                    val uri = MediaStorageManager.createTempVideoUri(context)
                    tempVideoUri = uri
                    videoLauncher.launch(uri)
                }) { Text(stringResource(R.string.record_video)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    dialogState.showCameraSelectionDialog = false
                    val uri = MediaStorageManager.createTempPictureUri(context)
                    tempCameraUri = uri
                    photoLauncher.launch(uri)
                }) { Text(stringResource(R.string.take_photo)) }
            },
            text = { Text(stringResource(R.string.camera_options_message)) }
        )
    }

    if (dialogState.showExitDialog) {
        JuneDialog(
            onDismissRequest = { dialogState.showExitDialog = false },
            title = stringResource(R.string.save_entry),
            icon = R.drawable.file_save_24px,
            confirmButton = {
                Button(onClick = {
                    dialogState.showExitDialog = false
                    editorState.images.forEach { CompressUtils.cleanUncompressedOriginalFile(context, it) }
                    onAction(EditorAction.SaveJournal)
                    onAction(EditorAction.NavigateBack)
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    dialogState.showExitDialog = false
                    editorState.images.forEach { CompressUtils.restoreUncompressedOriginalFile(context, it) }
                    onAction(EditorAction.NavigateBack)
                }) { Text(stringResource(R.string.discard)) }
            },
            text = { Text(stringResource(R.string.save_progress_message)) }
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
        JuneDateTimePicker(
            initialDateTimeMillis = editorState.dateTime,
            mode = JuneDateTimePickerMode.BOTH,
            startOfWeek = editorState.startOfWeek,
            is24Hour = editorState.timeFormat == TimeFormat.TWENTY_FOUR_HOUR,
            initialTab = dialogState.datePickerInitialTab,
            onDateTimeSelected = { millis ->
                onAction(EditorAction.ChangeDateTime(millis))
                dialogState.showDatePicker = false
            },
            onDismiss = { dialogState.showDatePicker = false }
        )
    }

    if (dialogState.showLocationDialog) {
        AddLocationDialog(
            existingLocation = editorState.location,
            onLocationSelected = { loc -> onAction(EditorAction.SetLocation(loc)) },
            onRemoveLocation = { onAction(EditorAction.RemoveLocation) },
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
                navigator.navigateTo(Route.AddSong, isSingleTop = true)
            },
            onAddLocationClick = {
                dialogState.showAddItemSheet = false
                dialogState.showLocationDialog = true
            }
        )
    }

    if (dialogState.showEmojiPicker) {
        JournalEmojiPickerSheet(
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