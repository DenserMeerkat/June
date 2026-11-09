package com.denser.june.presentation.components.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.media.compress.isOptimized
import com.denser.june.core.media.exif.model.MediaMetadata
import com.denser.june.core.utils.toExifDateTimeDisplay
import com.denser.june.presentation.components.media.MediaDetailsList
import com.denser.june.presentation.screens.home.timeline.components.journal.TimelineJournalCard

@Composable
fun MediaInfoSheetContent(
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
        val dateDisplay = metadata?.dateTaken
            ?: (dateTime?.takeIf { it > 946684800000L }
                ?: parentJournal?.dateTime?.takeIf { it > 946684800000L }
                ?: System.currentTimeMillis()).toExifDateTimeDisplay()

        Text(
            text = dateDisplay,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoSheet(
    metadata: MediaMetadata,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dateTime: Long? = metadata.dateTakenMillis,
    parentJournal: Journal? = null,
    isJournalNavigationEnabled: Boolean = parentJournal != null,
    isBackedUp: Boolean = metadata.isBackedUp,
    isPendingSync: Boolean = false,
    syncProvider: String? = null,
    isCompressing: Boolean = false,
    canCompressFurther: Boolean = !metadata.isOptimized,
    allowCompression: Boolean = false,
    onJournalClick: () -> Unit = {},
    onCompressClick: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            MediaInfoSheetContent(
                metadata = metadata,
                dateTime = dateTime,
                parentJournal = parentJournal,
                isJournalNavigationEnabled = isJournalNavigationEnabled,
                isMediaBackedUp = isBackedUp,
                isPendingSync = isPendingSync,
                syncProvider = syncProvider,
                isCompressing = isCompressing,
                canCompressFurther = canCompressFurther,
                allowCompression = allowCompression,
                onJournalClick = onJournalClick,
                onCompressClick = onCompressClick
            )
        }
    }
}
