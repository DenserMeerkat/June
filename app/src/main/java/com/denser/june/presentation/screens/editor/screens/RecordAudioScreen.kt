package com.denser.june.presentation.screens.editor.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.media.exif.MediaMetadataReader
import com.denser.june.core.media.storage.AudioStorageManager
import com.denser.june.core.utils.FileUtils
import com.denser.june.presentation.components.JuneBackButton
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.components.media.MediaInfoSheet
import com.denser.june.presentation.screens.editor.EditorAction
import com.denser.june.presentation.screens.editor.EditorVM
import com.denser.june.presentation.screens.editor.components.audio.*
import com.denser.june.presentation.utils.AudioRecordManager
import com.denser.june.presentation.utils.rememberAudioPlayerState
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordAudioScreen(
    viewModel: EditorVM,
    initialAudioId: String? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val recordManager = remember { AudioRecordManager(context, scope) }
    val isRecording by recordManager.isRecording.collectAsStateWithLifecycle()
    val isPaused by recordManager.isPaused.collectAsStateWithLifecycle()
    val elapsedMillis by recordManager.elapsedMillis.collectAsStateWithLifecycle()
    val amplitudes by recordManager.amplitudes.collectAsStateWithLifecycle()

    var isStudioMode by remember(state.audios) {
        mutableStateOf(state.audios.isEmpty())
    }

    var selectedAudioId by remember {
        mutableStateOf(
            initialAudioId?.takeIf { id -> state.audios.any { it.id == id } }
                ?: state.audios.firstOrNull()?.id
        )
    }

    LaunchedEffect(state.audios) {
        if (selectedAudioId == null || !state.audios.any { it.id == selectedAudioId }) {
            selectedAudioId = state.audios.firstOrNull()?.id
        }
    }

    LaunchedEffect(initialAudioId) {
        if (initialAudioId != null && state.audios.any { it.id == initialAudioId }) {
            selectedAudioId = initialAudioId
            isStudioMode = false
        }
    }

    val activeAudio = remember(state.audios, selectedAudioId) {
        state.audios.firstOrNull { it.id == selectedAudioId } ?: state.audios.firstOrNull()
    }

    val activeAudioFile = remember(activeAudio) {
        activeAudio?.let { AudioStorageManager.resolveAudioFile(context, it.path) }
    }

    val playerState = rememberAudioPlayerState(
        file = activeAudioFile,
        initialDurationMs = activeAudio?.durationMs ?: 0L
    )

    var audioForInfo by remember { mutableStateOf<JournalAudio?>(null) }
    var isListExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.audios.size) {
        if (state.audios.size <= 1) {
            isListExpanded = false
        }
    }

    fun handleDeleteAudio(idToDelete: String) {
        if (activeAudio?.id == idToDelete) {
            if (playerState.isPlaying) {
                playerState.onPlayPause()
            }
            val remaining = state.audios.filterNot { it.id == idToDelete }
            if (remaining.isNotEmpty()) {
                val currentIndex = state.audios.indexOfFirst { it.id == idToDelete }
                val nextIndex = currentIndex.coerceIn(0, remaining.lastIndex)
                selectedAudioId = remaining[nextIndex].id
            } else {
                selectedAudioId = null
                isStudioMode = true
                isListExpanded = false
            }
        }
        viewModel.onAction(EditorAction.RemoveAudio(idToDelete))
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val placeholderId = UUID.randomUUID().toString()
            val displayName = FileUtils.getDisplayName(context, uri) ?: "Audio"
            val originalLastModified = FileUtils.getDocumentLastModified(context, uri)
            val placeholderAudio = JournalAudio(
                id = placeholderId,
                path = "",
                durationMs = 0L,
                title = displayName.substringBeforeLast('.').ifBlank { displayName },
                format = displayName.substringAfterLast('.', "m4a"),
                size = 0L,
                waveform = emptyList(),
                createdAt = originalLastModified ?: System.currentTimeMillis()
            )
            viewModel.onAction(EditorAction.ImportAudio(uri, placeholderAudio))
            selectedAudioId = placeholderId
            isStudioMode = false
        }
    }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    val openAppSettings = {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            recordManager.start()
        } else {
            Toast.makeText(context, "Microphone permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    fun startRecordingWithPermission() {
        if (playerState.isPlaying) {
            playerState.onPlayPause()
        }
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        hasMicPermission = isGranted
        if (isGranted) {
            recordManager.start()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopAndSaveRecording() {
        val recordedFile = recordManager.stop()
        if (recordedFile != null) {
            val sampledWaveform = sampleAmplitudes(amplitudes, 80)
            val savedAudio = AudioStorageManager.saveRecordedAudio(
                context = context,
                tempFile = recordedFile,
                title = null,
                waveform = sampledWaveform
            )
            if (savedAudio != null) {
                viewModel.onAction(EditorAction.AddAudio(savedAudio))
                selectedAudioId = savedAudio.id
                isStudioMode = false
            }
        }
    }

    BackHandler {
        if (isRecording) {
            recordManager.cancel()
        }
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                recordManager.cancel()
            }
        }
    }

    val screenTitle = when {
        isRecording -> "Recording…"
        isStudioMode || activeAudio == null -> stringResource(R.string.record_audio)
        else -> stringResource(R.string.audio_notes)
    }

    Scaffold(
        topBar = {
            JuneTopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    JuneBackButton(onClick = {
                        if (isRecording) {
                            recordManager.cancel()
                        }
                        onNavigateBack()
                    })
                },
                actions = {
                    if (isRecording) {
                        OutlinedButton(
                            onClick = {
                                recordManager.cancel()
                                if (state.audios.isNotEmpty()) {
                                    isStudioMode = false
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!isStudioMode && activeAudio != null) {
                                FilledTonalIconButton(
                                    onClick = {
                                        if (playerState.isPlaying) {
                                            playerState.onPlayPause()
                                        }
                                        isStudioMode = true
                                    },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.mic_24px),
                                        contentDescription = stringResource(R.string.record_audio)
                                    )
                                }
                            }
                            FilledTonalIconButton(
                                onClick = { filePicker.launch("audio/*") },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.folder_open_24px),
                                    contentDescription = stringResource(R.string.attach_audio)
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val showRecordingStudio = isRecording || isStudioMode || activeAudio == null

            AnimatedContent(
                targetState = showRecordingStudio,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "StudioReviewSwitch"
            ) { studioActive ->
                if (studioActive) {
                    AudioStudioCanvas(
                        isRecording = isRecording,
                        elapsedMillis = elapsedMillis,
                        amplitudes = amplitudes,
                        isPaused = isPaused,
                        onStartRecording = { startRecordingWithPermission() },
                        onPauseToggle = {
                            if (isPaused) recordManager.resume() else recordManager.pause()
                        },
                        onStop = { stopAndSaveRecording() },
                        hasPermission = hasMicPermission,
                        onOpenSettings = openAppSettings
                    )
                } else {
                    AudioPlayerCanvas(
                        activeAudio = activeAudio,
                        allAudios = state.audios,
                        playerState = playerState,
                        isListExpanded = isListExpanded,
                        onToggleListExpanded = { isListExpanded = !isListExpanded },
                        onSelectAudio = { audio -> selectedAudioId = audio.id },
                        onReorderAudios = { newList -> viewModel.onAction(EditorAction.ReorderAudios(newList)) },
                        onDeleteAudio = { id -> handleDeleteAudio(id) },
                        onAudioInfo = { audio -> audioForInfo = audio },
                        onUpdateTitle = { id, newTitle -> viewModel.onAction(EditorAction.UpdateAudioTitle(id, newTitle)) },
                        processingAudioIds = state.processingAudioIds
                    )
                }
            }
        }
    }

    if (audioForInfo != null) {
        val currentInfoAudio = state.audios.firstOrNull { it.id == audioForInfo?.id } ?: audioForInfo!!
        val metadata = remember(currentInfoAudio) {
            MediaMetadataReader.readMediaMetadata(
                context = context,
                path = currentInfoAudio.path,
                fallbackDateTime = currentInfoAudio.createdAt.takeIf { it > 946684800000L },
                journalTitle = currentInfoAudio.title
            )
        }
        MediaInfoSheet(
            metadata = metadata,
            title = currentInfoAudio.title,
            onDismiss = { audioForInfo = null }
        )
    }
}

private fun sampleAmplitudes(amplitudes: List<Float>, targetSize: Int): List<Float> {
    if (amplitudes.isEmpty()) return emptyList()
    if (amplitudes.size <= targetSize) {
        val max = amplitudes.maxOrNull() ?: 1f
        val factor = if (max > 0.05f) 1f / max else 1f
        return amplitudes.map { (it * factor).coerceIn(0.08f, 1f) }
    }
    val step = amplitudes.size.toFloat() / targetSize
    val sampled = ArrayList<Float>(targetSize)
    for (i in 0 until targetSize) {
        val startIdx = (i * step).toInt()
        val endIdx = ((i + 1) * step).toInt().coerceAtMost(amplitudes.size)
        var maxAmp = 0f
        for (j in startIdx until endIdx) {
            if (amplitudes[j] > maxAmp) maxAmp = amplitudes[j]
        }
        sampled.add(maxAmp)
    }
    val overallMax = sampled.maxOrNull() ?: 1f
    val factor = if (overallMax > 0.05f) 1f / overallMax else 1f
    return sampled.map { (it * factor).coerceIn(0.08f, 1f) }
}
