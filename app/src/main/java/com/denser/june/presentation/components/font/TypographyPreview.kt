package com.denser.june.presentation.components.font

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.hyphen.state.HyphenTextState
import com.denser.hyphen.ui.material3.HyphenTextField
import com.denser.hyphen.ui.style.BlockquoteStyle
import com.denser.hyphen.ui.style.HyphenStyleConfig
import com.denser.hyphen.ui.style.ListItemStyle
import com.denser.june.core.R
import com.denser.june.core.domain.model.FontVariationConfig
import com.denser.june.core.domain.model.Journal
import com.denser.june.core.domain.model.JournalLocation
import com.denser.june.presentation.screens.editor.components.EditorMetadataBar
import com.denser.june.presentation.screens.home.timeline.components.journal.TimelineJournalCard
import com.denser.june.presentation.theme.provideTypography
import com.denser.june.presentation.utils.UiUtils
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TypographyPreview(
    modifier: Modifier = Modifier,
    fontName: String,
    variation: FontVariationConfig? = null,
    contentBottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val previewTypography = provideTypography(
        font = fontName,
        variation = variation
    )

    val sampleTitle = "Do not open before..."
    val sampleMarkdown = """
I don't even know why I'm writing this. Maybe because my therapist said keeping a journal would help me process things after Dad left in June. But **nothing makes sense anymore**. Today changed *everything*.

> "My question is not how, but when."

Things felt weird the second we stepped outside tonight:
- The woods were dead silent
- Flashlights started flickering near the #caves entrance
- A low hum in the air, like vibrating wire

~~Maybe it was just the wind.~~ No, it definitely wasn't.

Bartosz said we were just chasing an adventure, but @Martha and I knew better. Now @Mikkel is gone. We searched for hours in the pouring rain, but he simply vanished into thin air outside #Winden. I can't shake the feeling that whatever happened at `22:13` is tied to everything we thought we understood.
    """.trimIndent()

    val hyphenState = remember {
        HyphenTextState(
            initialTriggerConfigs = listOf(
                com.denser.hyphen.model.TriggerConfig(trigger = "@", scheme = "person"),
                com.denser.hyphen.model.TriggerConfig(trigger = "#", scheme = "topic")
            )
        ).apply {
            setMarkdown(sampleMarkdown)
        }
    }

    val sampleJournal = remember {
        val date = LocalDateTime.of(2019, 11, 4, 22, 14)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        Journal(
            id = "preview_dark_journal",
            title = sampleTitle,
            content = sampleMarkdown,
            emoji = "⏳",
            tags = listOf("Winden", "Mikkel", "SicMundus"),
            location = JournalLocation(
                address = "Winden Caves, Germany",
                latitude = 48.1351,
                longitude = 11.5820
            ),
            createdAt = date,
            updatedAt = date,
            dateTime = date,
            isBookmarked = true,
            isDraft = false
        )
    }

    MaterialTheme(typography = previewTypography) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = contentBottomPadding)
        ) {
            TimelineJournalCard(
                journal = sampleJournal,
                onJournalClick = {},
                onToggleBookmark = {},
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            var titleText by remember { mutableStateOf(sampleTitle) }
            val titleTextStyle = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold
            )

            TextField(
                value = titleText,
                onValueChange = { titleText = it },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        stringResource(R.string.add_title),
                        style = titleTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                textStyle = titleTextStyle,
                colors = UiUtils.getTransparentTextFieldColors()
            )

            EditorMetadataBar(
                emoji = "⏳",
                formattedDate = "Mon, Nov 4",
                formattedTime = "10:13 PM",
                tagCount = 3,
                onEmojiClick = {},
                onDatePickerOpen = {},
                onTimePickerOpen = {},
                onTagsClick = {}
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                HyphenTextField(
                    state = hyphenState,
                    showDefaultSuggestionsPopup = false,
                    triggerPopup = {},
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    placeholder = {
                        Text(
                            stringResource(R.string.whats_on_your_mind),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default,
                    colors = UiUtils.getTransparentTextFieldColors().copy(
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    ),
                    styleConfig = HyphenStyleConfig(
                        boldStyle = SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        italicStyle = SpanStyle(
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        strikethroughStyle = SpanStyle(
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        blockquoteStyle = BlockquoteStyle(
                            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                            borderColor = MaterialTheme.colorScheme.primary
                        ),
                        h1Style = SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
                        h2Style = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
                        h3Style = SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
                        h4Style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
                        h5Style = SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
                        h6Style = SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp),
                        bulletListStyle = ListItemStyle(
                            prefixStyle = SpanStyle(
                                color = MaterialTheme.colorScheme.primary
                            )
                        ),
                        orderedListStyle = ListItemStyle(
                            prefixStyle = SpanStyle(
                                color = MaterialTheme.colorScheme.primary
                            )
                        ),
                        linkStyle = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        ),
                        mentionStyles = mapOf(
                            "person" to SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            ),
                            "topic" to SpanStyle(
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    )
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                )
            }
        }
    }
}
