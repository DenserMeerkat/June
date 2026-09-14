package com.denser.june.presentation.screens.home.components

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.utils.toDayOfMonth
import com.denser.june.core.utils.toFullDateTime
import com.denser.june.core.utils.toShortMonth
import java.util.Locale
import com.denser.june.presentation.components.JuneBadge
import com.denser.june.presentation.components.JuneMetadataRow
import com.denser.june.presentation.theme.LocalSyncEnabled
import java.util.Date
import com.denser.june.presentation.utils.TagUtils
import com.denser.june.core.domain.model.enums.TagCategory

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JournalOptionsSheet(
    journal: Journal,
    is24Hour: Boolean = false,
    onToggleBookmark: () -> Unit,
    onDeleteOrRestore: () -> Unit,
    onPermanentDelete: (() -> Unit)? = null,
    onExportMarkdown: (() -> Unit)? = null
) {
    val wordCount = remember(journal.content) {
        if (journal.content.isBlank()) 0
        else journal.content.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    }
    val year = remember(journal.dateTime) {
        SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(journal.dateTime))
    }

    val spacesTags = remember(journal.tags) { TagUtils.filterTagsByCategory(journal.tags, TagCategory.Spaces) }
    val peopleTags = remember(journal.tags) { TagUtils.filterTagsByCategory(journal.tags, TagCategory.People) }
    val topicsTags = remember(journal.tags) { TagUtils.filterTagsByCategory(journal.tags, TagCategory.Topics) }

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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(end = 8.dp)
                        ) {
                            JuneBadge(
                                show = LocalSyncEnabled.current,
                                icon = if (journal.cloudId != null) R.drawable.cloud_24px else R.drawable.devices_24px,
                                label = if (journal.cloudId != null) stringResource(R.string.cloud) else stringResource(R.string.local)
                            )
                            JuneBadge(show = journal.images.isNotEmpty(), icon = R.drawable.photo_24px, label = "${journal.images.size}")
                            JuneBadge(show = journal.songDetails != null, icon = R.drawable.music_note_24px)
                            JuneBadge(show = journal.location != null, icon = R.drawable.location_on_24px)
                        }

                        val actions = remember(
                            journal.isDeleted,
                            journal.isBookmarked,
                            onExportMarkdown,
                            onToggleBookmark,
                            onDeleteOrRestore,
                            onPermanentDelete
                        ) {
                            buildList {
                                if (!journal.isDeleted && onExportMarkdown != null) {
                                    add(
                                        ActionConfig(
                                            iconRes = R.drawable.file_save_24px,
                                            contentDescriptionRes = R.string.export_as_markdown,
                                            onClick = onExportMarkdown
                                        )
                                    )
                                }
                                if (!journal.isDeleted) {
                                    add(
                                        ActionConfig(
                                            iconRes = if (journal.isBookmarked) R.drawable.bookmark_added_24px_fill else R.drawable.bookmark_24px,
                                            contentDescriptionRes = if (journal.isBookmarked) R.string.remove_bookmark else R.string.bookmark,
                                            onClick = onToggleBookmark,
                                            isActive = journal.isBookmarked
                                        )
                                    )
                                }
                                add(
                                    ActionConfig(
                                        iconRes = if (journal.isDeleted) R.drawable.restore_from_trash_24px else R.drawable.delete_24px,
                                        contentDescriptionRes = if (journal.isDeleted) R.string.restore else R.string.delete,
                                        onClick = onDeleteOrRestore,
                                        isDestructive = !journal.isDeleted
                                    )
                                )
                                if (journal.isDeleted && onPermanentDelete != null) {
                                    add(
                                        ActionConfig(
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
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            actions.forEachIndexed { index, action ->
                                val shape = when {
                                    actions.size == 1 -> ToggleButtonDefaults.shapes()
                                    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    index == actions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                                val contentColor = when {
                                    action.isDestructive -> MaterialTheme.colorScheme.error
                                    action.isActive -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                val buttonModifier = when {
                                    actions.size == 1 -> Modifier.height(36.dp).width(48.dp)
                                    index == 0 || index == actions.lastIndex -> Modifier.height(36.dp).width(44.dp)
                                    else -> Modifier.height(36.dp).width(38.dp)
                                }

                                ToggleButton(
                                    checked = action.isActive,
                                    onCheckedChange = { action.onClick() },
                                    shapes = shape,
                                    colors = ToggleButtonDefaults.toggleButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        contentColor = contentColor,
                                        checkedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        checkedContentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    modifier = buttonModifier
                                ) {
                                    Icon(
                                        painter = painterResource(action.iconRes),
                                        contentDescription = stringResource(action.contentDescriptionRes),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(pluralStringResource(R.plurals.words_count, wordCount, wordCount), style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = { Icon(painterResource(R.drawable.article_24px), null, modifier = Modifier.size(16.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null
                )

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

private data class ActionConfig(
    val iconRes: Int,
    val contentDescriptionRes: Int,
    val onClick: () -> Unit,
    val isActive: Boolean = false,
    val isDestructive: Boolean = false
)
