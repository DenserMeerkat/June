package com.denser.june.presentation.screens.settings.screens.sync.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.sync.SyncAnalysis
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneCloseButton
import com.denser.june.presentation.components.JuneConnectedRowItem
import com.denser.june.presentation.components.JuneFullScreenDialog
import com.denser.june.presentation.components.JuneTopAppBar
import kotlinx.coroutines.launch

private enum class ChangeType {
    UPLOAD,
    DOWNLOAD,
    DELETION
}

private data class SyncChangeItem(
    val title: String,
    val type: ChangeType
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncOverviewSheet(
    analysis: SyncAnalysis,
    initialCategory: SyncCategory? = null,
    onDismiss: () -> Unit
) {
    var activeCategory by remember {
        mutableStateOf(
            initialCategory ?: determineDefaultCategory(analysis)
        )
    }

    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current
    val rowHeight = 38.dp
    val maxCollapseDistancePx = with(density) { (rowHeight * 3).toPx() }
    var collapseOffsetPx by remember { mutableFloatStateOf(0f) }
    val collapseAnimatable = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(activeCategory) {
        lazyListState.scrollToItem(0)
        collapseOffsetPx = 0f
        collapseAnimatable.snapTo(0f)
    }

    val collapseFraction = if (maxCollapseDistancePx > 0f) {
        (collapseOffsetPx / maxCollapseDistancePx).coerceIn(0f, 1f)
    } else 0f

    val nestedScrollConnection = remember(maxCollapseDistancePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // When scrolling down (available.y < 0): collapse table before list scrolls
                if (available.y < 0 && collapseOffsetPx < maxCollapseDistancePx) {
                    val newOffset = (collapseOffsetPx - available.y).coerceIn(0f, maxCollapseDistancePx)
                    val consumed = collapseOffsetPx - newOffset
                    collapseOffsetPx = newOffset
                    coroutineScope.launch { collapseAnimatable.snapTo(newOffset) }
                    return Offset(0f, consumed)
                }
                // When scrolling up (available.y > 0): restore table as user scrolls up without waiting for the top
                if (available.y > 0 && collapseOffsetPx > 0f) {
                    val newOffset = (collapseOffsetPx - available.y).coerceIn(0f, maxCollapseDistancePx)
                    val consumed = collapseOffsetPx - newOffset
                    collapseOffsetPx = newOffset
                    coroutineScope.launch { collapseAnimatable.snapTo(newOffset) }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Also consume any remaining upward scroll delta to expand table
                if (available.y > 0 && collapseOffsetPx > 0f) {
                    val newOffset = (collapseOffsetPx - available.y).coerceIn(0f, maxCollapseDistancePx)
                    val consumed = collapseOffsetPx - newOffset
                    collapseOffsetPx = newOffset
                    coroutineScope.launch { collapseAnimatable.snapTo(newOffset) }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val targetOffset = if (collapseOffsetPx > maxCollapseDistancePx / 2f) maxCollapseDistancePx else 0f
                collapseAnimatable.animateTo(
                    targetValue = targetOffset,
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                ) {
                    collapseOffsetPx = value
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    JuneFullScreenDialog(
        onDismissRequest = onDismiss
    ) {
        BackHandler(enabled = true) {
            onDismiss()
        }

        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    JuneTopAppBar(
                        type = JuneAppBarType.Small,
                        title = {
                            Text(
                                text = stringResource(R.string.sync_overview),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            JuneCloseButton(onClick = onDismiss)
                        }
                    )
                }
            ) { innerPadding ->
                CompositionLocalProvider(
                    LocalOverscrollFactory provides null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                SyncTableHeader(
                                    modifier = Modifier.fillMaxWidth()
                                )

                                SyncCategory.entries.forEach { category ->
                                    val isSelected = category == activeCategory
                                    val stats = analysis.getStatsFor(category)

                                    if (isSelected) {
                                        SyncTableRow(
                                            category = category,
                                            stats = stats,
                                            isSelected = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = null
                                        )
                                    } else {
                                        val animatedHeight = (rowHeight * (1f - collapseFraction)).coerceAtLeast(0.dp)
                                        val animatedAlpha = (1f - collapseFraction).coerceIn(0f, 1f)

                                        if (animatedHeight > 0.5.dp) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(animatedHeight)
                                                    .clipToBounds()
                                                    .graphicsLayer { alpha = animatedAlpha }
                                            ) {
                                                SyncTableRow(
                                                    category = category,
                                                    stats = stats,
                                                    isSelected = false,
                                                    onClick = if (collapseFraction < 0.2f) {
                                                        { activeCategory = category }
                                                    } else null,
                                                    modifier = Modifier.requiredHeight(rowHeight)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val changeGroups = remember(analysis, activeCategory) {
                            getChangeGroupsForCategory(analysis, activeCategory)
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (changeGroups.isEmpty()) {
                                EmptyCategoryState(
                                    message = when (activeCategory) {
                                        SyncCategory.JOURNALS -> "All journals are in sync"
                                        SyncCategory.MEDIA -> "All photos and media are in sync"
                                        SyncCategory.AUDIOS -> "All voice notes are in sync"
                                        SyncCategory.SONGS -> "All songs and art are in sync"
                                    }
                                )
                            } else {
                                LazyColumn(
                                    state = lazyListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    changeGroups.forEach { groupItems ->
                                        item {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                groupItems.forEachIndexed { index, item ->
                                                    JuneConnectedRowItem(
                                                        index = index,
                                                        count = groupItems.size,
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                                        leadingSpacing = 12.dp,
                                                        leadingContent = {
                                                            Icon(
                                                                painter = painterResource(
                                                                    when (item.type) {
                                                                        ChangeType.UPLOAD -> R.drawable.cloud_upload_24px
                                                                        ChangeType.DOWNLOAD -> R.drawable.cloud_download_24px
                                                                        ChangeType.DELETION -> R.drawable.delete_24px
                                                                    }
                                                                ),
                                                                contentDescription = null,
                                                                modifier = Modifier.size(20.dp),
                                                                tint = when (item.type) {
                                                                    ChangeType.UPLOAD -> MaterialTheme.colorScheme.primary
                                                                    ChangeType.DOWNLOAD -> MaterialTheme.colorScheme.tertiary
                                                                    ChangeType.DELETION -> MaterialTheme.colorScheme.error
                                                                }
                                                            )
                                                        }
                                                    ) {
                                                        val isPlaceholder = isPlaceholderTitle(item.title)

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text(
                                                                text = if (isPlaceholder) formatIdentifier(item.title) else item.title,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Medium,
                                                                fontStyle = if (isPlaceholder) FontStyle.Italic else FontStyle.Normal,
                                                                color = if (isPlaceholder) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f, fill = false)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(modifier = Modifier.navigationBarsPadding())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getChangeGroupsForCategory(
    analysis: SyncAnalysis,
    category: SyncCategory
): List<List<SyncChangeItem>> {
    val uploads = mutableListOf<SyncChangeItem>()
    val downloads = mutableListOf<SyncChangeItem>()
    val deletions = mutableListOf<SyncChangeItem>()

    when (category) {
        SyncCategory.JOURNALS -> {
            analysis.pendingUploadsList.forEach { uploads.add(SyncChangeItem(it, ChangeType.UPLOAD)) }
            analysis.pendingDownloadsList.forEach { downloads.add(SyncChangeItem(it, ChangeType.DOWNLOAD)) }
            analysis.localDeletionsList.forEach { deletions.add(SyncChangeItem(it, ChangeType.DELETION)) }
            analysis.remoteDeletionsList.forEach { deletions.add(SyncChangeItem(it, ChangeType.DELETION)) }
        }
        SyncCategory.MEDIA -> {
            analysis.pendingMediaUploadsList.forEach { uploads.add(SyncChangeItem(it, ChangeType.UPLOAD)) }
            analysis.pendingMediaDownloadsList.forEach { downloads.add(SyncChangeItem(it, ChangeType.DOWNLOAD)) }
        }
        SyncCategory.AUDIOS -> {
            analysis.pendingAudioUploadsList.forEach { uploads.add(SyncChangeItem(it, ChangeType.UPLOAD)) }
            analysis.pendingAudioDownloadsList.forEach { downloads.add(SyncChangeItem(it, ChangeType.DOWNLOAD)) }
        }
        SyncCategory.SONGS -> {
            analysis.pendingSongUploadsList.forEach { uploads.add(SyncChangeItem(it, ChangeType.UPLOAD)) }
            analysis.pendingSongDownloadsList.forEach { downloads.add(SyncChangeItem(it, ChangeType.DOWNLOAD)) }
        }
    }

    return listOf(uploads, downloads, deletions).filter { it.isNotEmpty() }
}

private fun determineDefaultCategory(analysis: SyncAnalysis): SyncCategory {
    val journalChanges = analysis.pendingUploadsCount + analysis.pendingDownloadsCount + analysis.pendingDeletionsCount
    val mediaChanges = analysis.pendingMediaUploadsCount + analysis.pendingMediaDownloadsCount
    val audioChanges = analysis.pendingAudioUploadsCount + analysis.pendingAudioDownloadsCount
    val songChanges = analysis.pendingSongUploadsCount + analysis.pendingSongDownloadsCount

    return when {
        journalChanges > 0 -> SyncCategory.JOURNALS
        mediaChanges > 0 -> SyncCategory.MEDIA
        audioChanges > 0 -> SyncCategory.AUDIOS
        songChanges > 0 -> SyncCategory.SONGS
        else -> SyncCategory.JOURNALS
    }
}

@Composable
private fun EmptyCategoryState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.cloud_done_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatIdentifier(name: String): String {
    if (name.length <= 24) return name

    val extension = if (name.contains(".")) name.substringAfterLast(".", "") else ""
    val baseName = if (name.contains(".")) name.substringBeforeLast(".") else name

    if (baseName.length <= 20) return name

    val prefix = baseName.take(12)
    val suffix = baseName.takeLast(8)

    return if (extension.isNotEmpty()) "$prefix...$suffix.$extension" else "$prefix...$suffix"
}

private fun isPlaceholderTitle(title: String): Boolean {
    val trimmed = title.trim()
    if (trimmed == "Untitled" ||
        trimmed.startsWith("Untitled (") ||
        trimmed.startsWith("Untitled •") ||
        trimmed == "[Bin] Untitled" ||
        trimmed.startsWith("[Bin] Untitled (")
    ) {
        return true
    }
    val isFilename = trimmed.endsWith(".json", ignoreCase = true) ||
        trimmed.endsWith(".jpg", ignoreCase = true) ||
        trimmed.endsWith(".jpeg", ignoreCase = true) ||
        trimmed.endsWith(".png", ignoreCase = true) ||
        trimmed.endsWith(".mp3", ignoreCase = true) ||
        trimmed.endsWith(".m4a", ignoreCase = true) ||
        trimmed.endsWith(".wav", ignoreCase = true) ||
        trimmed.endsWith(".ogg", ignoreCase = true) ||
        trimmed.endsWith(".flac", ignoreCase = true)
    if (isFilename) {
        return true
    }
    if (trimmed.matches(Regex("^[0-9a-fA-F-]{8,}$"))) {
        return true
    }
    return false
}
