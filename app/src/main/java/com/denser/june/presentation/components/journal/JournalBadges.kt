package com.denser.june.presentation.components.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.presentation.theme.LocalSyncEnabled

@Composable
fun JuneBadge(
    modifier: Modifier = Modifier,
    show: Boolean,
    icon: Int,
    label: String? = null,
) {
    if (!show) return
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(11.dp)
            )
            if (label != null) {
                Spacer(Modifier.width(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun JournalMetadataBadges(
    journal: Journal,
    modifier: Modifier = Modifier
) {
    val mediaCount = remember(journal.images) { journal.images.size }
    val hasMusic = remember(journal.songDetails) { journal.songDetails != null }
    val hasLocation = remember(journal.location) { journal.location != null }
    val tagCount = remember(journal.tags) { journal.tags.size }
    val isSyncEnabled = LocalSyncEnabled.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        JuneBadge(
            show = journal.isDraft,
            icon = R.drawable.edit_24px_fill
        )
        JuneBadge(
            show = isSyncEnabled,
            icon = if (journal.isSynced) R.drawable.cloud_24px else R.drawable.devices_24px,
            label = if (journal.isSynced) stringResource(R.string.cloud) else stringResource(R.string.local)
        )
        JuneBadge(
            show = mediaCount > 0,
            icon = R.drawable.photo_24px,
            label = if (mediaCount > 1) "$mediaCount" else null
        )
        JuneBadge(
            show = hasMusic,
            icon = R.drawable.music_note_24px
        )
        JuneBadge(
            show = hasLocation,
            icon = R.drawable.location_on_24px
        )
        JuneBadge(
            show = tagCount > 0,
            icon = R.drawable.sell_24px,
            label = "$tagCount"
        )
    }
}

@Composable
fun JournalActionButton(
    isBookmarked: Boolean,
    innerContainerColor: Color,
    modifier: Modifier = Modifier,
    actionIcon: Int? = null,
    onActionClick: (() -> Unit)? = null,
    onToggleBookmark: (() -> Unit)? = null
) {
    if (actionIcon == null && onToggleBookmark == null) return

    FilledIconButton(
        onClick = {
            if (actionIcon != null) {
                onActionClick?.invoke()
            } else {
                onToggleBookmark?.invoke()
            }
        },
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = innerContainerColor,
            contentColor = if (actionIcon == null && isBookmarked) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        shape = IconButtonDefaults.smallRoundShape
    ) {
        Icon(
            painter = painterResource(
                actionIcon ?: if (isBookmarked) R.drawable.bookmark_added_24px_fill
                else R.drawable.bookmark_24px
            ),
            contentDescription = if (actionIcon != null) "Action" else stringResource(R.string.bookmark),
        )
    }
}
