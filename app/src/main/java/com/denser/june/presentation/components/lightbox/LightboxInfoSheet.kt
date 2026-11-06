package com.denser.june.presentation.components.lightbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.media.exif.model.MediaMetadata
import com.denser.june.core.utils.toExifDateTimeDisplay
import com.denser.june.presentation.components.media.MediaDetailsList
import com.denser.june.presentation.screens.home.timeline.components.TimelineJournalCard

@Composable
fun LightboxInfoSheetContent(
    metadata: MediaMetadata?,
    dateTime: Long?,
    parentJournal: Journal?,
    isJournalNavigationEnabled: Boolean,
    isMediaBackedUp: Boolean,
    isPendingSync: Boolean,
    syncProvider: String?,
    isCompressing: Boolean,
    canCompressFurther: Boolean,
    allowCompression: Boolean = true,
    onJournalClick: () -> Unit,
    onCompressClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }

        Text(
            text = metadata?.dateTaken
                ?: (dateTime?.takeIf { it > 946684800000L } ?: parentJournal?.dateTime?.takeIf { it > 946684800000L } ?: System.currentTimeMillis()).toExifDateTimeDisplay(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (isJournalNavigationEnabled && parentJournal != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TimelineJournalCard(
                journal = parentJournal,
                onJournalClick = onJournalClick
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Details",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        MediaDetailsList(
            metadata = metadata,
            isBackedUp = isMediaBackedUp,
            isPendingSync = isPendingSync,
            syncProvider = syncProvider,
            isCompressing = isCompressing,
            canCompressFurther = canCompressFurther,
            allowCompression = allowCompression,
            onCompressClick = onCompressClick
        )
    }
}

@Composable
fun LightboxDetailsSection(
    metadata: MediaMetadata?,
    isBackedUp: Boolean,
    isPendingSync: Boolean,
    syncProvider: String? = null,
    isCompressing: Boolean,
    canCompressFurther: Boolean = false,
    allowCompression: Boolean = true,
    onCompressClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MediaDetailsList(
        metadata = metadata,
        isBackedUp = isBackedUp,
        isPendingSync = isPendingSync,
        syncProvider = syncProvider,
        isCompressing = isCompressing,
        canCompressFurther = canCompressFurther,
        allowCompression = allowCompression,
        onCompressClick = onCompressClick,
        modifier = modifier
    )
}
