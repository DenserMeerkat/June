package com.denser.june.presentation.components.journal

import android.content.Intent
import android.icu.text.SimpleDateFormat
import androidx.compose.animation.AnimatedContent
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
import com.denser.june.presentation.pro.LocalIsPro
import com.denser.june.presentation.pro.LocalProSheetController
import com.denser.june.presentation.theme.LocalSyncEnabled
import com.denser.june.presentation.utils.TagUtils
import java.util.Date
import java.util.Locale

enum class JournalOptionsSheetMode {
    OPTIONS,
    CONFIRM_LOCK,
    CONFIRM_DELETE,
    CONFIRM_PERMANENT_DELETE
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JournalOptionsSheet(
    journal: Journal,
    is24Hour: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    onToggleBookmark: () -> Unit,
    onDeleteOrRestore: () -> Unit,
    onToggleLock: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onPermanentDelete: (() -> Unit)? = null,
    onExportMarkdown: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isPro = LocalIsPro.current
    val proSheetController = LocalProSheetController.current
    val isSyncEnabled = LocalSyncEnabled.current
    var currentMode by remember { mutableStateOf(JournalOptionsSheetMode.OPTIONS) }

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

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedContent(
            targetState = currentMode,
            label = "journal_options_sheet_mode"
        ) { mode ->
            when (mode) {
                JournalOptionsSheetMode.OPTIONS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        JournalOptionsHeader(journal = journal)

                        JournalOptionsActionRow(
                            journal = journal,
                            onToggleBookmark = onToggleBookmark,
                            onToggleLock = onToggleLock?.let {
                                {
                                    if (!journal.isLocked && !isPro) {
                                        proSheetController.show {
                                            currentMode = JournalOptionsSheetMode.CONFIRM_LOCK
                                        }
                                    } else {
                                        currentMode = JournalOptionsSheetMode.CONFIRM_LOCK
                                    }
                                }
                            },
                            onShare = handleShare,
                            onExportMarkdown = onExportMarkdown,
                            onDelete = {
                                if (journal.isDeleted) {
                                    onDeleteOrRestore()
                                    onDismiss?.invoke()
                                } else {
                                    currentMode = JournalOptionsSheetMode.CONFIRM_DELETE
                                }
                            },
                            onPermanentDelete = if (journal.isDeleted && onPermanentDelete != null) {
                                { currentMode = JournalOptionsSheetMode.CONFIRM_PERMANENT_DELETE }
                            } else null
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        JournalOptionsMetadataDetails(
                            journal = journal,
                            is24Hour = is24Hour
                        )
                    }
                }

                JournalOptionsSheetMode.CONFIRM_LOCK -> {
                    JournalConfirmationView(
                        iconRes = if (journal.isLocked) R.drawable.lock_open_right_24px else R.drawable.lock_24px,
                        title = stringResource(if (journal.isLocked) R.string.move_out_of_vault else R.string.move_to_vault),
                        message = stringResource(if (journal.isLocked) R.string.move_out_of_vault_desc else R.string.move_to_vault_desc),
                        confirmText = stringResource(if (journal.isLocked) R.string.move_out_of_vault else R.string.move_to_vault),
                        confirmColor = MaterialTheme.colorScheme.primary,
                        onConfirm = {
                            onToggleLock?.invoke()
                            onDismiss?.invoke()
                        },
                        onCancel = { currentMode = JournalOptionsSheetMode.OPTIONS }
                    )
                }

                JournalOptionsSheetMode.CONFIRM_DELETE -> {
                    JournalConfirmationView(
                        iconRes = R.drawable.delete_24px,
                        title = stringResource(R.string.move_to_bin),
                        message = stringResource(R.string.move_to_bin_default_desc),
                        confirmText = stringResource(R.string.move_to_bin),
                        confirmColor = MaterialTheme.colorScheme.error,
                        onConfirm = {
                            onDeleteOrRestore()
                            onDismiss?.invoke()
                        },
                        onCancel = { currentMode = JournalOptionsSheetMode.OPTIONS }
                    )
                }

                JournalOptionsSheetMode.CONFIRM_PERMANENT_DELETE -> {
                    JournalConfirmationView(
                        iconRes = R.drawable.delete_24px,
                        title = stringResource(R.string.permanently_delete),
                        message = if (isSyncEnabled) {
                            stringResource(R.string.delete_journal_sync_desc)
                        } else {
                            stringResource(R.string.delete_journal_local_desc)
                        },
                        confirmText = stringResource(R.string.permanently_delete),
                        confirmColor = MaterialTheme.colorScheme.error,
                        onConfirm = {
                            onPermanentDelete?.invoke()
                            onDismiss?.invoke()
                        },
                        onCancel = { currentMode = JournalOptionsSheetMode.OPTIONS }
                    )
                }
            }
        }
    }
}

@Composable
fun JournalOptionsHeader(
    journal: Journal,
    modifier: Modifier = Modifier
) {
    val year = remember(journal.dateTime) {
        SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(journal.dateTime))
    }

    Row(
        modifier = modifier
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
}

@Composable
fun JournalOptionsActionRow(
    journal: Journal,
    onToggleBookmark: () -> Unit,
    onToggleLock: (() -> Unit)?,
    onShare: () -> Unit,
    onExportMarkdown: (() -> Unit)?,
    onDelete: () -> Unit,
    onPermanentDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = onToggleBookmark,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (journal.isBookmarked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (journal.isBookmarked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                painter = painterResource(if (journal.isBookmarked) R.drawable.bookmark_added_24px_fill else R.drawable.bookmark_24px),
                contentDescription = stringResource(if (journal.isBookmarked) R.string.remove_bookmark else R.string.bookmark),
                modifier = Modifier.size(24.dp)
            )
        }

        if (!journal.isDeleted && onToggleLock != null) {
            JournalLockActionButton(
                isLocked = journal.isLocked,
                onClick = onToggleLock,
                modifier = Modifier.weight(1f)
            )
        }

        FilledTonalIconButton(
            onClick = onShare,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.share_24px),
                contentDescription = stringResource(R.string.share),
                modifier = Modifier.size(24.dp)
            )
        }

        if (onExportMarkdown != null) {
            FilledTonalIconButton(
                onClick = onExportMarkdown,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.file_save_24px),
                    contentDescription = stringResource(R.string.export_as_markdown),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        JournalDeleteActionButton(
            isDeleted = journal.isDeleted,
            onClick = onDelete,
            modifier = Modifier.weight(1f)
        )

        if (onPermanentDelete != null) {
            FilledTonalIconButton(
                onClick = onPermanentDelete,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = stringResource(R.string.permanently_delete),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun JournalOptionsMetadataDetails(
    journal: Journal,
    is24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    val spacesTags = remember(journal.tags) { TagUtils.filterTagsByCategory(journal.tags, TagCategory.Spaces) }
    val peopleTags = remember(journal.tags) { TagUtils.filterTagsByCategory(journal.tags, TagCategory.People) }
    val topicsTags = remember(journal.tags) { TagUtils.filterTagsByCategory(journal.tags, TagCategory.Topics) }

    Column(modifier = modifier) {
        if (spacesTags.isNotEmpty() || peopleTags.isNotEmpty() || topicsTags.isNotEmpty()) {
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
