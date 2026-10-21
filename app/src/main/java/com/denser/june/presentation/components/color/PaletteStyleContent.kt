package com.denser.june.presentation.components.color

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import coil.compose.AsyncImage
import com.denser.june.core.R
import com.denser.june.core.domain.model.JournalTheme
import com.denser.june.presentation.utils.ExtractedMediaTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

private fun isThemeMatching(a: JournalTheme?, b: JournalTheme): Boolean {
    if (a == null) return false
    return a.primaryColor == b.primaryColor &&
        a.secondaryColor == b.secondaryColor &&
        a.tertiaryColor == b.tertiaryColor &&
        a.neutralColor == b.neutralColor
}

@Composable
fun PaletteTabContent(
    activeColorSource: ColorSource,
    effectiveSeedColor: Int,
    isDark: Boolean,
    isAmoled: Boolean,
    currentStyle: PaletteStyle,
    appDefaultStyle: PaletteStyle,
    activeJournalTheme: JournalTheme?,
    extractedMediaThemes: List<ExtractedMediaTheme>,
    onMediaThemeSelect: (JournalTheme) -> Unit,
    onStyleSelect: (PaletteStyle) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val isAnyMediaPaletteSelected = activeJournalTheme != null && (
        extractedMediaThemes.any { isThemeMatching(activeJournalTheme, it.theme) } ||
        activeJournalTheme.neutralColor != null ||
        activeJournalTheme.secondaryColor != null ||
        activeJournalTheme.tertiaryColor != null
    )

    val groupedMediaThemes = remember(extractedMediaThemes) {
        extractedMediaThemes.groupBy { it.sourceLabel }
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp)
            .padding(bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (groupedMediaThemes.isNotEmpty()) {
            groupedMediaThemes.forEach { (sourceLabel, themesForSource) ->
                val representative = themesForSource.firstOrNull()
                val isThisMediaActive = activeColorSource is ColorSource.Media && (
                    (representative?.mediaUri != null && representative.mediaUri == activeColorSource.mediaUri) ||
                    sourceLabel == activeColorSource.sourceLabel
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(bottom = 2.dp)
                    ) {
                        if (representative?.mediaUri != null) {
                            AsyncImage(
                                model = representative.mediaUri,
                                contentDescription = sourceLabel,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else if (representative != null) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(representative.theme.primaryColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.art_track_24px),
                                    contentDescription = null,
                                    tint = if (ColorUtils.calculateLuminance(representative.theme.primaryColor) > 0.5) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (isThisMediaActive) {
                            Icon(
                                painter = painterResource(R.drawable.check_circle_24px_fill),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    val themePairs = remember(themesForSource) { themesForSource.chunked(2) }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        themePairs.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                            pair.forEach { mediaThemeItem ->
                                val mediaTheme = mediaThemeItem.theme
                                val scheme = rememberDynamicColorScheme(
                                    primary = Color(mediaTheme.primaryColor),
                                    secondary = mediaTheme.secondaryColor?.let { Color(it) },
                                    tertiary = mediaTheme.tertiaryColor?.let { Color(it) },
                                    neutral = mediaTheme.neutralColor?.let { Color(it) },
                                    style = PaletteStyle.Content,
                                    isDark = isDark,
                                    isAmoled = isAmoled
                                )

                                val isSelected = isThemeMatching(activeJournalTheme, mediaTheme)

                                val paletteColors = listOf(
                                    scheme.primary,
                                    scheme.primaryContainer,
                                    scheme.secondary,
                                    scheme.secondaryContainer,
                                    scheme.tertiary,
                                    scheme.tertiaryContainer,
                                    scheme.surfaceContainerHighest
                                )

                                val label = if (mediaThemeItem.variantLabel.isNotBlank()) {
                                    mediaThemeItem.variantLabel
                                } else {
                                    mediaThemeItem.sourceLabel
                                }

                                PaletteCard(
                                    name = label,
                                    colors = paletteColors,
                                    isSelected = isSelected,
                                    onClick = { onMediaThemeSelect(mediaTheme) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        }

        val stylesSeedColor = if (!isAnyMediaPaletteSelected && activeJournalTheme != null) {
            activeJournalTheme.primaryColor
        } else {
            effectiveSeedColor
        }

        NormalPalettesSection(
            title = "Palettes",
            seedColor = stylesSeedColor,
            isDark = isDark,
            isAmoled = isAmoled,
            isSelected = { style ->
                !isAnyMediaPaletteSelected && (
                    if (activeJournalTheme == null) {
                        activeColorSource is ColorSource.App && style == appDefaultStyle
                    } else {
                        currentStyle == style
                    }
                )
            },
            onStyleSelect = onStyleSelect
        )
    }
}

@Composable
private fun NormalPalettesSection(
    title: String,
    seedColor: Int,
    isDark: Boolean,
    isAmoled: Boolean,
    isSelected: (PaletteStyle) -> Boolean,
    onStyleSelect: (PaletteStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(bottom = 2.dp)
        )

        val stylePairs = remember { PaletteStyle.entries.chunked(2) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stylePairs.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pair.forEach { style ->
                        val scheme = rememberDynamicColorScheme(
                            seedColor = Color(seedColor),
                            isDark = isDark,
                            isAmoled = isAmoled,
                            style = style
                        )

                        val selected = isSelected(style)
                        val paletteColors = listOf(
                            scheme.primary,
                            scheme.primaryContainer,
                            scheme.secondary,
                            scheme.secondaryContainer,
                            scheme.tertiary,
                            scheme.tertiaryContainer,
                            scheme.surfaceContainerHighest
                        )

                        val displayName = remember(style) {
                            style.name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
                        }

                        PaletteCard(
                            name = displayName,
                            colors = paletteColors,
                            isSelected = selected,
                            onClick = { onStyleSelect(style) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

