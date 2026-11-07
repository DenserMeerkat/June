package com.denser.june.presentation.screens.home.timeline.components.audio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.utils.toLocalDate
import com.denser.june.presentation.components.journal.DayJournalGroupData
import com.denser.june.presentation.components.sheets.AudioInfoSheet
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.home.timeline.TimelineVM
import com.denser.june.presentation.screens.home.timeline.components.common.EmptyStateMessage
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TimelineAudioTab(
    journals: List<Journal>,
    bottomPadding: Dp,
    viewModel: TimelineVM = koinViewModel()
) {
    val navigator = koinInject<AppNavigator>()
    val audioJournals = remember(journals) {
        journals.filter { it.audios.isNotEmpty() }
    }

    val dayGroups = remember(audioJournals) {
        audioJournals
            .groupBy { it.dateTime.toLocalDate() }
            .map { (date, journalsOnDay) -> DayJournalGroupData(date, journalsOnDay) }
    }

    val activeAudio by viewModel.activeAudio.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isAudioPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.audioProgress.collectAsStateWithLifecycle()

    var selectedAudioEntryForOptions by remember { mutableStateOf<DayAudioEntry?>(null) }
    val currentAudioEntryForOptions = remember(selectedAudioEntryForOptions, journals) {
        val selected = selectedAudioEntryForOptions ?: return@remember null
        val updatedJournal = journals.find { it.id == selected.journal.id } ?: selected.journal
        val updatedAudio = updatedJournal.audios.find { it.id == selected.audio.id } ?: selected.audio
        DayAudioEntry(updatedJournal, updatedAudio)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.pause()
        }
    }

    LaunchedEffect(audioJournals) {
        if (activeAudio == null && audioJournals.isNotEmpty()) {
            val firstJournal = audioJournals.first()
            val firstAudio = firstJournal.audios.first()
            viewModel.onAudioSelected(firstAudio, firstJournal.id, autoPlay = false)
            viewModel.pause()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (audioJournals.isEmpty()) {
            EmptyStateMessage(stringResource(R.string.no_audio_this_month))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = bottomPadding + 120.dp,
                    top = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dayGroups.forEach { dayGroup ->
                    val dayAudios = dayGroup.journals.flatMap { journal ->
                        journal.audios.map { audio -> DayAudioEntry(journal, audio) }
                    }

                    if (dayAudios.size > 1) {
                        item(key = "audio_day_group_${dayGroup.date}") {
                            TimelineAudioDayGroup(
                                entries = dayAudios,
                                activeAudio = activeAudio,
                                onAudioClick = { audio, journalId ->
                                    viewModel.onAudioSelected(audio, journalId)
                                },
                                onAudioLongClick = { audio, journalId ->
                                    val journal = dayGroup.journals.firstOrNull { it.id == journalId }
                                    if (journal != null) {
                                        selectedAudioEntryForOptions = DayAudioEntry(journal, audio)
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else if (dayAudios.isNotEmpty()) {
                        val singleEntry = dayAudios.first()
                        item(key = "single_audio_${singleEntry.audio.id}") {
                            TimelineAudioCard(
                                journal = singleEntry.journal,
                                audio = singleEntry.audio,
                                isActive = activeAudio?.id == singleEntry.audio.id,
                                onClick = {
                                    viewModel.onAudioSelected(singleEntry.audio, singleEntry.journal.id)
                                },
                                onLongClick = {
                                    selectedAudioEntryForOptions = singleEntry
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (activeAudio != null) {
            val activeJournal = remember(activeAudio, journals) {
                journals.find { j -> j.audios.any { it.id == activeAudio?.id } }
            }
            DockedAudioPlayer(
                audio = activeAudio!!,
                journalTitle = activeJournal?.title,
                isPlaying = isPlaying,
                progress = progress,
                onPlayPause = { viewModel.toggleAudioPlayPause() },
                onRewind5s = { viewModel.rewind5s() },
                onForward10s = { viewModel.forward10s() },
                onClick = {
                    activeJournal?.let { j ->
                        navigator.navigateTo(Route.Editor(j.id), isSingleTop = true)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding)
            )
        }

        if (currentAudioEntryForOptions != null) {
            AudioInfoSheet(
                audio = currentAudioEntryForOptions.audio,
                journal = currentAudioEntryForOptions.journal,
                onDismiss = { selectedAudioEntryForOptions = null },
                onOpenJournal = {
                    navigator.navigateTo(Route.Editor(currentAudioEntryForOptions.journal.id), isSingleTop = true)
                }
            )
        }
    }
}
