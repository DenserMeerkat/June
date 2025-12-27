package com.example.june.core.presentation.screens.journal.journalmedia

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.example.june.R
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.presentation.components.JuneIconButton
import com.example.june.viewmodels.JournalVM
import org.koin.compose.koinInject
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalMediaDetailScreen(
    initialIndex: Int,
    viewModel: JournalVM
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }

    val reversedImages = state.images.reversed()
    val pagerState = rememberPagerState(initialPage = initialIndex) { state.images.size }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp
        ) { page ->
            if (page < state.images.size) {
                val path = reversedImages[page]
                val isVideo = remember(path) { path.endsWith("mp4", ignoreCase = true) }

                val isPageActive = pagerState.currentPage == page

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        DetailVideoPlayer(
                            uri = Uri.fromFile(File(path)),
                            playWhenReady = isPageActive
                        )
                    } else {
                        DetailImage(
                            path = path,
                            imageLoader = imageLoader
                        )
                    }
                }
            }
        }
        JuneIconButton(
            onClick = { navigator.navigateBack() },
            icon = R.drawable.close_24px,
            contentDescription = "Close",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .statusBarsPadding(),
        )
    }
}

@Composable
private fun DetailImage(
    path: String,
    imageLoader: ImageLoader
) {
    AsyncImage(
        model = File(path),
        imageLoader = imageLoader,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun DetailVideoPlayer(
    uri: Uri,
    playWhenReady: Boolean
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(playWhenReady) {
        exoPlayer.playWhenReady = playWhenReady
        if (!playWhenReady) {
            exoPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                useController = true
                controllerAutoShow = true
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}