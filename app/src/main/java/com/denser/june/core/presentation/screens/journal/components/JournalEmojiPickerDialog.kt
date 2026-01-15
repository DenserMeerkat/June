package com.denser.june.core.presentation.screens.journal.components

import android.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.emoji2.emojipicker.EmojiPickerView
import com.denser.june.LocalAppTheme
import com.denser.june.core.domain.enums.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEmojiPickerDialog(
    initialEmoji: String? = null,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentSelection by remember(initialEmoji) { mutableStateOf(initialEmoji) }
    val currentTheme = LocalAppTheme.current.appTheme
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = remember(currentTheme, systemDark) {
        when (currentTheme) {
            AppTheme.SYSTEM -> systemDark
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .height(600.dp)
                    .padding(horizontal = 16.dp)
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp, 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pick a Mood",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    EmojiDialogPreview(selectedEmoji = currentSelection)
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer,
                                RoundedCornerShape(16.dp)
                            )
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                val themeResId = if (isDarkTheme) {
                                    android.R.style.Theme_DeviceDefault
                                } else {
                                    android.R.style.Theme_DeviceDefault_Light
                                }
                                val themedContext = ContextThemeWrapper(context, themeResId)

                                EmojiPickerView(themedContext).apply {
                                    emojiGridColumns = 8
                                    setOnEmojiPickedListener { item ->
                                        currentSelection = item.emoji
                                    }
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                currentSelection?.let { onEmojiSelected(it) }
                                if (currentSelection == null) onDismiss()
                            }
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmojiDialogPreview(selectedEmoji: String?) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (selectedEmoji != null) {
                Text(
                    text = selectedEmoji,
                    fontSize = 40.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Select an emoji",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}