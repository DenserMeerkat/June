package com.denser.june.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.denser.june.core.R
import com.denser.june.core.utils.FileUtils
import com.denser.june.presentation.theme.LocalInternetAllowed

@Composable
fun RestrictedAsyncImage(
    modifier: Modifier = Modifier,
    imageUrl: String?,
    localPath: String? = null,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    iconResource: Int = R.drawable.music_note_24px,
    iconSize: Dp = 24.dp,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
) {
    val context = LocalContext.current
    val isInternetAllowed = LocalInternetAllowed.current
    val localFile = remember(localPath) {
        FileUtils.resolveSongMedia(context, localPath, "art")
    }
    val model = remember(localFile, imageUrl, isInternetAllowed) {
        localFile ?: if (isInternetAllowed) imageUrl else null
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )

        if (model == null) {
            Icon(
                painter = painterResource(iconResource),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = iconTint
            )
        }
    }
}
