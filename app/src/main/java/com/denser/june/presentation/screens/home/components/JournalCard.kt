package com.denser.june.presentation.screens.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.utils.toDayOfMonth
import com.denser.june.core.utils.toFullDate
import com.denser.june.core.utils.toShortMonth
import com.denser.june.presentation.components.JournalThumbnail
import com.denser.june.presentation.components.JuneBadge
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.editor.components.JournalMosaicCard
import com.denser.june.presentation.screens.editor.components.MediaOperations
import com.denser.june.presentation.theme.LocalSyncEnabled
import org.koin.compose.koinInject

import com.denser.june.core.R

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JournalCard(
    journal: Journal,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    is24Hour: Boolean = false,
    showDate: Boolean = false,
    actionIcon: Int? = null,
    onActionClick: (() -> Unit)? = null,
    onToggleBookmark: (() -> Unit)? = null,
    onJournalClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val navigator = if (onJournalClick == null) koinInject<AppNavigator>() else null
    val mediaOperations = MediaOperations(onMediaClick = null)

    val containerColor = when {
        journal.isDraft -> MaterialTheme.colorScheme.surfaceContainerLow
        journal.isBookmarked -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    val innerContainerColor = if (journal.isBookmarked ) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClick = {
                    if (onJournalClick != null) {
                        onJournalClick()
                    } else {
                        navigator?.navigateTo(Route.Editor(journal.id), isSingleTop = true)
                    }
                },
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val emoji = journal.emoji

            JournalThumbnail(
                journal = journal,
                containerColor = innerContainerColor,
                mediaOperations = mediaOperations
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dateColor = when {
                        journal.isDeleted -> MaterialTheme.colorScheme.error
                        journal.isDraft -> MaterialTheme.colorScheme.tertiary
                        journal.isBookmarked -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    }

                    Text(
                        text = journal.dateTime.toFullDate(),
                        style = MaterialTheme.typography.labelMedium,
                        color = dateColor,
                        fontWeight = FontWeight.SemiBold
                    )

                    JuneBadge(
                        show = journal.isDraft,
                        icon = R.drawable.edit_24px_fill
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                val rawTitle = journal.title.ifBlank { journal.content.ifBlank { stringResource(R.string.add_title) } }
                val displayTitle = if (!emoji.isNullOrBlank()) "$emoji $rawTitle" else rawTitle

                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (actionIcon != null || onToggleBookmark != null) {
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (actionIcon != null) {
                            onActionClick?.invoke()
                        } else {
                            onToggleBookmark?.invoke()
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = innerContainerColor,
                        contentColor = if (actionIcon == null && journal.isBookmarked) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    shape = IconButtonDefaults.smallRoundShape
                ) {
                    Icon(
                        painter = painterResource(
                            actionIcon ?: if (journal.isBookmarked) R.drawable.bookmark_added_24px_fill
                            else R.drawable.bookmark_24px
                        ),
                        contentDescription = if (actionIcon != null) "Action" else stringResource(R.string.bookmark),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecentJournalCard(
    journal: Journal,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    is24Hour: Boolean = false,
    actionIcon: Int? = null,
    onActionClick: (() -> Unit)? = null,
    onToggleBookmark: (() -> Unit)? = null,
    onJournalClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    if (journal.images.isEmpty()) {
        JournalCard(
            journal = journal,
            modifier = modifier,
            shape = shape,
            is24Hour = is24Hour,
            actionIcon = actionIcon,
            onActionClick = onActionClick,
            onToggleBookmark = onToggleBookmark,
            onJournalClick = onJournalClick,
            onLongClick = onLongClick
        )
    } else {
        val navigator = if (onJournalClick == null) koinInject<AppNavigator>() else null

        val displayImages = remember(journal.images) {
            journal.images.reversed().take(3)
        }

        val mediaOperations = MediaOperations(onMediaClick = null)

        val mediaCount = remember(journal.images) { journal.images.size }
        val hasMusic = remember(journal.songDetails) { journal.songDetails != null }
        val hasLocation = remember(journal.location) { journal.location != null }
        val tagCount = remember(journal.tags) { journal.tags.size }

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

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .combinedClickable(
                    onClick = {
                        if (onJournalClick != null) {
                            onJournalClick()
                        } else {
                            navigator?.navigateTo(
                                Route.Editor(journal.id),
                                isSingleTop = true
                            )
                        }
                    },
                    onLongClick = onLongClick
                ),
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            shape = shape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = journal.dateTime.toDayOfMonth(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = journal.dateTime.toShortMonth(),
                            style = MaterialTheme.typography.labelMedium,
                            color = dateColor,
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val emoji = journal.emoji
                        val rawTitle = journal.title.ifBlank { journal.content.ifBlank { stringResource(R.string.untitled) } }
                        val displayTitle = if (!emoji.isNullOrBlank()) "$emoji $rawTitle" else rawTitle

                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            JuneBadge(
                                show = journal.isDraft,
                                icon = R.drawable.edit_24px_fill
                            )
                            JuneBadge(
                                show = LocalSyncEnabled.current,
                                icon = if (journal.cloudId != null) R.drawable.cloud_24px else R.drawable.devices_24px,
                                label = if (journal.cloudId != null) stringResource(R.string.cloud) else stringResource(R.string.local)
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
                    FilledIconButton(
                        onClick = {
                            if (actionIcon != null) {
                                onActionClick?.invoke()
                            } else {
                                onToggleBookmark?.invoke()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = innerContainerColor,
                            contentColor = if (actionIcon == null && journal.isBookmarked) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ),
                        shape = IconButtonDefaults.smallRoundShape
                    ) {
                        Icon(
                            painter = painterResource(
                                actionIcon ?: if (journal.isBookmarked) R.drawable.bookmark_added_24px_fill 
                                else R.drawable.bookmark_24px
                            ),
                            contentDescription = if (actionIcon != null) "Action" else stringResource(R.string.bookmark),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                JournalMosaicCard(
                    mediaList = displayImages,
                    enablePlayback = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.7f),
                    operations = mediaOperations,
                    roundedCornerShape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}