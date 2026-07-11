package com.denser.june.presentation.screens.settings.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.june.core.R
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleLogsBottomSheet(
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val consoleLazyListState = rememberLazyListState()

    var logsList by remember { mutableStateOf(emptyList<String>()) }
    val refreshLogs = {
        logsList = AppLogger.getBufferedLogs()
    }

    LaunchedEffect(Unit) {
        refreshLogs()
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        val consoleBgColor = MaterialTheme.colorScheme.surfaceContainerLowest
        val defaultTextColor = MaterialTheme.colorScheme.onSurface

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Console Logs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { refreshLogs() },
                    label = { Text("Refresh") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.replay_24px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                AssistChip(
                    enabled = logsList.isNotEmpty(),
                    onClick = {
                        val logs = AppLogger.getBufferedLogs().joinToString("\n")
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("June App Logs", logs)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    label = { Text("Copy") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.content_copy_24px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                AssistChip(
                    enabled = logsList.isNotEmpty(),
                    onClick = {
                        AppLogger.clearBufferedLogs()
                        refreshLogs()
                        Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                    },
                    label = {
                        Text(
                            text = "Clear",
                            color = if (logsList.isNotEmpty()) MaterialTheme.colorScheme.error else LocalContentColor.current
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.delete_24px),
                            contentDescription = null,
                            tint = if (logsList.isNotEmpty()) MaterialTheme.colorScheme.error else LocalContentColor.current,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(consoleBgColor)
                    .padding(8.dp)
            ) {
                if (logsList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Console empty. Enable a logging category and perform some actions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    val horizontalScrollState = rememberScrollState()
                    LazyColumn(
                        state = consoleLazyListState,
                        contentPadding = PaddingValues(bottom = 8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScrollState),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logsList) { log ->
                            val color = when {
                                log.contains(" E/") -> MaterialTheme.colorScheme.error
                                log.contains(" W/") -> MaterialTheme.colorScheme.tertiary
                                log.contains(" D/") -> MaterialTheme.colorScheme.primary
                                else -> defaultTextColor
                            }
                            Text(
                                text = log,
                                color = color,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    LaunchedEffect(logsList.size) {
                        if (logsList.isNotEmpty()) {
                            consoleLazyListState.scrollToItem(logsList.size - 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            JuneFloatingActionBar(
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                JuneFloatingAction(
                    onClick = onDismissRequest,
                    label = "Done",
                    icon = { Icon(painterResource(R.drawable.check_24px), contentDescription = "Done") }
                )
            }
        }
    }
}
