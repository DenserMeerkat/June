package com.denser.june.presentation.screens.home.timeline

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.denser.june.core.R
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.media.storage.AudioStorageManager
import java.time.DayOfWeek
import com.denser.june.core.utils.combineDateAndTime
import com.denser.june.core.utils.toYearMonth
import com.denser.june.core.domain.model.enums.TimeFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.YearMonth
import java.io.File
import androidx.core.net.toUri

enum class TimelineTab(@param:androidx.annotation.StringRes val labelRes: Int, val iconRes: Int) {
    Journals(R.string.journals, R.drawable.list_alt_24px),
    Media(R.string.media, R.drawable.art_track_24px),
    Audio(R.string.audio_notes, R.drawable.mic_24px),
    Music(R.string.music, R.drawable.music_note_24px),
    Map(R.string.map, R.drawable.location_on_24px)
}

class TimelineVM(
    private val repo: JournalRepository,
    private val journalPrefs: JournalPreferences,
    private val privacyPreferences: PrivacyPreferences,
    private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var playingJournalId: String? = null
    private val _currentMonth = MutableStateFlow(
        savedStateHandle.get<String>("current_month")?.let { YearMonth.parse(it) }
            ?: YearMonth.now()
    )
    val currentMonth = _currentMonth.asStateFlow()

    private val _selectedTab = MutableStateFlow(TimelineTab.Journals)
    val selectedTab = _selectedTab.asStateFlow()

    private val _isCalendarExpanded = MutableStateFlow(true)
    val isCalendarExpanded = _isCalendarExpanded.asStateFlow()

    val startOfWeek = journalPrefs.startOfWeek().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DayOfWeek.SUNDAY
    )

    val timeFormat = journalPrefs.timeFormat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimeFormat.TWELVE_HOUR
    )

    val initialPage = Int.MAX_VALUE / 2
    private val isInternetAllowed = privacyPreferences.getIsInternetAllowedFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val journalsInMonth: StateFlow<List<Journal>> = _currentMonth.flatMapLatest { month ->
        val start = combineDateAndTime(month.atDay(1), null)
        val end = combineDateAndTime(month.atEndOfMonth(), LocalTime.of(23, 59, 59))
        repo.getJournalsByDateRange(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _activeSong = MutableStateFlow<SongDetails?>(null)
    val activeSong = _activeSong.asStateFlow()

    private val _activeAudio = MutableStateFlow<JournalAudio?>(null)
    val activeAudio = _activeAudio.asStateFlow()

    private val _activeAudioJournalId = MutableStateFlow<String?>(null)
    val activeAudioJournalId = _activeAudioJournalId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _songProgress = MutableStateFlow(0f)
    val songProgress = _songProgress.asStateFlow()

    private val _audioProgress = MutableStateFlow(0f)
    val audioProgress = _audioProgress.asStateFlow()

    val sliderProgress = _songProgress.asStateFlow()

    private enum class MediaSource { NONE, SONG, AUDIO }
    private val _currentMediaSource = MutableStateFlow(MediaSource.NONE)

    val isSongPlaying: StateFlow<Boolean> = combine(_isPlaying, _currentMediaSource) { playing, source ->
        playing && source == MediaSource.SONG
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAudioPlaying: StateFlow<Boolean> = combine(_isPlaying, _currentMediaSource) { playing, source ->
        playing && source == MediaSource.AUDIO
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _isLoading.value = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_ENDED) {
                        _isPlaying.value = false
                        _songProgress.value = 0f
                        _audioProgress.value = 0f
                        _currentMediaSource.value = MediaSource.NONE
                        seekTo(0)
                        pause()
                    }
                }
            })
        }
    }

    init {
        if (!savedStateHandle.contains("current_month")) {
            viewModelScope.launch(Dispatchers.IO) {
                val latestJournal = repo.getLatestJournal()
                if (latestJournal != null) {
                    val latestMonth = latestJournal.dateTime.toYearMonth()
                    if (latestMonth != YearMonth.now()) {
                        withContext(Dispatchers.Main) {
                            onMonthChange(latestMonth)
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                if (_isPlaying.value) {
                    when (_currentMediaSource.value) {
                        MediaSource.AUDIO -> {
                            updateAudioProgress()
                        }
                        MediaSource.SONG -> {
                            val active = _activeSong.value
                            if (active != null) {
                                val startMs = (active.clipStartMs ?: 0L).coerceAtLeast(0L)
                                val totalDuration = exoPlayer.duration.coerceAtLeast(1L)
                                val effectiveEndMs = active.clipEndMs?.coerceAtMost(totalDuration)?.takeIf { it > startMs } ?: totalDuration
                                val clipSpan = (effectiveEndMs - startMs).coerceAtLeast(1L)
                                val currentPos = exoPlayer.currentPosition

                                if (active.clipEndMs != null && currentPos >= effectiveEndMs) {
                                    exoPlayer.seekTo(startMs)
                                    exoPlayer.pause()
                                    _songProgress.value = 0f
                                } else {
                                    val offsetInClip = (currentPos - startMs).coerceIn(0L, clipSpan)
                                    _songProgress.value = offsetInClip.toFloat() / clipSpan.toFloat()
                                }
                            }
                        }
                        MediaSource.NONE -> {}
                    }
                }
                delay(100)
            }
        }
        viewModelScope.launch {
            journalsInMonth.collect {
                val currentId = playingJournalId
                if (_activeSong.value != null && currentId != null) {
                    withContext(Dispatchers.IO) {
                        val journal = repo.getJournalById(currentId)
                        if (journal == null) {
                            withContext(Dispatchers.Main) {
                                _activeSong.value = null
                                playingJournalId = null
                                exoPlayer.stop()
                                exoPlayer.clearMediaItems()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadSongIntoPlayer(song: SongDetails, autoPlay: Boolean) {
        val localFile = song.localPreviewPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0L }
        val uri = if (localFile != null) {
            localFile.toUri()
        } else if (isInternetAllowed.value && song.previewUrl != null) {
            song.previewUrl?.toUri()
        } else {
            null
        }
        if (uri != null) {
            _currentMediaSource.value = MediaSource.SONG
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
            val startMs = (song.clipStartMs ?: 0L).coerceAtLeast(0L)
            val seekTarget = if (_songProgress.value > 0f) {
                val totalDuration = exoPlayer.duration.coerceAtLeast(1L)
                val effectiveEndMs = song.clipEndMs?.coerceAtMost(totalDuration)?.takeIf { it > startMs } ?: totalDuration
                val clipSpan = (effectiveEndMs - startMs).coerceAtLeast(1L)
                startMs + (_songProgress.value * clipSpan).toLong()
            } else {
                startMs
            }
            if (seekTarget > 0) {
                exoPlayer.seekTo(seekTarget)
            }
            if (autoPlay) {
                exoPlayer.play()
            }
        }
    }

    private fun loadAudioIntoPlayer(audio: JournalAudio, autoPlay: Boolean) {
        val resolvedFile = AudioStorageManager.resolveAudioFile(context, audio.path)
        if (resolvedFile.exists()) {
            _currentMediaSource.value = MediaSource.AUDIO
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(MediaItem.fromUri(resolvedFile.toUri()))
            exoPlayer.prepare()
            val seekTarget = if (_audioProgress.value > 0f) {
                (_audioProgress.value * audio.durationMs).toLong()
            } else {
                0L
            }
            if (seekTarget > 0L) {
                exoPlayer.seekTo(seekTarget)
            }
            if (autoPlay) {
                exoPlayer.play()
            }
        }
    }

    fun onSongSelected(song: SongDetails, journalId: String, autoPlay: Boolean = true) {
        val hasLocalAudio = song.localPreviewPath?.let { File(it).exists() } == true
        if (!hasLocalAudio && !isInternetAllowed.value) return
        playingJournalId = journalId
        val sameSong = (_activeSong.value?.localPreviewPath != null && _activeSong.value?.localPreviewPath == song.localPreviewPath) ||
                (_activeSong.value?.previewUrl != null && _activeSong.value?.previewUrl == song.previewUrl)
        if (sameSong && _currentMediaSource.value == MediaSource.SONG) {
            toggleSongPlayPause()
        } else {
            _activeSong.value = song
            _songProgress.value = 0f
            loadSongIntoPlayer(song, autoPlay)
        }
    }

    fun onAudioSelected(audio: JournalAudio, journalId: String, autoPlay: Boolean = true) {
        val sameAudio = _activeAudio.value?.id == audio.id
        if (sameAudio && _currentMediaSource.value == MediaSource.AUDIO) {
            toggleAudioPlayPause()
        } else {
            _activeAudio.value = audio
            _activeAudioJournalId.value = journalId
            _audioProgress.value = 0f
            playingJournalId = journalId
            loadAudioIntoPlayer(audio, autoPlay)
        }
    }

    fun toggleSongPlayPause() {
        val active = _activeSong.value ?: return
        if (_currentMediaSource.value != MediaSource.SONG) {
            loadSongIntoPlayer(active, autoPlay = true)
            return
        }
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            val startMs = (active.clipStartMs ?: 0L).coerceAtLeast(0L)
            val totalDuration = exoPlayer.duration.coerceAtLeast(1L)
            val effectiveEndMs = active.clipEndMs?.coerceAtMost(totalDuration)?.takeIf { it > startMs } ?: totalDuration
            val currentPos = exoPlayer.currentPosition
            if (currentPos < startMs || (active.clipEndMs != null && currentPos >= effectiveEndMs)) {
                exoPlayer.seekTo(startMs)
            }
            if (exoPlayer.mediaItemCount > 0) exoPlayer.play()
        }
    }

    fun toggleAudioPlayPause() {
        val active = _activeAudio.value ?: return
        if (_currentMediaSource.value != MediaSource.AUDIO) {
            loadAudioIntoPlayer(active, autoPlay = true)
            return
        }
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.mediaItemCount > 0) exoPlayer.play()
        }
    }

    fun togglePlayPause() {
        if (_currentMediaSource.value == MediaSource.AUDIO) {
            toggleAudioPlayPause()
        } else {
            toggleSongPlayPause()
        }
    }

    fun pause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
    }

    private fun updateAudioProgress() {
        val totalDuration = exoPlayer.duration.coerceAtLeast(1L)
        val currentPos = exoPlayer.currentPosition
        _audioProgress.value = (currentPos.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    }

    fun rewind5s() {
        val active = _activeAudio.value ?: return
        if (_currentMediaSource.value != MediaSource.AUDIO) {
            loadAudioIntoPlayer(active, autoPlay = false)
        }
        val current = exoPlayer.currentPosition
        val newPos = (current - 5000L).coerceAtLeast(0L)
        exoPlayer.seekTo(newPos)
        updateAudioProgress()
    }

    fun forward10s() {
        val active = _activeAudio.value ?: return
        if (_currentMediaSource.value != MediaSource.AUDIO) {
            loadAudioIntoPlayer(active, autoPlay = false)
        }
        val current = exoPlayer.currentPosition
        val duration = exoPlayer.duration.coerceAtLeast(0L)
        val newPos = (current + 10000L).coerceAtMost(duration)
        exoPlayer.seekTo(newPos)
        updateAudioProgress()
    }

    fun onMonthChange(newMonth: YearMonth) {
        _currentMonth.value = newMonth
        savedStateHandle["current_month"] = newMonth.toString()
    }

    fun onTabChange(tab: TimelineTab) {
        _selectedTab.value = tab
    }

    fun setCalendarExpanded(expanded: Boolean) {
        _isCalendarExpanded.value = expanded
    }

    fun getMonthForPage(page: Int): YearMonth {
        val diff = page - initialPage
        return YearMonth.now().plusMonths(diff.toLong())
    }

    fun getPageForMonth(month: YearMonth): Int {
        val now = YearMonth.now()
        val diff = (month.year - now.year) * 12 + (month.monthValue - now.monthValue)
        return initialPage + diff
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }

    fun toggleBookmark(id: String) {
        viewModelScope.launch { repo.toggleBookmark(id) }
    }

    fun toggleLock(id: String) {
        viewModelScope.launch { repo.toggleLock(id) }
    }

    fun deleteJournal(id: String) {
        viewModelScope.launch { repo.softDeleteJournal(id) }
    }

    fun restoreJournal(id: String) {
        viewModelScope.launch { repo.restoreJournal(id) }
    }
}