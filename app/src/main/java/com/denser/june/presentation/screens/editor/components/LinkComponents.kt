package com.denser.june.presentation.screens.editor.components

import android.content.ClipData
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import com.denser.hyphen.ui.LocalHyphenRawClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.denser.hyphen.model.MarkupStyle
import com.denser.hyphen.model.MarkupStyleRange
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar
import com.denser.june.presentation.components.JuneTextField
import kotlinx.coroutines.launch

@Composable
fun JuneLinkMenu(
    span: MarkupStyleRange,
    menuOffset: Offset,
    onDismiss: () -> Unit,
    onEditRequest: (MarkupStyleRange) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalHyphenRawClipboard.current ?: LocalClipboard.current
    val scope = rememberCoroutineScope()
    val url = remember(span.style) { (span.style as? MarkupStyle.Link).getDisplayUrl() }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        offset = DpOffset(menuOffset.x.dp, menuOffset.y.dp + 4.dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        DropdownMenuItem(
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            text = { Text("Open Link", style = MaterialTheme.typography.labelLarge) },
            onClick = {
                onDismiss()
                if (url.isNotEmpty()) uriHandler.openUri(url)
            },
            leadingIcon = {
                Icon(
                    painterResource(R.drawable.open_in_new_24px),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
        DropdownMenuItem(
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            text = { Text("Edit Link", style = MaterialTheme.typography.labelLarge) },
            onClick = {
                onDismiss()
                onEditRequest(span)
            },
            leadingIcon = {
                Icon(
                    painterResource(R.drawable.edit_24px),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        )
        DropdownMenuItem(
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            text = { Text("Copy URL", style = MaterialTheme.typography.labelLarge) },
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("URL", url)))
                    onDismiss()
                }
            },
            leadingIcon = {
                Icon(
                    painterResource(R.drawable.content_copy_24px),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuneLinkSheet(
    span: MarkupStyleRange,
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (newText: String, newUrl: String) -> Unit,
) {
    val initialUrl = remember(span.style) { (span.style as? MarkupStyle.Link).getDisplayUrl() }
    var url by remember { mutableStateOf(initialUrl) }
    var text by remember { mutableStateOf(initialText) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.link_24px),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (initialUrl.isEmpty()) "Add Link" else "Edit Link",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                JuneTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Display Text",
                    placeholder = "Enter text to display",
                    leadingIcon = R.drawable.edit_note_24px
                )

                JuneTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = "Link URL",
                    placeholder = "https://example.com",
                    leadingIcon = R.drawable.link_24px
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                JuneFloatingActionBar {
                     JuneFloatingAction(
                        onClick = onDismiss,
                        label = "Cancel",
                        icon = { Icon(painterResource(R.drawable.close_24px), null, Modifier.size(20.dp)) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    JuneFloatingAction(
                        onClick = { onConfirm(text, url) },
                        label = "Confirm",
                        icon = { Icon(painterResource(R.drawable.check_24px), null, Modifier.size(20.dp)) },
                        enabled = url.isNotBlank()
                    )
                }
            }
        }
    }
}

private fun MarkupStyle.Link?.getDisplayUrl(): String {
    val trimmed = this?.url?.trim() ?: return ""
    val bracketIndex = trimmed.lastIndexOf(']')
    if (bracketIndex == -1) return trimmed

    val parenStart = trimmed.indexOf('(', bracketIndex)
    val parenEnd = trimmed.lastIndexOf(')')

    return if (parenStart != -1 && parenEnd > parenStart) {
        trimmed.substring(parenStart + 1, parenEnd)
    } else {
        trimmed
    }
}
