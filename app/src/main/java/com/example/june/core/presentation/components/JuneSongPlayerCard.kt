package com.example.june.core.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.june.R
import com.example.june.core.domain.data_classes.SongDetails
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JuneSongPlayerCard(
    details: SongDetails,
    isPlaying: Boolean,
    sliderValue: Float,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit
) {
    val context = LocalContext.current
    var showLinksMenu by remember { mutableStateOf(false) }

    val availableLinks = remember(details.links) {
        listOf(
            "Spotify" to details.links.spotify,
            "Apple Music" to details.links.appleMusic,
            "YouTube Music" to details.links.youtubeMusic,
            "YouTube" to details.links.youtube,
            "Deezer" to details.links.deezer,
            "SoundCloud" to details.links.soundcloud,
            "Tidal" to details.links.tidal,
            "Amazon Music" to details.links.amazonMusic
        ).filter { it.second != null }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = details.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.2f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp, 16.dp, 16.dp, 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = details.thumbnailUrl,
                            contentDescription = "Album Art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(108.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier.offset(y = (-12).dp),
                        ) {
                            ListenChip(onClick = { showLinksMenu = true })
                            DropdownMenu(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                expanded = showLinksMenu,
                                onDismissRequest = { showLinksMenu = false },
                                shape = RoundedCornerShape(24.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 4.dp)
                            ) {
                                availableLinks.forEach { (platform, url) ->
                                    DropdownMenuItem(
                                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                                        text = { Text(platform) },
                                        onClick = {
                                            showLinksMenu = false
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(getPlatformIcon(platform)),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Icon(
                            painterResource(R.drawable.spotify),
                            contentDescription = null,
                            modifier = Modifier.alpha(0.8f)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = details.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = details.artistName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.width(4.dp))
                        WavySlider(
                            value = sliderValue,
                            onValueChange = onSeek,
                            onValueChangeFinished = onSeekFinished,
                            trackThickness = 4.dp,
                            waveThickness = 2.dp,
                            waveHeight = 4.dp,
                            thumb = {
                                Surface(
                                    modifier = Modifier
                                        .size(width = 4.dp, height = 16.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.onSurface
                                ) {}
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.onSurface,
                                activeTrackColor = MaterialTheme.colorScheme.onSurface,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(16.dp))
                        PlayPauseButton(
                            isPlaying = isPlaying,
                            enabled = details.previewUrl != null,
                            onClick = onPlayPause
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilledIconToggleButton(
        checked = isPlaying,
        onCheckedChange = { onClick() },
        enabled = enabled,
        modifier = Modifier
            .size(width = 56.dp, height = 40.dp),
        shapes = IconButtonDefaults.toggleableShapes(),
    ) {
        Icon(
            painter = painterResource(
                if (isPlaying) R.drawable.pause_24px else R.drawable.play_arrow_24px
            ),
            contentDescription = if (isPlaying) "Pause" else "Play",
        )
    }
}

@Composable
fun ListenChip(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.music_note_24px),
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Listen",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun getPlatformIcon(platform: String): Int {
    return when (platform) {
        "Spotify" -> R.drawable.spotify
        "Apple Music" -> R.drawable.applemusic
        "YouTube Music" -> R.drawable.youtubemusic
        "YouTube" -> R.drawable.youtube
        "SoundCloud" -> R.drawable.soundcloud
        "Deezer" -> R.drawable.deezer
        "Tidal" -> R.drawable.tidal
        "Amazon Music" -> R.drawable.amazonmusic
        else -> R.drawable.music_note_2_24px
    }
}