package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.model.SongDetails
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.presentation.components.InternetRestrictedBanner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

import com.denser.june.core.R
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar
import com.denser.june.presentation.utils.UiUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongSheet(
    songDetails: SongDetails? = null,
    isFetching: Boolean = false,
    onFetchDetails: (String) -> Unit,
    onRemoveSong: () -> Unit,
    onDismiss: () -> Unit
) {
    val privacyPreferences = koinInject<PrivacyPreferences>()
    val isInternetAllowed by privacyPreferences.getIsInternetAllowedFlow()
        .collectAsStateWithLifecycle(initialValue = true)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var songLink by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = null
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if(isInternetAllowed) 600.dp else 660.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            topBar = { AddSongSheetHeader() },
            floatingActionButton = {
                JuneFloatingActionBar(
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    if (songDetails != null) {
                        FilledIconButton(
                            onClick = {
                                onRemoveSong()
                                songLink = ""
                            },
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                painterResource(R.drawable.delete_24px),
                                contentDescription = "Delete"
                            )
                        }
                    }
                    JuneFloatingAction(
                        onClick = {
                            if (isInternetAllowed) {
                                scope.launch {
                                    val clipEntry = clipboard.getClipEntry()
                                    val text = clipEntry?.clipData?.getItemAt(0)?.text?.toString() ?: ""
                                    songLink = text
                                    onFetchDetails(text)
                                }
                            }
                        },
                        label = "Paste",
                        icon = {
                            Icon(
                                painterResource(R.drawable.content_paste_go_24px),
                                contentDescription = null
                            )
                        },
                        containerColor = if (isInternetAllowed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isInternetAllowed) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        enabled = isInternetAllowed
                    )
                    JuneFloatingAction(
                        onClick = onDismiss,
                        label = "Done",
                        icon = {
                            Icon(
                                painterResource(R.drawable.check_24px),
                                contentDescription = null
                            )
                        }
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isInternetAllowed) {
                    Spacer(modifier = Modifier.height(16.dp))
                    InternetRestrictedBanner(
                        description = "Enable internet access to fetch song details."
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                SongInputCard(
                    songLink = songLink,
                    onLinkChange = { songLink = it },
                    isFetching = isFetching,
                    onFetchClick = { onFetchDetails(songLink) },
                    enabled = isInternetAllowed
                )
                Spacer(modifier = Modifier.height(16.dp))
                SongPreviewCard(
                    songDetails = songDetails,
                    isFetching = isFetching,
                    onRemoveSong = onRemoveSong
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongInputCard(
    songLink: String,
    onLinkChange: (String) -> Unit,
    isFetching: Boolean,
    onFetchClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Song URL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.spotify), null, Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Icon(painterResource(R.drawable.applemusic), null, Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Icon(painterResource(R.drawable.youtubemusic), null, Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Icon(painterResource(R.drawable.soundcloud), null, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Icon(painterResource(R.drawable.amazonmusic), null, Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Icon(painterResource(R.drawable.deezer), null, Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Icon(painterResource(R.drawable.tidal), null, Modifier.size(12.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            TextField(
                value = songLink,
                onValueChange = onLinkChange,
                placeholder = { Text("Paste link here...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFetching && enabled,
                trailingIcon = {
                    if (isFetching) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (songLink.isNotBlank() && enabled) {
                        FilledTonalIconButton(
                            onClick = onFetchClick,
                            shape = IconButtonDefaults.extraSmallRoundShape,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward_24px),
                                contentDescription = "Fetch Song",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                colors = UiUtils.getTransparentTextFieldColors(),
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true
            )
        }
    }
}

@Composable
fun SongPreviewCard(
    songDetails: SongDetails?,
    isFetching: Boolean,
    onRemoveSong: () -> Unit
) {
    JournalSongItem(
        details = songDetails,
        isFetching = isFetching,
        onRemove = onRemoveSong,
        onEdit = {}
    )
}

@Composable
private fun AddSongSheetHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Attach Song",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}