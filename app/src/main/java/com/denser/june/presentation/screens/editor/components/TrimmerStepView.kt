package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.utils.FileUtils
import com.denser.june.core.utils.toAudioTimestamp
import com.denser.june.presentation.components.RestrictedAsyncImage
import com.denser.june.presentation.utils.rememberSongPlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TrimmerStepView(
    modifier: Modifier = Modifier,
    songDetails: SongDetails,
    onBindSaveAction: (() -> Unit) -> Unit,
    onSaveClip: (song: SongDetails, startMs: Long, endMs: Long?) -> Unit
) {
    val localPath = songDetails.localPreviewPath
    var totalDurationMs by remember(songDetails) { mutableLongStateOf(30_000L) }

    LaunchedEffect(localPath) {
        if (localPath != null) {
            withContext(Dispatchers.IO) {
                val dur = FileUtils.getAudioDurationMs(localPath)
                if (dur != null && dur > 0L) {
                    totalDurationMs = dur
                }
            }
        }
    }

    var clipDurationMs by remember(songDetails) {
        val start = songDetails.clipStartMs
        val end = songDetails.clipEndMs
        val initialDuration = if (end != null && end > start) {
            end - start
        } else {
            totalDurationMs
        }
        mutableLongStateOf(initialDuration.coerceIn(5_000L, totalDurationMs.coerceAtLeast(5_000L)))
    }

    var startMs by remember(songDetails) {
        val maxStart = (totalDurationMs - clipDurationMs).coerceAtLeast(0L)
        mutableLongStateOf(songDetails.clipStartMs.coerceIn(0L, maxStart))
    }

    LaunchedEffect(totalDurationMs) {
        if (clipDurationMs > totalDurationMs) {
            clipDurationMs = totalDurationMs.coerceAtLeast(5_000L)
        }
        val maxStart = (totalDurationMs - clipDurationMs).coerceAtLeast(0L)
        if (startMs > maxStart) {
            startMs = maxStart
        }
    }

    val endMs = remember(startMs, clipDurationMs, totalDurationMs) {
        (startMs + clipDurationMs).coerceAtMost(totalDurationMs)
    }

    val auditionPlayerState = rememberSongPlayerState(
        previewUrl = songDetails.previewUrl,
        localPreviewPath = songDetails.localPreviewPath,
        clipStartMs = startMs,
        clipEndMs = endMs,
        autoRepeat = true
    )

    LaunchedEffect(auditionPlayerState.exoPlayer) {
        auditionPlayerState.exoPlayer?.let { player ->
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY && player.duration > 0L) {
                        totalDurationMs = player.duration
                    }
                }
            }
            player.addListener(listener)
            if (player.playbackState == Player.STATE_READY && player.duration > 0L) {
                totalDurationMs = player.duration
            }
        }
    }

    LaunchedEffect(songDetails, startMs, endMs, clipDurationMs, totalDurationMs) {
        onBindSaveAction {
            auditionPlayerState.exoPlayer?.pause()
            val finalEndMs = if (clipDurationMs >= totalDurationMs && startMs == 0L) null else endMs
            onSaveClip(songDetails, startMs, finalEndMs)
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        EditScopeChip(scope = EditSongScope.JournalOnly)

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .size(160.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            RestrictedAsyncImage(
                imageUrl = songDetails.thumbnailUrl,
                localPath = songDetails.localThumbnailPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = songDetails.title,
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = songDetails.artistName,
            modifier = Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        val isDraggable = clipDurationMs < totalDurationMs

        ControlsRow(
            modifier = Modifier.padding(horizontal = 20.dp),
            startMs = startMs,
            clipDurationMs = clipDurationMs,
            totalDurationMs = totalDurationMs,
            isPlaying = auditionPlayerState.isPlaying,
            onPlayPause = auditionPlayerState.onPlayPause,
            onDurationSelected = { newDur ->
                clipDurationMs = newDur
                val maxStart = (totalDurationMs - newDur).coerceAtLeast(0L)
                startMs = startMs.coerceIn(0L, maxStart)
                auditionPlayerState.exoPlayer?.seekTo(startMs)
            },
            onSeekStartMs = { newStartMs ->
                if (isDraggable) {
                    startMs = newStartMs
                    auditionPlayerState.exoPlayer?.seekTo(newStartMs)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AlternatingWaveformScrubber(
            modifier = Modifier.fillMaxWidth(),
            totalDurationMs = totalDurationMs,
            clipDurationMs = clipDurationMs,
            startMs = startMs,
            playbackFraction = auditionPlayerState.sliderValue,
            isPlaying = auditionPlayerState.isPlaying,
            onScrollStartMs = { newStartMs ->
                if (isDraggable) {
                    startMs = newStartMs
                }
            },
            onDragFinished = {
                auditionPlayerState.exoPlayer?.seekTo(startMs)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = startMs.toAudioTimestamp(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = endMs.toAudioTimestamp(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}
