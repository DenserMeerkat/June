package com.example.june.core.presentation.screens.journal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.june.R
import java.io.File

@Composable
fun JournalMediaPreview(
    mediaPaths: List<String>,
    isEditMode: Boolean,
    imageLoader: ImageLoader,
    onRemoveMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(mediaPaths) { mediaPath ->
            JournalMediaItem(
                mediaPath = mediaPath,
                isEditMode = isEditMode,
                imageLoader = imageLoader,
                onRemove = { onRemoveMedia(mediaPath) }
            )
        }
    }
}

@Composable
private fun JournalMediaItem(
    mediaPath: String,
    isEditMode: Boolean,
    imageLoader: ImageLoader,
    onRemove: () -> Unit
) {
    Box {
        AsyncImage(
            model = File(mediaPath),
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .aspectRatio(0.75f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        if (mediaPath.endsWith("mp4", ignoreCase = true)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(0.75f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.play_circle_24px),
                    contentDescription = "Video",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
            }
        }

        if (isEditMode) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.close_24px),
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}