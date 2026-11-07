package com.denser.june.presentation.screens.home.timeline.components.audio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.JournalAudio
import com.denser.june.core.utils.toAudioTimestamp
import com.denser.june.presentation.components.journal.rememberJournalGroupShape
import com.denser.june.presentation.screens.home.timeline.components.common.TimelineDateColumn

data class DayAudioEntry(
    val journal: Journal,
    val audio: JournalAudio
)

@Composable
fun TimelineAudioDayGroup(
    entries: List<DayAudioEntry>,
    activeAudio: JournalAudio?,
    onAudioClick: (JournalAudio, String) -> Unit,
    onAudioLongClick: ((JournalAudio, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val groupShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(groupShape),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        entries.forEachIndexed { index, entry ->
            val shape = rememberJournalGroupShape(index = index, totalCount = entries.size)
            val isAudioActive = activeAudio?.id == entry.audio.id

            TimelineAudioCard(
                journal = entry.journal,
                audio = entry.audio,
                shape = shape,
                isActive = isAudioActive,
                onClick = { onAudioClick(entry.audio, entry.journal.id) },
                onLongClick = onAudioLongClick?.let { { it(entry.audio, entry.journal.id) } }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineAudioCard(
    journal: Journal,
    audio: JournalAudio,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    isActive: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimelineDateColumn(dateTime = journal.dateTime)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.mic_24px),
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = audio.title?.ifBlank { stringResource(R.string.audio_notes) } ?: stringResource(R.string.audio_notes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = audio.durationMs.toAudioTimestamp(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (journal.title.isNotBlank()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = journal.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }
    }
}
