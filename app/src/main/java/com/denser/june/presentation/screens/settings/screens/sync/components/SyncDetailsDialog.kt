package com.denser.june.presentation.screens.settings.screens.sync.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.presentation.components.JuneFullScreenDialog
import com.denser.june.core.R
import com.denser.june.core.domain.sync.SyncAnalysis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDetailsDialog(
    analysis: SyncAnalysis,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Journals", "Media")

    JuneFullScreenDialog(
        onDismissRequest = onDismiss,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = "Sync Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Review changes before syncing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    divider = {},
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val count = if (index == 0) {
                                        (analysis.pendingUploadsCount + analysis.pendingDownloadsCount + analysis.pendingDeletionsCount)
                                    } else {
                                        (analysis.pendingMediaUploadsCount + analysis.pendingMediaDownloadsCount)
                                    }

                                    Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal)
                                    if (count > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge(
                                            containerColor = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (selectedTabIndex == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ) {
                                            Text(count.toString())
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTabIndex == 0) {
                        JournalList(analysis)
                    } else {
                        MediaList(analysis)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp, 20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = onDismiss) {
                        Text("Got it")
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalList(analysis: SyncAnalysis) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (analysis.pendingUploadsList.isNotEmpty()) {
            item { SectionHeader("To Upload", R.drawable.upload_24px) }
            items(analysis.pendingUploadsList) { DetailItem(it) }
        }

        if (analysis.pendingDownloadsList.isNotEmpty()) {
            item { SectionHeader("To Download", R.drawable.download_24px) }
            items(analysis.pendingDownloadsList) { DetailItem(it) }
        }

        if (analysis.remoteDeletionsList.isNotEmpty()) {
            item { SectionHeader("To Delete Locally", R.drawable.delete_24px) }
            items(analysis.remoteDeletionsList) { DetailItem(it, isDeletion = true) }
        }

        if (analysis.localDeletionsList.isNotEmpty()) {
            item { SectionHeader("To Delete on Cloud", R.drawable.delete_24px) }
            items(analysis.localDeletionsList) { DetailItem(it, isDeletion = true) }
        }

        if (analysis.pendingUploadsList.isEmpty() &&
            analysis.pendingDownloadsList.isEmpty() &&
            analysis.remoteDeletionsList.isEmpty() &&
            analysis.localDeletionsList.isEmpty()) {
            item { EmptyState("No journal changes") }
        }
    }
}

@Composable
private fun MediaList(analysis: SyncAnalysis) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (analysis.pendingMediaUploadsList.isNotEmpty()) {
            item { SectionHeader("To Upload", R.drawable.photo_24px) }
            items(analysis.pendingMediaUploadsList) { DetailItem(it) }
        }

        if (analysis.pendingMediaDownloadsList.isNotEmpty()) {
            item { SectionHeader("To Download", R.drawable.photo_24px) }
            items(analysis.pendingMediaDownloadsList) { DetailItem(it) }
        }

        if (analysis.pendingMediaUploadsList.isEmpty() && analysis.pendingMediaDownloadsList.isEmpty()) {
            item { EmptyState("No media changes") }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DetailItem(title: String, isDeletion: Boolean = false) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatIdentifier(title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (isDeletion) {
                    Icon(
                        painterResource(R.drawable.delete_24px),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}

private fun formatIdentifier(name: String): String {
    if (name.length <= 48) return name

    val extension = if (name.contains(".")) name.substringAfterLast(".", "") else ""
    val baseName = if (name.contains(".")) name.substringBeforeLast(".") else name

    if (baseName.length <= 22) return name

    val prefix = baseName.take(10)
    val suffix = baseName.takeLast(10)

    return if (extension.isNotEmpty()) "$prefix...$suffix.$extension" else "$prefix...$suffix"
}
