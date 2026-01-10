package com.denser.june.core.presentation.screens.journal.components

import android.util.Patterns
import android.widget.Toast
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.domain.data_classes.SongDetails
import kotlinx.coroutines.launch

import com.denser.june.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongSheet(
    songDetails: SongDetails? = null,
    isFetching: Boolean = false,
    onFetchDetails: (String) -> Unit,
    onRemoveSong: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var songLink by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val context = LocalContext.current

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = null
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            topBar = { AddSongSheetHeader() },
            floatingActionButton = {
                AddSongBottomBar(
                    onPaste = {
                        scope.launch {
                            val clipEntry = clipboard.getClipEntry()
                            val text = clipEntry?.clipData?.getItemAt(0)?.text?.toString() ?: ""
                            songLink = text
                            onFetchDetails(text)
                        }
                    },
                    onDone = onDismiss
                )
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
                Spacer(modifier = Modifier.height(24.dp))
                SongInputCard(
                    songLink = songLink,
                    onLinkChange = { songLink = it },
                    isFetching = isFetching,
                    onFetchClick = { onFetchDetails(songLink) }
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
    onFetchClick: () -> Unit
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
                enabled = !isFetching,
                trailingIcon = {
                    if (isFetching) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (songLink.isNotBlank()) {
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
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongPreviewCard(
    songDetails: SongDetails?,
    isFetching: Boolean,
    onRemoveSong: () -> Unit
) {
    Box {
        JournalSongItem(
            details = songDetails,
            isFetching = isFetching,
            onRemove = onRemoveSong,
            onEdit = { },
            isEditMode = false
        )
        if (!isFetching && songDetails != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp, bottom = 36.dp)
            ) {
                FilledIconButton(
                    onClick = onRemoveSong,
                    shape = IconButtonDefaults.largePressedShape,
                    modifier = Modifier
                        .size(56.dp)
                        .alpha(0.8f),
                    colors = IconButtonDefaults.iconButtonColors().copy(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete_24px),
                        contentDescription = "Remove Song",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
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

@Composable
private fun AddSongBottomBar(
    onPaste: () -> Unit,
    onDone: () -> Unit
) {
    Surface(
        modifier = Modifier.padding(bottom = 16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(percent = 50)
                )
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExtendedFloatingActionButton(
                    onClick = onPaste,
                    icon = {
                        Icon(
                            painterResource(R.drawable.content_paste_go_24px),
                            contentDescription = null
                        )
                    },
                    text = { Text("Paste") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.height(40.dp),
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                )

                ExtendedFloatingActionButton(
                    onClick = onDone,
                    icon = {
                        Icon(
                            painterResource(R.drawable.check_24px),
                            contentDescription = null
                        )
                    },
                    text = { Text("Done") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.height(40.dp),
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                )
            }
        }
    }
}