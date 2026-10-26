package com.denser.june.presentation.screens.home.components

import android.content.Intent
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.enums.TagCategory
import com.denser.june.core.utils.toDayOfMonth
import com.denser.june.core.utils.toFullDateTime
import com.denser.june.core.utils.toShortMonth
import com.denser.june.presentation.components.journal.JuneBadge
import com.denser.june.presentation.components.journal.JuneMetadataRow
import com.denser.june.presentation.theme.LocalSyncEnabled
import com.denser.june.presentation.utils.TagUtils
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JournalOptionsSheet(
    journal: Journal,
    is24Hour: Boolean = false,
    onToggleBookmark: () -> Unit,
    onDeleteOrRestore: () -> Unit,
    onToggleLock: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onPermanentDelete: (() -> Unit)? = null,
    onExportMarkdown: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val year = remember(journal.dateTime) {
        SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(journal.dateTime))
    }

    val spacesTags = remember(journal.tags) { TagUtils.filterTagsByCategory(journal.tags, TagCategory.Spaces) }
    val peopleTags = remember(journal.tags) { TagUtils.filterTagsByCategory(journal.tags, TagCategory.People) }
    val topicsTags = remember(journal.tags) { TagUtils.filterTagsByCategory(journal.tags, TagCategory.Topics) }

    val handleShare = onShare ?: {
        val textToShare = buildString {
            if (journal.title.isNotBlank()) {
                append("# ").append(journal.title).append("\n\n")
            }
            append(journal.content)
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, textToShare)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    val handleLockClick: () -> Unit = {
        onToggleLock?.invoke()
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text(
                        text = journal.dateTime.toDayOfMonth(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = journal.dateTime.toShortMonth(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = year,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = journal.title.ifBlank { stringResource(R.string.untitled) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        JuneBadge(
                            show = LocalSyncEnabled.current,
                            icon = if (journal.isSynced) R.drawable.cloud_24px else R.drawable.devices_24px,
                            label = if (journal.isSynced) stringResource(R.string.cloud) else stringResource(R.string.local)
                        )
                        JuneBadge(show = journal.images.isNotEmpty(), icon = R.drawable.photo_24px, label = "${journal.images.size}")
                        JuneBadge(show = journal.songDetails != null, icon = R.drawable.music_note_24px)
                        JuneBadge(show = journal.location != null, icon = R.drawable.location_on_24px)
                    }
                }
            }

            data class SheetAction(
                val iconRes: Int,
                val contentDescriptionRes: Int,
                val onClick: () -> Unit,
                val isActive: Boolean = false,
                val isDestructive: Boolean = false
            )

            val actions = remember(
                journal.isDeleted,
                journal.isBookmarked,
                journal.isLocked,
                onExportMarkdown,
                onToggleBookmark,
                onDeleteOrRestore,
                onPermanentDelete,
                onToggleLock
            ) {
                buildList {
                    add(
                        SheetAction(
                            iconRes = if (journal.isBookmarked) R.drawable.bookmark_added_24px_fill else R.drawable.bookmark_24px,
                            contentDescriptionRes = if (journal.isBookmarked) R.string.remove_bookmark else R.string.bookmark,
                            onClick = onToggleBookmark,
                            isActive = journal.isBookmarked
                        )
                    )

                    if (!journal.isDeleted && onToggleLock != null) {
                        add(
                            SheetAction(
                                iconRes = if (journal.isLocked) R.drawable.lock_24px else R.drawable.lock_open_right_24px,
                                contentDescriptionRes = if (journal.isLocked) R.string.unlock else R.string.lock,
                                onClick = handleLockClick,
                                isActive = journal.isLocked
                            )
                        )
                    }

                    add(
                        SheetAction(
                            iconRes = R.drawable.share_24px,
                            contentDescriptionRes = R.string.share,
                            onClick = handleShare
                        )
                    )

                    if (onExportMarkdown != null) {
                        add(
                            SheetAction(
                                iconRes = R.drawable.file_save_24px,
                                contentDescriptionRes = R.string.export_as_markdown,
                                onClick = onExportMarkdown
                            )
                        )
                    }

                    add(
                        SheetAction(
                            iconRes = if (journal.isDeleted) R.drawable.restore_from_trash_24px else R.drawable.delete_24px,
                            contentDescriptionRes = if (journal.isDeleted) R.string.restore else R.string.delete,
                            onClick = onDeleteOrRestore,
                            isDestructive = !journal.isDeleted
                        )
                    )

                    if (journal.isDeleted && onPermanentDelete != null) {
                        add(
                            SheetAction(
                                iconRes = R.drawable.delete_24px,
                                contentDescriptionRes = R.string.permanently_delete,
                                onClick = onPermanentDelete,
                                isDestructive = true
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions.forEach { action ->
                    val containerColor = when {
                        action.isActive -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    val contentColor = when {
                        action.isDestructive -> MaterialTheme.colorScheme.error
                        action.isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    FilledTonalIconButton(
                        onClick = action.onClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = containerColor,
                            contentColor = contentColor
                        )
                    ) {
                        Icon(
                            painter = painterResource(action.iconRes),
                            contentDescription = stringResource(action.contentDescriptionRes),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    spacesTags.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(12.dp),
                            colors = TagUtils.getTagSuggestionChipColors(tag),
                            border = null
                        )
                    }
                    peopleTags.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(12.dp),
                            colors = TagUtils.getTagSuggestionChipColors(tag),
                            border = null
                        )
                    }
                    topicsTags.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(12.dp),
                            colors = TagUtils.getTagSuggestionChipColors(tag),
                            border = null
                        )
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    if (LocalSyncEnabled.current) {
                        JuneMetadataRow(
                            iconRes = R.drawable.cloud_sync_24px,
                            label = stringResource(R.string.synced),
                            value = journal.syncedAt?.toFullDateTime(is24Hour) ?: stringResource(R.string.not_synced)
                        )
                    }
                    JuneMetadataRow(
                        iconRes = R.drawable.today_24px,
                        label = stringResource(R.string.created),
                        value = journal.createdAt.toFullDateTime(is24Hour)
                    )
                    JuneMetadataRow(
                        iconRes = R.drawable.history_24px,
                        label = stringResource(R.string.updated),
                        value = journal.updatedAt?.toFullDateTime(is24Hour) ?: "—"
                    )
                }
        }
    }
}
