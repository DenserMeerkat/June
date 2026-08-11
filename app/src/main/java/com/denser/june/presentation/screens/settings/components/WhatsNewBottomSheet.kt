package com.denser.june.presentation.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.presentation.components.AnnouncementCard
import com.denser.june.presentation.utils.AnnouncementEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewBottomSheet(
    modifier: Modifier = Modifier,
    versionEntry: VersionEntry?,
    announcement: AnnouncementEntry? = null,
    onDismissRequest: () -> Unit,
    onAnnouncementAction: ((AnnouncementEntry) -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.whats_new),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (versionEntry != null) {
                Text(
                    text = versionEntry.version,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                if (announcement != null) {
                    item {
                        AnnouncementCard(
                            announcement = announcement,
                            onActionClick = { onAnnouncementAction?.invoke(announcement) },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                if (versionEntry != null) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            versionEntry.changes.forEachIndexed { index, change ->
                                val shape = when {
                                    versionEntry.changes.size == 1 -> RoundedCornerShape(16.dp)
                                    index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                    index == versionEntry.changes.size - 1 -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                    else -> RectangleShape
                                }

                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    headlineContent = {
                                        Text(
                                            text = change,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                )

                                if (index < versionEntry.changes.size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.done))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
