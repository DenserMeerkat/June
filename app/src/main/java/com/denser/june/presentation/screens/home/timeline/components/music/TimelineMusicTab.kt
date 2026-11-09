package com.denser.june.presentation.screens.home.timeline.components.music

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.media.storage.SongMediaStorage
import com.denser.june.core.utils.toLocalDate
import com.denser.june.presentation.components.journal.DayJournalGroupData
import com.denser.june.presentation.components.sheets.SongInfoSheet
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.home.timeline.TimelineVM
import com.denser.june.presentation.screens.home.timeline.components.common.EmptyStateMessage
import com.denser.june.presentation.theme.LocalInternetAllowed
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TimelineMusicTab(
    journals: List<Journal>,
    bottomPadding: Dp,
    viewModel: TimelineVM = koinViewModel()
) {
    val context = LocalContext.current
    val navigator = koinInject<AppNavigator>()
    val isInternetAllowed = LocalInternetAllowed.current
    val musicJournals = remember(journals) {
        journals.filter { it.songDetails != null }
    }

    val dayGroups = remember(musicJournals) {
        musicJournals
            .groupBy { it.dateTime.toLocalDate() }
            .map { (date, journalsOnDay) -> DayJournalGroupData(date, journalsOnDay) }
    }

    val activeSong by viewModel.activeSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isSongPlaying.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val progress by viewModel.songProgress.collectAsStateWithLifecycle()

    var selectedSongEntryForOptions by remember { mutableStateOf<Pair<Journal, SongDetails>?>(null) }
    val currentSongEntryForOptions = remember(selectedSongEntryForOptions, journals) {
        val (selectedJournal, selectedSong) = selectedSongEntryForOptions ?: return@remember null
        val updatedJournal = journals.find { it.id == selectedJournal.id } ?: selectedJournal
        val updatedSong = updatedJournal.songDetails ?: selectedSong
        updatedJournal to updatedSong
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
    LaunchedEffect(musicJournals) {
        if (activeSong == null && musicJournals.isNotEmpty()) {
            viewModel.onSongSelected(
                musicJournals.first().songDetails!!,
                musicJournals.first().id,
                false
            )
            viewModel.pause()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (musicJournals.isEmpty()) {
            EmptyStateMessage(stringResource(R.string.no_music_this_month))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = bottomPadding + 92.dp,
                    top = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dayGroups.forEach { dayGroup ->
                    if (dayGroup.journals.size > 1) {
                        item(key = "music_day_group_${dayGroup.date}") {
                            TimelineMusicDayGroup(
                                journals = dayGroup.journals,
                                activeSong = activeSong,
                                onSongClick = { song, journalId ->
                                    viewModel.onSongSelected(song, journalId)
                                },
                                onSongLongClick = { song, journalId ->
                                    val journal = dayGroup.journals.firstOrNull { it.id == journalId }
                                    if (journal != null) {
                                        selectedSongEntryForOptions = journal to song
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        val singleJournal = dayGroup.journals.first()
                        val song = singleJournal.songDetails!!
                        val isSongActive = (activeSong?.localPreviewPath != null && activeSong?.localPreviewPath == song.localPreviewPath) ||
                                (activeSong?.previewUrl != null && activeSong?.previewUrl == song.previewUrl)
                        item(key = "single_music_${singleJournal.id}") {
                            TimelineMusicCard(
                                journal = singleJournal,
                                song = song,
                                isActive = isSongActive,
                                onClick = { viewModel.onSongSelected(song, singleJournal.id) },
                                onLongClick = {
                                    selectedSongEntryForOptions = singleJournal to song
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        val hasLocalAudio = remember(activeSong?.localPreviewPath) {
            SongMediaStorage.resolveSongMedia(context, activeSong?.localPreviewPath, "library") != null
        }
        if (activeSong != null && (isInternetAllowed || hasLocalAudio)) {
            DockedMusicPlayer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding),
                song = activeSong!!,
                isPlaying = isPlaying,
                isLoading = isLoading,
                progress = progress,
                onPlayPause = { viewModel.toggleSongPlayPause() }
            )
        }

        if (currentSongEntryForOptions != null) {
            val (journal, song) = currentSongEntryForOptions
            SongInfoSheet(
                song = song,
                journal = journal,
                onDismiss = { selectedSongEntryForOptions = null },
                onOpenJournal = {
                    navigator.navigateTo(Route.Editor(journal.id), isSingleTop = true)
                }
            )
        }
    }
}
