package com.denser.june.presentation.screens.editor.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.SongFetchProgress

@Composable
fun SongInputCard(
    songLink: String,
    onLinkChange: (String) -> Unit,
    isFetching: Boolean,
    fetchProgress: SongFetchProgress?,
    enabled: Boolean,
    onPaste: () -> Unit,
    onFetch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
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
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = songLink,
                    onValueChange = onLinkChange,
                    placeholder = { Text(stringResource(R.string.paste_link_placeholder)) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go,
                        keyboardType = KeyboardType.Uri
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { onFetch() },
                        onDone = { onFetch() }
                    ),
                    trailingIcon = {
                        if (isFetching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (songLink.isNotBlank() && enabled) {
                            IconButton(onClick = { onLinkChange("") }) {
                                Icon(
                                    painter = painterResource(R.drawable.close_24px),
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onPaste,
                                enabled = enabled
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.content_paste_go_24px),
                                    contentDescription = "Paste",
                                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true
                )
            }

            if (isFetching || fetchProgress != null) {
                val animatedProgress by animateFloatAsState(
                    targetValue = fetchProgress?.fraction ?: 0f,
                    label = "fetch_progress"
                )

                if (fetchProgress != null) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                        color = if (fetchProgress.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}
