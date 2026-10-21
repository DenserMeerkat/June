package com.denser.june.presentation.components.color

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.JournalTheme
import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar
import com.denser.june.presentation.theme.LocalAppTheme
import com.denser.june.presentation.utils.ExtractedMediaColor
import com.denser.june.presentation.utils.ExtractedMediaTheme
import com.materialkolor.PaletteStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorCustomizationPanel(
    journalTheme: JournalTheme?,
    extractedMediaColors: List<ExtractedMediaColor> = emptyList(),
    extractedMediaThemes: List<ExtractedMediaTheme> = emptyList(),
    onThemeChange: (JournalTheme?) -> Unit,
    modifier: Modifier = Modifier,
    showAppDefault: Boolean = true,
    contentHeight: Dp = 420.dp,
    initialTab: ColorScreenTab = ColorScreenTab.COLOR,
    selectedTab: ColorScreenTab? = null,
    onTabSelect: ((ColorScreenTab) -> Unit)? = null,
    toggleTitle: String? = null,
    toggleChecked: Boolean? = null,
    onToggleChange: ((Boolean) -> Unit)? = null,
    allowPaletteWhenDefault: Boolean = false
) {
    var internalTab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initialTab) }
    val currentTab = selectedTab ?: internalTab
    val handleTabSelect: (ColorScreenTab) -> Unit = { tab ->
        if (onTabSelect != null) {
            onTabSelect(tab)
        } else {
            internalTab = tab
        }
    }

    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val floatingBarBottomInset = 76.dp + navBottomPadding
    val totalHeight = contentHeight + 16.dp + navBottomPadding

    val appTheme = LocalAppTheme.current
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (appTheme.themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val isMediaTheme = journalTheme != null && (journalTheme.secondaryColor != null || journalTheme.neutralColor != null)
    val selectedSeedColor = journalTheme?.takeUnless { isMediaTheme }?.primaryColor
    val appDefaultSeedColor = if (appTheme.materialTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorResource(android.R.color.system_accent1_200).toArgb()
    } else {
        appTheme.seedColor
    }

    val effectiveSeedColor = selectedSeedColor ?: appDefaultSeedColor

    val currentPaletteStyle = journalTheme?.style?.let {
        runCatching { PaletteStyle.valueOf(it) }.getOrNull()
    } ?: appTheme.style

    val isCustomActive = journalTheme != null
    val isPaletteActive = isCustomActive || allowPaletteWhenDefault

    LaunchedEffect(isPaletteActive) {
        if (!isPaletteActive && currentTab == ColorScreenTab.PALETTE) {
            handleTabSelect(ColorScreenTab.COLOR)
        }
    }

    val availableMediaSources = remember(extractedMediaColors, extractedMediaThemes) {
        val list = mutableListOf<ColorSource.Media>()
        val seen = mutableSetOf<String>()

        extractedMediaThemes.forEach { mt ->
            val key = mt.mediaUri?.toString() ?: mt.sourceLabel
            if (seen.add(key)) {
                list.add(ColorSource.Media(mt.sourceLabel, mt.mediaUri, mt.theme.primaryColor))
            }
        }
        extractedMediaColors.forEach { mc ->
            val key = mc.mediaUri?.toString() ?: mc.sourceLabel
            if (seen.add(key)) {
                list.add(ColorSource.Media(mc.sourceLabel, mc.mediaUri, mc.color))
            }
        }
        list
    }

    var activeColorSource by remember {
        mutableStateOf<ColorSource>(
            when {
                journalTheme == null -> ColorSource.App
                isMediaTheme -> {
                    val matchedMedia = availableMediaSources.firstOrNull { media ->
                        extractedMediaThemes.any { mt ->
                            (mt.mediaUri == media.mediaUri || mt.sourceLabel == media.sourceLabel) &&
                            mt.theme.primaryColor == journalTheme.primaryColor &&
                            mt.theme.secondaryColor == journalTheme.secondaryColor &&
                            mt.theme.tertiaryColor == journalTheme.tertiaryColor
                        }
                    }
                    matchedMedia ?: ColorSource.Custom
                }
                else -> {
                    val matchedMedia = availableMediaSources.firstOrNull { media ->
                        extractedMediaColors.any { mc ->
                            (mc.mediaUri == media.mediaUri || mc.sourceLabel == media.sourceLabel) &&
                            mc.color == journalTheme.primaryColor
                        } || media.seedColor == journalTheme.primaryColor
                    }
                    matchedMedia ?: ColorSource.Custom
                }
            }
        )
    }

    LaunchedEffect(journalTheme) {
        if (journalTheme == null) {
            activeColorSource = ColorSource.App
        } else if (journalTheme.secondaryColor != null || journalTheme.neutralColor != null) {
            val matchedMediaTheme = extractedMediaThemes.firstOrNull { mt ->
                mt.theme.primaryColor == journalTheme.primaryColor &&
                mt.theme.secondaryColor == journalTheme.secondaryColor &&
                mt.theme.tertiaryColor == journalTheme.tertiaryColor &&
                mt.theme.neutralColor == journalTheme.neutralColor
            }
            if (matchedMediaTheme != null) {
                val mediaSource = availableMediaSources.firstOrNull {
                    (it.mediaUri != null && it.mediaUri == matchedMediaTheme.mediaUri) ||
                    it.sourceLabel == matchedMediaTheme.sourceLabel
                }
                if (mediaSource != null) {
                    activeColorSource = mediaSource
                }
            }
        } else if (activeColorSource is ColorSource.Media) {
            val currentMedia = activeColorSource as ColorSource.Media
            val isColorFromCurrentMedia = extractedMediaColors.any { mc ->
                (mc.mediaUri == currentMedia.mediaUri || mc.sourceLabel == currentMedia.sourceLabel) &&
                mc.color == journalTheme.primaryColor
            } || currentMedia.seedColor == journalTheme.primaryColor

            if (!isColorFromCurrentMedia) {
                val otherMedia = availableMediaSources.firstOrNull { media ->
                    extractedMediaColors.any { mc ->
                        (mc.mediaUri == media.mediaUri || mc.sourceLabel == media.sourceLabel) &&
                        mc.color == journalTheme.primaryColor
                    } || media.seedColor == journalTheme.primaryColor
                }
                activeColorSource = otherMedia ?: ColorSource.Custom
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = sheetShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                when (currentTab) {
                    ColorScreenTab.COLOR -> {
                        ColorTabContent(
                            isCustomActive = isCustomActive,
                            activeColorSource = activeColorSource,
                            availableMediaSources = availableMediaSources,
                            effectiveSeedColor = effectiveSeedColor,
                            currentPaletteStyle = currentPaletteStyle,
                            extractedMediaColors = extractedMediaColors,
                            activeJournalTheme = journalTheme,
                            extractedMediaThemes = extractedMediaThemes,
                            isDark = isDarkTheme,
                            isAmoled = appTheme.withAmoled,
                            onToggleCustom = { enabled ->
                                if (enabled) {
                                    activeColorSource = ColorSource.Custom
                                    onThemeChange(
                                        JournalTheme(
                                            primaryColor = appDefaultSeedColor,
                                            style = appTheme.style.name
                                        )
                                    )
                                } else {
                                    activeColorSource = ColorSource.App
                                    onThemeChange(null)
                                }
                            },
                            onSourceSelect = { source ->
                                activeColorSource = source
                                when (source) {
                                    ColorSource.App -> {
                                        onThemeChange(null)
                                    }
                                    is ColorSource.Media -> {
                                        onThemeChange(
                                            JournalTheme(
                                                primaryColor = source.seedColor,
                                                style = currentPaletteStyle.name
                                            )
                                        )
                                    }
                                    ColorSource.Custom -> {
                                        onThemeChange(
                                            JournalTheme(
                                                primaryColor = appDefaultSeedColor,
                                                style = currentPaletteStyle.name
                                            )
                                        )
                                    }
                                }
                            },
                            onThemeChange = onThemeChange,
                            bottomPadding = floatingBarBottomInset,
                            modifier = Modifier.fillMaxSize(),
                            toggleTitle = toggleTitle,
                            toggleChecked = toggleChecked ?: isCustomActive,
                            onToggleChange = onToggleChange
                        )
                    }
                    ColorScreenTab.PALETTE -> {
                        PaletteTabContent(
                            activeColorSource = activeColorSource,
                            effectiveSeedColor = effectiveSeedColor,
                            isDark = isDarkTheme,
                            isAmoled = appTheme.withAmoled,
                            currentStyle = currentPaletteStyle,
                            appDefaultStyle = appTheme.style,
                            activeJournalTheme = journalTheme,
                            extractedMediaThemes = extractedMediaThemes,
                            onMediaThemeSelect = { mediaTheme ->
                                val matchedMedia = availableMediaSources.firstOrNull { media ->
                                    extractedMediaThemes.any { mt ->
                                        (mt.mediaUri == media.mediaUri || mt.sourceLabel == media.sourceLabel) &&
                                        mt.theme == mediaTheme
                                    }
                                }
                                if (matchedMedia != null) {
                                    activeColorSource = matchedMedia
                                }
                                onThemeChange(mediaTheme)
                            },
                            onStyleSelect = { style ->
                                val isFromMediaPalette = journalTheme?.secondaryColor != null ||
                                    journalTheme?.neutralColor != null ||
                                    journalTheme?.tertiaryColor != null

                                val seed = if (isFromMediaPalette) {
                                    appDefaultSeedColor
                                } else {
                                    effectiveSeedColor
                                }

                                if (isFromMediaPalette) {
                                    activeColorSource = ColorSource.Custom
                                }

                                val isDefaultAppSeed = seed == appDefaultSeedColor
                                val isDefaultAppStyle = style == appTheme.style
                                if (activeColorSource is ColorSource.App && isDefaultAppStyle && isDefaultAppSeed) {
                                    onThemeChange(null)
                                } else {
                                    val updated = JournalTheme(
                                        primaryColor = seed,
                                        style = style.name,
                                        secondaryColor = null,
                                        tertiaryColor = null,
                                        neutralColor = null
                                    )
                                    onThemeChange(updated)
                                }
                            },
                            bottomPadding = floatingBarBottomInset,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        JuneFloatingActionBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            ColorScreenTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                val isTabEnabled = when (tab) {
                    ColorScreenTab.COLOR -> true
                    ColorScreenTab.PALETTE -> isPaletteActive
                }
                val label = when (tab) {
                    ColorScreenTab.COLOR -> "Color"
                    ColorScreenTab.PALETTE -> "Palette"
                }
                val iconRes = when (tab) {
                    ColorScreenTab.COLOR -> R.drawable.colors_24px
                    ColorScreenTab.PALETTE -> R.drawable.palette_24px
                }

                JuneFloatingAction(
                    selected = isSelected,
                    enabled = isTabEnabled,
                    onClick = { if (isTabEnabled) handleTabSelect(tab) },
                    label = label,
                    icon = if (isSelected) {
                        {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}
