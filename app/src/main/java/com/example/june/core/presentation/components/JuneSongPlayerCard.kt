package com.example.june.core.presentation.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import androidx.core.net.toUri
import com.example.june.core.presentation.utils.rememberDynamicThemeColors

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JuneSongPlayerCard(
    details: SongDetails,
    isPlaying: Boolean,
    isLoading: Boolean,
    sliderValue: Float,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit
) {
    val context = LocalContext.current
    var showLinksMenu by remember { mutableStateOf(false) }

    val themeColors = rememberDynamicThemeColors(details.thumbnailUrl)

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
            color = themeColors.surface,
            contentColor = themeColors.onSurface,
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
                        .alpha(0.25f)
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
                                .clip(RoundedCornerShape(16.dp))
                                .shadow(8.dp, RoundedCornerShape(16.dp))
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier.offset(y = (-12).dp),
                        ) {
                            ListenChip(
                                onClick = { showLinksMenu = true },
                                containerColor = themeColors.primaryContainer, 
                                contentColor = themeColors.onPrimaryContainer
                            )

                            DropdownMenu(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                expanded = showLinksMenu,
                                onDismissRequest = { showLinksMenu = false },
                                shape = RoundedCornerShape(24.dp),
                                offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 4.dp)
                            ) {
                                availableLinks.forEach { (platform, url) ->
                                    DropdownMenuItem(
                                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                                        text = {
                                            Text(
                                                text = platform,
                                                color = Color.White
                                            )
                                        },
                                        onClick = {
                                            showLinksMenu = false
                                            val intent = Intent(Intent.ACTION_VIEW, url?.toUri())
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
                        val spotifyUrl = details.links.spotify
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, spotifyUrl?.toUri())
                                    context.startActivity(intent)
                                }
                        ) {
                            Surface(
                                modifier = Modifier.size(20.dp),
                                shape = CircleShape,
                                color = themeColors.onPrimaryContainer,
                                content = {}
                            )
                            Icon(
                                painter = painterResource(R.drawable.spotify),
                                contentDescription = "Open Spotify",
                                modifier = Modifier.size(24.dp),
                                tint = themeColors.primaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = details.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = themeColors.onSurface 
                        )
                        Text(
                            text = details.artistName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.onSurfaceVariant, 
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
                                if (!isLoading && details.previewUrl != null) {
                                    Surface(
                                        modifier = Modifier
                                            .size(width = 4.dp, height = 16.dp),
                                        shape = CircleShape,
                                        color = themeColors.onSurface 
                                    ) {}
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = themeColors.onSurface,
                                activeTrackColor = themeColors.onSurface, 
                                inactiveTrackColor = themeColors.onSurface.copy(alpha = 0.2f),
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(16.dp))
                        PlayPauseButton(
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            enabled = details.previewUrl != null,
                            onClick = onPlayPause,
                            containerColor = themeColors.primaryContainer, 
                            contentColor = themeColors.onPrimaryContainer 
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
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    FilledIconToggleButton(
        checked = isPlaying,
        onCheckedChange = { if (!isLoading) onClick() },
        enabled = enabled,
        modifier = Modifier
            .size(width = 56.dp, height = 40.dp),
        shapes = IconButtonDefaults.toggleableShapes(),

        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            checkedContainerColor = containerColor,
            checkedContentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularWavyProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = contentColor,
            )
        } else {
            Icon(
                painter = painterResource(
                    if (isPlaying) R.drawable.pause_24px else R.drawable.play_arrow_24px
                ),
                contentDescription = if (isPlaying) "Pause" else "Play",
            )
        }
    }
}

@Composable
fun ListenChip(
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, end = 12.dp, bottom = 4.dp),
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