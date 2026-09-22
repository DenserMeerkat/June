package com.denser.june.presentation.components

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.denser.june.core.R
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.domain.model.SongSourceType
import com.denser.june.core.utils.FileUtils
import com.denser.june.presentation.theme.LocalInternetAllowed
import com.denser.june.presentation.utils.rememberDynamicThemeColors
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JuneSongPlayerCard(
    details: SongDetails,
    isPlaying: Boolean,
    isLoading: Boolean,
    sliderValue: Float,
    isRepeatEnabled: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInternetAllowed = LocalInternetAllowed.current
    val localArtFile = remember(details.localThumbnailPath) {
        FileUtils.resolveSongMedia(context, details.localThumbnailPath, "art")
    }
    val artModel = remember(localArtFile, details.thumbnailUrl, isInternetAllowed) {
        localArtFile ?: if (isInternetAllowed) details.thumbnailUrl else null
    }
    val themeColors = rememberDynamicThemeColors(artModel)
    val hasAudio = remember(details.previewUrl, details.localPreviewPath, isInternetAllowed) {
        val hasLocal = FileUtils.resolveSongMedia(context, details.localPreviewPath, "library") != null
        hasLocal || (isInternetAllowed && details.previewUrl != null)
    }

    var rippleTrigger by remember { mutableIntStateOf(0) }
    val rippleScale = remember { Animatable(1f) }
    val rippleAlpha = remember { Animatable(0f) }

    LaunchedEffect(rippleTrigger) {
        if (rippleTrigger > 0) {
            rippleScale.snapTo(1f)
            rippleAlpha.snapTo(0.7f)
            launch {
                rippleScale.animateTo(
                    targetValue = 24f,
                    animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                rippleAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
                )
            }
        }
    }

    val availableLinks = remember(details.links) {
        details.links.toAvailableLinks()
    }

    val hasAlbumCover = artModel != null
    val surfaceColor =
        if (hasAlbumCover) themeColors.surface else MaterialTheme.colorScheme.surfaceContainerLowest

    Surface(
        color = surfaceColor,
        contentColor = themeColors.onSurface,
        shape = RoundedCornerShape(32.dp),
        modifier = modifier,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val heightVal = maxHeight
            val albumSize = heightVal * 0.42f
            val rippleSize = heightVal * 0.2f
            val iconSize = heightVal * 0.1f

            RestrictedAsyncImage(
                imageUrl = details.thumbnailUrl,
                localPath = details.localThumbnailPath,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.25f)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 36.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(rippleSize)
                        .scale(rippleScale.value)
                        .alpha(rippleAlpha.value)
                        .background(themeColors.primaryContainer.copy(alpha = 0.5f), CircleShape)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp, 16.dp, 16.dp, 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(albumSize)
                            .clip(RoundedCornerShape(16.dp))
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .background(themeColors.secondaryContainer)
                    ) {
                        RestrictedAsyncImage(
                            imageUrl = details.thumbnailUrl,
                            localPath = details.localThumbnailPath,
                            contentDescription = "Album Art",
                            iconSize = 32.dp,
                            iconTint = themeColors.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    val isLocal =
                        details.sourceType == SongSourceType.LOCAL_FILE || (details.localPreviewPath != null && details.previewUrl == null)

                    if (!isLocal && availableLinks.isNotEmpty()) {
                        Box(
                            modifier = Modifier.offset(y = (-12).dp),
                        ) {
                            var showLinksMenu by remember { mutableStateOf(false) }
                            ListenDropdownMenu(
                                availableLinks = availableLinks,
                                expanded = showLinksMenu,
                                onDismissRequest = { showLinksMenu = false },
                                trigger = {
                                    ListenChip(
                                        onClick = { showLinksMenu = true },
                                        containerColor = themeColors.primaryContainer,
                                        contentColor = themeColors.onPrimaryContainer
                                    )
                                }
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    }

                    if (isLocal) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(iconSize)
                        ) {
                            Surface(
                                modifier = Modifier.size(iconSize * 0.83f),
                                shape = CircleShape,
                                color = themeColors.primaryContainer,
                                content = {}
                            )
                            Icon(
                                painter = painterResource(R.drawable.folder_open_24px),
                                contentDescription = "Local Audio",
                                modifier = Modifier.size(iconSize * 0.55f),
                                tint = themeColors.onPrimaryContainer
                            )
                        }
                    } else {
                        val activeProvider = details.previewUrlProvider ?: "Spotify"
                        val activeUrl = when (activeProvider) {
                            "Spotify" -> details.links.spotify
                            "Deezer" -> details.links.deezer
                            "Apple Music" -> details.links.appleMusic
                            else -> details.links.spotify
                        } ?: availableLinks.firstOrNull()?.second

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(iconSize)
                                .clip(CircleShape)
                                .clickable(enabled = activeUrl != null) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, activeUrl?.toUri())
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                        ) {
                            Surface(
                                modifier = Modifier.size(iconSize * 0.83f),
                                shape = CircleShape,
                                color = themeColors.onPrimaryContainer,
                                content = {}
                            )
                            Icon(
                                painter = painterResource(getPlatformIcon(activeProvider)),
                                contentDescription = "Open $activeProvider",
                                modifier = Modifier.size(iconSize),
                                tint = themeColors.primaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(end = 80.dp),
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
                        enabled = hasAudio,
                        trackThickness = 4.dp,
                        waveThickness = 2.dp,
                        waveHeight = 4.dp,
                        thumb = {
                            if (!isLoading && hasAudio) {
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
                    Spacer(Modifier.width(12.dp))
                    FilledIconToggleButton(
                        checked = isRepeatEnabled,
                        onCheckedChange = { onToggleRepeat() },
                        modifier = Modifier.size(heightVal * 0.13f),
                        shapes = IconButtonDefaults.toggleableShapes(),
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            containerColor = Color.Transparent,
                            checkedContainerColor = themeColors.primaryContainer
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.repeat_24px),
                            contentDescription = "Toggle Repeat",
                            tint = if (isRepeatEnabled) themeColors.onPrimaryContainer else themeColors.onSurface,
                            modifier = Modifier.size(heightVal * 0.08f)
                        )
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
            ) {
                PlayPauseButton(
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    enabled = hasAudio,
                    onClick = {
                        rippleTrigger++
                        onPlayPause()
                    },
                    containerColor = themeColors.primaryContainer,
                    contentColor = themeColors.onPrimaryContainer,
                )
            }
        }
    }
}
