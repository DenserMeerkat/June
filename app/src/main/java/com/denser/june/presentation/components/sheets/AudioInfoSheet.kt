package com.denser.june.presentation.components.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.media.exif.MediaMetadataReader
import com.denser.june.core.utils.toExifDateTimeDisplay
import com.denser.june.presentation.components.media.MediaDetailsList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioInfoSheet(
    audio: JournalAudio,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    journal: Journal? = null,
    onOpenJournal: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val metadata = remember(audio) {
        MediaMetadataReader.readMediaMetadata(
            context = context,
            path = audio.path,
            fallbackDateTime = audio.createdAt.takeIf { it > 946684800000L } ?: journal?.dateTime?.takeIf { it > 946684800000L },
            journalTitle = audio.title
        )
    }

    val dateDisplay = remember(audio, journal) {
        (audio.createdAt.takeIf { it > 946684800000L }
            ?: journal?.dateTime?.takeIf { it > 946684800000L }
            ?: System.currentTimeMillis()).toExifDateTimeDisplay()
    }

    ItemInfoSheet(
        onDismissRequest = onDismiss,
        dateDisplay = dateDisplay,
        parentJournal = journal,
        onOpenJournal = {
            onDismiss()
            onOpenJournal?.invoke()
        },
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Details",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            MediaDetailsList(
                metadata = metadata,
                isBackedUp = metadata.isBackedUp,
                isPendingSync = false,
                syncProvider = null,
                allowCompression = false
            )
        }
    }
}
