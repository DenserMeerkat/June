package com.denser.june.presentation.screens.editor.components.audio

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.presentation.components.JuneDropdownMenu
import com.denser.june.presentation.components.JuneDropdownMenuItem
import com.denser.june.presentation.utils.AudioPlayerState
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerCanvas(
    activeAudio: JournalAudio?,
    allAudios: List<JournalAudio>,
    playerState: AudioPlayerState,
    isListExpanded: Boolean,
    onToggleListExpanded: () -> Unit,
    onSelectAudio: (JournalAudio) -> Unit,
    onReorderAudios: (List<JournalAudio>) -> Unit,
    onDeleteAudio: (String) -> Unit,
    onAudioInfo: (JournalAudio) -> Unit,
    onUpdateTitle: (String, String) -> Unit,
    processingAudioIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    if (activeAudio == null) return

    val isProcessing = processingAudioIds.contains(activeAudio.id)
    val currentIndex = remember(activeAudio.id, allAudios) {
        allAudios.indexOfFirst { it.id == activeAudio.id }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val totalHeight = maxHeight
        val minCanvasHeight = 64.dp
        val maxListHeight = (totalHeight - 278.dp).coerceAtLeast(60.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 28.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                ),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    var titleValue by remember(activeAudio.id, activeAudio.title) {
                        mutableStateOf(
                            TextFieldValue(
                                text = activeAudio.title.orEmpty(),
                                selection = TextRange(activeAudio.title.orEmpty().length)
                            )
                        )
                    }
                    BasicTextField(
                        value = titleValue,
                        onValueChange = { newValue ->
                            titleValue = newValue
                            if (newValue.text != activeAudio.title.orEmpty()) {
                                onUpdateTitle(activeAudio.id, newValue.text)
                            }
                        },
                        enabled = !isProcessing,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .focusRequester(focusRequester),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (isProcessing) {
                                    Text(
                                        text = stringResource(R.string.importing_audio),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Start
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    if (titleValue.text.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.untitled),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                textAlign = TextAlign.Start
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        }
                    )

                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        FilledIconButton(
                            onClick = { showMenu = true },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert_24px),
                                contentDescription = stringResource(R.string.cd_more_options),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        JuneDropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            JuneDropdownMenuItem(
                                text = { Text("Audio Info") },
                                onClick = {
                                    showMenu = false
                                    onAudioInfo(activeAudio)
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.info_24px),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                            JuneDropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete)) },
                                onClick = {
                                    showMenu = false
                                    onDeleteAudio(activeAudio.id)
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.delete_24px),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .heightIn(min = minCanvasHeight),
                shape = RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp,
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp
                ),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                if (isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator(
                            modifier = Modifier.size(40.dp)
                        )
                    }
                } else {
                    AudioWaveformScrubber(
                        waveform = activeAudio.waveform,
                        sliderValue = playerState.sliderValue,
                        onSeek = playerState.onSeek,
                        onSeekFinished = playerState.onSeekFinished,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = !isListExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    val animatedWaveHeight by animateDpAsState(
                        targetValue = if (playerState.isPlaying) 4.dp else 0.dp,
                        label = "WaveHeight"
                    )

                    WavySlider(
                        value = playerState.sliderValue,
                        onValueChange = { if (!isProcessing) playerState.onSeek(it) },
                        onValueChangeFinished = { if (!isProcessing) playerState.onSeekFinished() },
                        enabled = !isProcessing,
                        trackThickness = 4.dp,
                        waveThickness = 2.dp,
                        waveHeight = animatedWaveHeight,
                        thumb = {
                            if (!isProcessing) {
                                Surface(
                                    modifier = Modifier.size(width = 4.dp, height = 16.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.onSurface
                                ) {}
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.onSurface,
                            activeTrackColor = MaterialTheme.colorScheme.onSurface,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    AudioSpeedTimeRow(
                        playerState = playerState,
                        isProcessing = isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isListExpanded) 8.dp else 14.dp))

            AudioControlBar(
                playerState = playerState,
                hasMultipleAudios = allAudios.size > 1,
                hasPreviousAudio = currentIndex > 0,
                hasNextAudio = currentIndex in 0 until allAudios.lastIndex,
                onPreviousAudio = {
                    if (currentIndex > 0) onSelectAudio(allAudios[currentIndex - 1])
                },
                onNextAudio = {
                    if (currentIndex in 0 until allAudios.lastIndex) onSelectAudio(allAudios[currentIndex + 1])
                },
                isProcessing = isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )

            if (allAudios.size > 1) {
                Spacer(modifier = Modifier.height(18.dp))

                AudioListPanel(
                    audios = allAudios,
                    activeAudioId = activeAudio.id,
                    isExpanded = isListExpanded,
                    onToggleExpand = onToggleListExpanded,
                    onSelectAudio = onSelectAudio,
                    onReorderAudios = onReorderAudios,
                    onDeleteAudio = onDeleteAudio,
                    onAudioInfo = onAudioInfo,
                    processingAudioIds = processingAudioIds,
                    maxListHeight = maxListHeight,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

