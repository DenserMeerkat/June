package com.denser.june.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.R
import com.denser.june.presentation.theme.LocalSyncEnabled
import java.time.LocalDate

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
fun JuneMetadataRow(
    iconRes: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.width(60.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    prefix: String,
    label: String,
    modifier: Modifier = Modifier,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapes()
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        modifier = modifier,
        shapes = shapes,
        colors = ToggleButtonDefaults.toggleButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Normal,
                        color = LocalContentColor.current.copy(alpha = 0.6f)
                    )
                ) {
                    append(prefix)
                }
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append(label)
                }
            }, style = MaterialTheme.typography.labelLarge
        )
    }
}

data class JournalCardColors(
    val containerColor: Color,
    val innerContainerColor: Color,
    val dateColor: Color
)

@Composable
fun rememberJournalCardColors(journal: Journal): JournalCardColors {
    val containerColor = when {
        journal.isDraft -> MaterialTheme.colorScheme.surfaceContainerLow
        journal.isBookmarked -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val innerContainerColor = if (journal.isBookmarked) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val dateColor = when {
        journal.isDeleted -> MaterialTheme.colorScheme.error
        journal.isDraft -> MaterialTheme.colorScheme.tertiary
        journal.isBookmarked -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    }
    return remember(journal.isDraft, journal.isBookmarked, journal.isDeleted) {
        JournalCardColors(containerColor, innerContainerColor, dateColor)
    }
}

@Composable
fun rememberJournalDisplayTitle(journal: Journal): String {
    val defaultTitle = stringResource(R.string.untitled)
    return remember(journal.title, journal.content, journal.emoji, defaultTitle) {
        val rawTitle = journal.title.ifBlank {
            journal.content.lineSequence().firstOrNull { it.isNotBlank() }?.take(100)
                ?: defaultTitle
        }
        val emoji = journal.emoji
        if (!emoji.isNullOrBlank()) "$emoji $rawTitle" else rawTitle
    }
}

fun rememberJournalGroupShape(index: Int, totalCount: Int): RoundedCornerShape {
    return when {
        totalCount <= 1 -> RoundedCornerShape(24.dp)
        index == 0 -> RoundedCornerShape(
            topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp
        )

        index == totalCount - 1 -> RoundedCornerShape(
            topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp
        )

        else -> RoundedCornerShape(8.dp)
    }
}

@Composable
fun JournalMetadataBadges(
    journal: Journal, modifier: Modifier = Modifier
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
            show = journal.isDraft, icon = R.drawable.edit_24px_fill
        )
        JuneBadge(
            show = isSyncEnabled,
            icon = if (journal.cloudId != null) R.drawable.cloud_24px else R.drawable.devices_24px,
            label = if (journal.cloudId != null) stringResource(R.string.cloud) else stringResource(
                R.string.local
            )
        )
        JuneBadge(
            show = mediaCount > 0,
            icon = R.drawable.photo_24px,
            label = if (mediaCount > 1) "$mediaCount" else null
        )
        JuneBadge(
            show = hasMusic, icon = R.drawable.music_note_24px
        )
        JuneBadge(
            show = hasLocation, icon = R.drawable.location_on_24px
        )
        JuneBadge(
            show = tagCount > 0, icon = R.drawable.sell_24px, label = "$tagCount"
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
        }, modifier = modifier, colors = IconButtonDefaults.iconButtonColors(
            containerColor = innerContainerColor,
            contentColor = if (actionIcon == null && isBookmarked) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ), shape = IconButtonDefaults.smallRoundShape
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

data class DayJournalGroupData(
    val date: LocalDate, val journals: List<Journal>
)