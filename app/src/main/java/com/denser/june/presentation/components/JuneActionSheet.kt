package com.denser.june.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.denser.june.core.R

@Composable
fun JuneActionContent(
    title: String,
    confirmButtonText: String,
    icon: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    isDestructive: Boolean = false,
    confirmColor: Color? = null,
    confirmIcon: Int = icon,
    dismissIcon: Int = R.drawable.arrow_back_24px,
    dismissButtonText: String = stringResource(R.string.action_cancel),
    onDismissAction: (() -> Unit)? = null,
    confirmText: String? = null,
    confirmEnabled: Boolean = true,
    closeOnConfirm: Boolean = true,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    val actionColor = confirmColor ?: if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val onActionColor = if (actionColor == MaterialTheme.colorScheme.error) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    var textValue by remember { mutableStateOf("") }
    val isConfirmEnabled by remember {
        derivedStateOf { confirmText == null || textValue.equals(confirmText, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 24.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = actionColor.copy(alpha = 0.12f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (!description.isNullOrEmpty()) {
                Text(
                    text = buildAnnotatedString {
                        append(description)
                        if (confirmText != null) {
                            append("\n\n")
                            val fullPrompt = stringResource(R.string.type_to_confirm, confirmText)
                            val target = "'$confirmText'"
                            val startIndex = fullPrompt.indexOf(target)
                            if (startIndex != -1) {
                                append(fullPrompt.substring(0, startIndex))
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    append(target)
                                }
                                append(fullPrompt.substring(startIndex + target.length))
                            } else {
                                append(fullPrompt)
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            } else if (confirmText != null) {
                Text(
                    text = buildAnnotatedString {
                        val fullPrompt = stringResource(R.string.type_to_confirm, confirmText)
                        val target = "'$confirmText'"
                        val startIndex = fullPrompt.indexOf(target)
                        if (startIndex != -1) {
                            append(fullPrompt.substring(0, startIndex))
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                append(target)
                            }
                            append(fullPrompt.substring(startIndex + target.length))
                        } else {
                            append(fullPrompt)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (confirmText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text(confirmText) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                    ),
                    isError = textValue.isNotEmpty() && !textValue.equals(confirmText, ignoreCase = true)
                )
            }
        }

        if (content != null) {
            content()
        }

        Spacer(modifier = Modifier.height(4.dp))

        JuneFloatingActionBar {
            JuneFloatingAction(
                onClick = {
                    if (onDismissAction != null) {
                        onDismissAction()
                    }
                    onDismiss()
                },
                label = dismissButtonText,
                icon = { Icon(painterResource(dismissIcon), contentDescription = null) },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            JuneFloatingAction(
                onClick = {
                    onConfirm()
                    if (closeOnConfirm) {
                        onDismiss()
                    }
                },
                label = confirmButtonText,
                icon = { Icon(painterResource(confirmIcon), contentDescription = null) },
                enabled = isConfirmEnabled && confirmEnabled,
                containerColor = actionColor,
                contentColor = onActionColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuneActionSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    confirmButtonText: String,
    icon: Int,
    modifier: Modifier = Modifier,
    description: String? = null,
    isDestructive: Boolean = false,
    confirmColor: Color? = null,
    confirmIcon: Int = icon,
    dismissIcon: Int = R.drawable.arrow_back_24px,
    dismissButtonText: String = stringResource(R.string.action_cancel),
    onDismissAction: (() -> Unit)? = null,
    confirmText: String? = null,
    confirmEnabled: Boolean = true,
    closeOnConfirm: Boolean = true,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        JuneActionContent(
            title = title,
            confirmButtonText = confirmButtonText,
            icon = icon,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            modifier = modifier,
            description = description,
            isDestructive = isDestructive,
            confirmColor = confirmColor,
            confirmIcon = confirmIcon,
            dismissIcon = dismissIcon,
            dismissButtonText = dismissButtonText,
            onDismissAction = onDismissAction,
            confirmText = confirmText,
            confirmEnabled = confirmEnabled,
            closeOnConfirm = closeOnConfirm,
            content = content
        )
    }
}
