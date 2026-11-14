package com.denser.june.presentation.screens.settings.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.june.core.R
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.presentation.components.*
import com.denser.june.presentation.navigation.AppNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConsoleLogsScreen() {
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var logsList by remember { mutableStateOf(emptyList<String>()) }
    var selectedLevel by remember { mutableStateOf<String?>(null) } // null = All

    val refreshLogs = {
        logsList = AppLogger.getBufferedLogs()
    }

    LaunchedEffect(Unit) {
        refreshLogs()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Small,
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(R.string.console_logs)) },
                navigationIcon = {
                    JuneBackButton(onClick = { navigator.navigateBack() })
                },
                actions = {
                    JuneConnectedActionBar(
                        actions = listOf(
                            ConnectedBarAction(
                                iconRes = R.drawable.replay_24px,
                                contentDescription = stringResource(R.string.refresh),
                                onClick = {
                                    refreshLogs()
                                    Toast.makeText(context, R.string.refresh, Toast.LENGTH_SHORT).show()
                                }
                            ),
                            ConnectedBarAction(
                                iconRes = R.drawable.content_copy_24px,
                                contentDescription = stringResource(R.string.action_copy),
                                enabled = logsList.isNotEmpty(),
                                onClick = {
                                    val logs = logsList.joinToString("\n")
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("June App Logs", logs))
                                    Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ),
                            ConnectedBarAction(
                                iconRes = R.drawable.delete_24px,
                                contentDescription = stringResource(R.string.action_clear),
                                enabled = logsList.isNotEmpty(),
                                isDestructive = true,
                                onClick = {
                                    AppLogger.clearBufferedLogs()
                                    refreshLogs()
                                    Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                                }
                            )
                        ),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        checkedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        destructiveColor = MaterialTheme.colorScheme.error,
                        buttonHeight = 36.dp,
                        iconOnlyWidth = 40.dp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Level Filter Connected Button Group
            val levels = listOf(
                null to "All",
                "D" to "Debug",
                "I" to "Info",
                "W" to "Warn",
                "E" to "Error"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                JuneConnectedButtonGroup(
                    options = levels,
                    selected = { (key, _) -> selectedLevel == key },
                    onSelect = { (key, _) -> selectedLevel = key },
                    label = { (_, label) -> label },
                    animateWidthOnPress = false,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ToggleButtonDefaults.toggleButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            val filteredLogs = remember(logsList, selectedLevel) {
                if (selectedLevel == null) {
                    logsList
                } else {
                    logsList.filter { log ->
                        val level = selectedLevel!!
                        log.contains(" $level/") || log.startsWith("$level/")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .clip(JuneConnectedListShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(12.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (logsList.isEmpty()) {
                                "Console empty. Enable a logging category and perform some actions."
                            } else {
                                "No logs match the selected filter."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = filteredLogs,
                            key = { index, log -> "$index-${log.hashCode()}" }
                        ) { _, log ->
                            ConsoleLogItem(log = log)
                        }
                    }

                    LaunchedEffect(filteredLogs.size) {
                        if (filteredLogs.isNotEmpty()) {
                            listState.scrollToItem(filteredLogs.size - 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsoleLogItem(log: String) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }

    val isLongLog = log.length > 250
    val displayText = if (!isExpanded && isLongLog) {
        log.take(250) + "..."
    } else {
        log
    }

    val (textColor, bgColor) = when {
        log.contains(" E/") || log.startsWith("E/") ->
            MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        log.contains(" W/") || log.startsWith("W/") ->
            MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.22f)
        log.contains(" I/") || log.startsWith("I/") ->
            MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.20f)
        log.contains(" D/") || log.startsWith("D/") ->
            MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)
        else ->
            MaterialTheme.colorScheme.onSurface to MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(JuneConnectedItemShape)
            .background(bgColor)
            .combinedClickable(
                onClick = {
                    if (isLongLog) isExpanded = !isExpanded
                },
                onLongClick = {
                    showContextMenu = true
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .animateContentSize()
    ) {
        Column {
            Text(
                text = displayText,
                color = textColor,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 15.sp
            )

            if (isLongLog) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Show Less" else "Show More",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }

            JuneDropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                JuneDropdownMenuItem(
                    text = { Text(stringResource(R.string.action_copy)) },
                    onClick = {
                        showContextMenu = false
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("June App Log Entry", log))
                        Toast.makeText(context, "Log entry copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.content_copy_24px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                if (isLongLog) {
                    JuneDropdownMenuItem(
                        text = { Text(if (isExpanded) "Collapse" else "Expand Full Log") },
                        onClick = {
                            showContextMenu = false
                            isExpanded = !isExpanded
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(
                                    if (isExpanded) R.drawable.chevron_left_24px else R.drawable.chevron_right_24px
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

