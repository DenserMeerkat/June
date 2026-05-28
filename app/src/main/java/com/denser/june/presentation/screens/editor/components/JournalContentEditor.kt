package com.denser.june.presentation.screens.editor.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.denser.hyphen.state.HyphenTextState
import com.denser.hyphen.ui.link.HyphenLinkConfig
import com.denser.hyphen.ui.material3.HyphenTextField
import com.denser.hyphen.ui.style.HyphenStyleConfig
import com.denser.hyphen.ui.style.ListItemStyle
import com.denser.june.presentation.utils.UiUtils

@Composable
fun JournalContentEditor(
    state: HyphenTextState,
    onMarkdownChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val linkConfig = remember {
        HyphenLinkConfig(
            dropdownContent = { span, menuOffset, onDismiss, onEditRequest ->
                JuneLinkMenu(
                    span = span,
                    menuOffset = menuOffset,
                    onDismiss = onDismiss,
                    onEditRequest = onEditRequest
                )
            },
            dialogContent = { span, onDismiss, onConfirm ->
                JuneLinkSheet(
                    span = span,
                    initialText = state.text.substring(span.start, span.end),
                    onDismiss = onDismiss,
                    onConfirm = onConfirm
                )
            }
        )
    }

    HyphenTextField(
        state = state,
        linkConfig = linkConfig,
        showDefaultSuggestionsPopup = false,
        triggerPopup = {},
        onMarkdownChange = onMarkdownChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                onFocusChanged(focusState.isFocused)
            },
        placeholder = {
            Text(
                "What's on your mind?",
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
        ),
        colors = UiUtils.getTransparentTextFieldColors().copy(
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        ),
        styleConfig = HyphenStyleConfig(
            boldStyle = SpanStyle(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            italicStyle = SpanStyle(
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            strikethroughStyle = SpanStyle(
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            ),
            highlightStyle = SpanStyle(
                background = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            inlineCodeStyle = SpanStyle(
                background = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            ),
            blockquoteSpanStyle = SpanStyle(
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                background = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            h1Style = SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
            h2Style = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
            h3Style = SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
            h4Style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
            h5Style = SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
            h6Style = SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp),
            bulletListStyle = ListItemStyle(
                prefixStyle = SpanStyle(
                    color = MaterialTheme.colorScheme.tertiary
                )
            ),
            orderedListStyle = ListItemStyle(
                prefixStyle = SpanStyle(
                    color = MaterialTheme.colorScheme.tertiary
                )
            ),
            linkStyle = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
            ),
            mentionStyles = mapOf(
                "person" to SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                ),
                "topic" to SpanStyle(
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium,
                )
            )
        )
    )
}
