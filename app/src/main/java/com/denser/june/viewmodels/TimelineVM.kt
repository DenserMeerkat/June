package com.denser.june.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.denser.june.R
import com.denser.june.core.domain.JournalRepo
import com.denser.june.core.domain.data_classes.Journal
import com.denser.june.core.domain.data_classes.SongDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

enum class TimelineTab(val label: String, val iconRes: Int) {
    Journals("Journals", R.drawable.list_alt_24px),
    Media("Media", R.drawable.art_track_24px),
    Music("Music", R.drawable.music_note_24px),
    Map("Map", R.drawable.location_on_24px)
}

class TimelineVM(
    private val repo: JournalRepo,
    context: Context
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth = _currentMonth.asStateFlow()

    private val _selectedTab = MutableStateFlow(TimelineTab.Journals)
    val selectedTab = _selectedTab.asStateFlow()

    private val _isCalendarExpanded = MutableStateFlow(true)
    val isCalendarExpanded = _isCalendarExpanded.asStateFlow()

    val initialPage = Int.MAX_VALUE / 2

    @OptIn(ExperimentalCoroutinesApi::class)
    val journalsInMonth: StateFlow<List<Journal>> = _currentMonth.flatMapLatest { month ->
        val start = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = month.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
            .toEpochMilli()
        repo.getJournalsByDateRange(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _activeSong = MutableStateFlow<SongDetails?>(null)
    val activeSong = _activeSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _sliderProgress = MutableStateFlow(0f)
    val sliderProgress = _sliderProgress.asStateFlow()

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
                        _sliderProgress.value = 0f
                        seekTo(0)
                        pause()
                    }
                }
            })
        }
    }

    init {
        viewModelScope.launch {
            while (isActive) {
                if (_isPlaying.value) {
                    val duration = exoPlayer.duration.coerceAtLeast(1)
                    val position = exoPlayer.currentPosition
                    _sliderProgress.value = position.toFloat() / duration.toFloat()
                }
                delay(100)
            }
        }
    }

    fun onSongSelected(song: SongDetails, autoPlay: Boolean = true) {
        if (_activeSong.value?.previewUrl == song.previewUrl) {
            togglePlayPause()
        } else {
            _activeSong.value = song
            val url = song.previewUrl
            if (url != null) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                exoPlayer.prepare()
                if (autoPlay) {
                    exoPlayer.play()
                }
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.mediaItemCount > 0) exoPlayer.play()
        }
    }

    fun pause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
    }

    fun onMonthChange(newMonth: YearMonth) {
        _currentMonth.value = newMonth
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
}