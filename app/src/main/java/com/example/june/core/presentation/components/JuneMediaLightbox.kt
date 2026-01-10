package com.example.june.core.presentation.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import com.example.june.R
import com.example.june.core.navigation.AppNavigator
import org.koin.compose.koinInject
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JuneMediaLightbox(
    mediaPaths: List<String>,
    initialIndex: Int,
) {
    val navigator = koinInject<AppNavigator>()
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) { mediaPaths.size }
    var showUI by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp
        ) { page ->
            if (page < mediaPaths.size) {
                val path = mediaPaths[page]
                val isVideo = remember(path) { path.endsWith("mp4", ignoreCase = true) }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        JuneVideoPlayer(
                            uri = Uri.fromFile(File(path)),
                            playWhenReady = false,
                            isVisible = showUI,
                            onVisibilityChange = { showUI = it }
                        )
                    } else {
                        DetailImage(
                            path = path,
                            imageLoader = imageLoader,
                            onTap = { showUI = !showUI }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showUI,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp)
                    .padding(top = 4.dp)
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    FilledIconButton(
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Back",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailImage(
    path: String,
    imageLoader: ImageLoader,
    onTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTap() }
    ) {
        AsyncImage(
            model = File(path),
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}