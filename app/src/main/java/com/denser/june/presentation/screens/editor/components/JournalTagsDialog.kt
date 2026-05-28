package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.TagCategory
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneDialog
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.utils.TagUtils
import com.denser.june.presentation.utils.UiUtils


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalTagsDialog(
    tags: List<String>,
    suggestions: List<String>,
    onSaveTags: (List<String>) -> Unit,
    onSearchTags: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var localTags by remember { mutableStateOf(tags) }
    val hasChanges = localTags != tags

    var tagInput by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    fun onInsertPrefix(prefix: String) {
        tagInput = prefix
        onSearchTags(prefix)
    }

    fun handleDismiss() {
        if (hasChanges) {
            showExitDialog = true
        } else {
            onDismiss()
        }
    }

    if (showInfoDialog) {
        TagInfoDialog(onDismiss = { showInfoDialog = false })
    }

    if (showExitDialog) {
        JuneDialog(
            onDismissRequest = { showExitDialog = false },
            title = "Save Changes?",
            icon = R.drawable.file_save_24px,
            confirmButton = {
                Button(onClick = {
                    showExitDialog = false
                    onSaveTags(localTags)
                    onDismiss()
                }) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showExitDialog = false
                    onDismiss()
                }) { Text("Discard") }
            },
            text = { Text("You have unsaved changes to your tags. Do you want to save them before exiting?") }
        )
    }

    Dialog(
        onDismissRequest = ::handleDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                JuneTopAppBar(
                    type = JuneAppBarType.CenterAligned,
                    title = {
                        Text(
                            text = "Manage Tags",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        FilledIconButton(
                            onClick = ::handleDismiss,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close_24px),
                                contentDescription = "Close",
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.info_24px),
                                contentDescription = "Tag Help",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            },
            floatingActionButtonPosition = FabPosition.Center,
            floatingActionButton = {
                JuneFloatingActionBar {
                    JuneFloatingAction(
                        onClick = { if (hasChanges) localTags = tags },
                        label = "Reset",
                        icon = { Icon(painterResource(R.drawable.replay_24px), null) },
                        enabled = hasChanges,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    JuneFloatingAction(
                        onClick = {
                            if (hasChanges) {
                                onSaveTags(localTags)
                            }
                            onDismiss()
                        },
                        label = "Done",
                        icon = {
                            Icon(painterResource(R.drawable.check_24px), null)
                        }
                    )
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TagInputArea(
                        tagInput = tagInput,
                        onInputChange = {
                            tagInput = it
                            onSearchTags(it)
                        },
                        suggestions = suggestions,
                        onAddTag = { newTag ->
                            val trimmed = newTag.trim()
                            if (trimmed.isNotBlank() && !localTags.contains(trimmed)) {
                                localTags = localTags + trimmed
                            }
                            tagInput = ""
                            onSearchTags("")
                        },
                        onInsertPrefix = ::onInsertPrefix
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                TagCategory.entries.forEach { category ->
                    val spec = TagUtils.getCategoryUiSpec(category)
                    val categoryTags = TagUtils.filterTagsByCategory(localTags, category)
                    TagSectionCard(
                        title = category.label,
                        prefix = category.prefix,
                        iconRes = spec.iconRes,
                        tags = categoryTags,
                        onRemove = { tagToRemove ->
                            localTags = localTags - tagToRemove
                        },
                        emptyMessage = spec.emptyMessage,
                        tintColor = spec.color
                    )
                }
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TagSectionCard(
    title: String,
    prefix: String?,
    iconRes: Int,
    tags: List<String>,
    onRemove: (String) -> Unit,
    emptyMessage: String,
    tintColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialShapes.Cookie4Sided.toShape(),
                    color = tintColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = tintColor
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                if (prefix != null) {
                    Text(
                        text = prefix,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = tintColor
                    )
                    Spacer(Modifier.width(8.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (tags.isEmpty()) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { onRemove(tag) },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.close_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = TagUtils.getTagInputChipColors(tag),
                            border = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagInputArea(
    tagInput: String,
    onInputChange: (String) -> Unit,
    suggestions: List<String>,
    onAddTag: (String) -> Unit,
    onInsertPrefix: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val charLimit = TagUtils.TAG_CHARACTER_LIMIT
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = tagInput, selection = TextRange(tagInput.length)))
    }

    SideEffect {
        if (tagInput != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = tagInput,
                selection = TextRange(tagInput.length)
            )
        }
    }

    val isInputValid = remember(tagInput) {
        val trimmed = tagInput.trim()
        val isPersonOrTopic = trimmed.startsWith("@") || trimmed.startsWith("#")
        trimmed.isNotBlank() && trimmed != "@" && trimmed != "#" && (!isPersonOrTopic || !trimmed.contains(" "))
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
        ) {
            val showSuggestions = suggestions.isNotEmpty() && tagInput.isNotBlank()

            if (showSuggestions) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions) { suggestion ->
                        SuggestionChip(
                            onClick = { onAddTag(suggestion) },
                            label = { Text(suggestion) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = CircleShape
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            } else if (tagInput.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TagCategory.entries.filter { it.prefix != null }.forEach { category ->
                        val spec = TagUtils.getCategoryUiSpec(category)
                        SuggestionChip(
                            onClick = {
                                onInsertPrefix(category.prefix!!)
                                focusRequester.requestFocus()
                            },
                            label = { Text(category.singularLabel) },
                            icon = {
                                Text(
                                    category.prefix!!,
                                    fontWeight = FontWeight.Bold,
                                    color = spec.color
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = spec.containerColor.copy(alpha = 0.5f),
                                labelColor = spec.color
                            ),
                            border = null,
                            shape = CircleShape
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        val text = newValue.text
                        val isPersonOrTopic = text.startsWith("@") || text.startsWith("#")
                        
                        val isValidChars = if (isPersonOrTopic) {
                            val body = text.removePrefix("@").removePrefix("#")
                            body.all { it.isLetterOrDigit() || it == '_' || it == '-' }
                        } else {
                            text.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == ' ' }
                        }

                        if (isValidChars && text.length <= charLimit && (!isPersonOrTopic || !text.contains(" "))) {
                            textFieldValue = newValue
                            onInputChange(newValue.text)
                        }
                    },
                    placeholder = {
                        Text(
                            "Add tag...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(12.dp),
                    colors = UiUtils.getTransparentTextFieldColors().copy(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (isInputValid) onAddTag(tagInput)
                    }),
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            if (tagInput.isNotEmpty()) {
                                Text(
                                    text = "${tagInput.length}/$charLimit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (tagInput.length == charLimit) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }

                            FilledIconButton(
                                onClick = { onAddTag(tagInput) },
                                enabled = isInputValid,
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.12f
                                    ),
                                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.38f
                                    )
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_upward_24px),
                                    contentDescription = "Add Tag",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}