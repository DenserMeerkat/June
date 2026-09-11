package com.denser.june.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.presentation.screens.editor.components.JournalMosaicCard
import com.denser.june.presentation.screens.editor.components.MediaOperations
import com.denser.june.presentation.theme.LocalInternetAllowed

@Composable
fun JournalThumbnail(
    journal: Journal,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    mediaOperations: MediaOperations = remember { MediaOperations(onMediaClick = null) }
) {
    val songDetails = journal.songDetails
    val isInternetAllowed = LocalInternetAllowed.current
    val hasThumbnail = isInternetAllowed && !songDetails?.thumbnailUrl.isNullOrBlank()

    Surface(
        modifier = modifier.size(width = 96.dp, height = 60.dp),
        shape = shape,
        color = containerColor,
    ) {
        when {
            journal.images.isNotEmpty() -> {
                JournalMosaicCard(
                    mediaList = listOf(journal.images.last()),
                    enablePlayback = false,
                    modifier = Modifier.fillMaxSize(),
                    operations = mediaOperations,
                    roundedCornerShape = shape
                )
            }
            songDetails != null -> {
                if (hasThumbnail) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = songDetails.thumbnailUrl,
                            contentDescription = songDetails.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.music_note_24px),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.music_note_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.book_5_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
