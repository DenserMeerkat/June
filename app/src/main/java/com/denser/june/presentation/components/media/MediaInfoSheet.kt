package com.denser.june.presentation.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.media.compress.isOptimized
import com.denser.june.core.media.exif.model.MediaMetadata
import com.denser.june.core.utils.toExifDateTimeDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoSheet(
    metadata: MediaMetadata,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    isBackedUp: Boolean = metadata.isBackedUp,
    isPendingSync: Boolean = false,
    syncProvider: String? = null,
    allowCompression: Boolean = false,
    isCompressing: Boolean = false,
    onCompressClick: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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

            val dateDisplay = metadata.dateTaken
                ?: metadata.dateTakenMillis?.takeIf { it > 946684800000L }?.toExifDateTimeDisplay()
                ?: System.currentTimeMillis().toExifDateTimeDisplay()

            Text(
                text = dateDisplay,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            val displayTitle = title?.takeIf { it.isNotBlank() }
                ?: metadata.journalTitle?.takeIf { it.isNotBlank() }
                ?: metadata.fileName
            if (displayTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                isBackedUp = isBackedUp,
                isPendingSync = isPendingSync,
                syncProvider = syncProvider,
                isCompressing = isCompressing,
                canCompressFurther = !metadata.isOptimized,
                allowCompression = allowCompression,
                onCompressClick = onCompressClick
            )
        }
    }
}

