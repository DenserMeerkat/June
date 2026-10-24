package com.denser.june.presentation.components.color

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import coil.compose.AsyncImage
import com.denser.june.core.R
import com.denser.june.core.domain.model.JournalTheme
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneSliderTile
import com.denser.june.core.media.color.ExtractedMediaColor
import com.denser.june.core.media.color.ExtractedMediaTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.coroutines.delay

@Composable
fun ColorTabContent(
    isCustomActive: Boolean,
    activeColorSource: ColorSource,
    availableMediaSources: List<ColorSource.Media>,
    effectiveSeedColor: Int,
    currentPaletteStyle: PaletteStyle,
    extractedMediaColors: List<ExtractedMediaColor>,
    activeJournalTheme: JournalTheme? = null,
    extractedMediaThemes: List<ExtractedMediaTheme> = emptyList(),
    isDark: Boolean = false,
    isAmoled: Boolean = false,
    onToggleCustom: (Boolean) -> Unit,
    onSourceSelect: (ColorSource) -> Unit,
    onThemeChange: (JournalTheme?) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    toggleTitle: String? = null,
    toggleChecked: Boolean = isCustomActive,
    onToggleChange: ((Boolean) -> Unit)? = null,
) {
    val effectiveToggleChecked = onToggleChange?.let { toggleChecked } ?: isCustomActive
    val effectiveOnToggle = onToggleChange ?: onToggleCustom
    val verticalScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val hsl = remember(effectiveSeedColor) {
        val outHsl = FloatArray(3)
        ColorUtils.colorToHSL(effectiveSeedColor, outHsl)
        val defaultS = if (outHsl[1] < 0.05f) 0.8f else outHsl[1]
        val defaultL = if (outHsl[2] < 0.05f || outHsl[2] > 0.95f) 0.5f else outHsl[2]
        floatArrayOf(outHsl[0], defaultS, defaultL)
    }

    var localHue by rememberSaveable { mutableFloatStateOf(hsl[0]) }
    var localSaturation by rememberSaveable { mutableFloatStateOf(hsl[1]) }
    var localLightness by rememberSaveable { mutableFloatStateOf(hsl[2]) }

    var lastEmittedColor by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(effectiveSeedColor) {
        if (effectiveSeedColor != lastEmittedColor) {
            val outHsl = FloatArray(3)
            ColorUtils.colorToHSL(effectiveSeedColor, outHsl)
            val defaultS = if (outHsl[1] < 0.05f) 0.8f else outHsl[1]
            val defaultL = if (outHsl[2] < 0.05f || outHsl[2] > 0.95f) 0.5f else outHsl[2]
            localHue = outHsl[0]
            localSaturation = defaultS
            localLightness = defaultL
        }
    }

    val currentOnThemeChange by rememberUpdatedState(onThemeChange)
    val currentStyleName by rememberUpdatedState(currentPaletteStyle.name)

    var pendingColor by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(pendingColor) {
        val color = pendingColor ?: return@LaunchedEffect
        delay(35L)
        lastEmittedColor = color
        currentOnThemeChange(
            JournalTheme(
                primaryColor = color,
                style = currentStyleName
            )
        )
    }

    fun commitColor(newH: Float, newS: Float, newL: Float) {
        val newColor = ColorUtils.HSLToColor(floatArrayOf(newH, newS, newL))
        pendingColor = newColor
    }

    Column(
        modifier = modifier
            .verticalScroll(verticalScrollState)
            .padding(bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        JuneConnectedItem(
            title = toggleTitle ?: if (isCustomActive) {
                stringResource(R.string.custom_theme)
            } else {
                stringResource(R.string.default_theme)
            },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
            onClick = { effectiveOnToggle(!effectiveToggleChecked) },
            trailingContent = {
                Switch(
                    checked = effectiveToggleChecked,
                    onCheckedChange = { effectiveOnToggle(it) }
                )
            }
        )

        val controlsAlpha = if (isCustomActive) 1f else 0.38f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(controlsAlpha),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (availableMediaSources.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Media",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    var displayedMediaSource by remember(availableMediaSources) {
                        mutableStateOf(
                            (activeColorSource as? ColorSource.Media)
                                ?: availableMediaSources.firstOrNull()
                        )
                    }

                    LaunchedEffect(activeColorSource) {
                        if (activeColorSource is ColorSource.Media) {
                            displayedMediaSource = activeColorSource
                        }
                    }

                    val mediaScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(mediaScrollState),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(10.dp))
                        availableMediaSources.forEach { mediaSource ->
                            val isChosen = isCustomActive &&
                                activeColorSource is ColorSource.Media &&
                                activeColorSource.sourceLabel == mediaSource.sourceLabel &&
                                activeColorSource.mediaUri == mediaSource.mediaUri

                            val isFocused = displayedMediaSource?.sourceLabel == mediaSource.sourceLabel &&
                                displayedMediaSource?.mediaUri == mediaSource.mediaUri

                            val cardShape = if (isFocused) {
                                RoundedCornerShape(20.dp)
                            } else {
                                RoundedCornerShape(12.dp)
                            }

                            val selectionColor = MaterialTheme.colorScheme.primary

                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(cardShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .then(
                                        if (isChosen) {
                                            Modifier.border(2.5.dp, selectionColor, cardShape)
                                        } else {
                                            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), cardShape)
                                        }
                                    )
                                    .clickable(enabled = isCustomActive) {
                                        displayedMediaSource = mediaSource
                                        onSourceSelect(mediaSource)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (mediaSource.mediaUri != null) {
                                    AsyncImage(
                                        model = mediaSource.mediaUri,
                                        contentDescription = mediaSource.sourceLabel,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(mediaSource.seedColor)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.art_track_24px),
                                            contentDescription = null,
                                            tint = if (ColorUtils.calculateLuminance(mediaSource.seedColor) > 0.5) Color.Black else Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                if (isChosen) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.35f))
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.check_circle_24px_fill),
                                        contentDescription = null,
                                        tint = selectionColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                        Box(modifier = Modifier.width(10.dp))
                    }

                    val mediaForSwatches = displayedMediaSource
                    if (isCustomActive && mediaForSwatches != null) {
                        val mediaColors = remember(extractedMediaColors, mediaForSwatches) {
                            extractedMediaColors.filter {
                                (it.mediaUri != null && it.mediaUri == mediaForSwatches.mediaUri) ||
                                it.sourceLabel == mediaForSwatches.sourceLabel
                            }
                        }

                        val isChosenMedia = activeColorSource is ColorSource.Media &&
                            activeColorSource.sourceLabel == mediaForSwatches.sourceLabel &&
                            activeColorSource.mediaUri == mediaForSwatches.mediaUri

                        val isMediaPaletteSelected = isChosenMedia && activeJournalTheme != null && (
                            extractedMediaThemes.any { mt ->
                                mt.theme.primaryColor == activeJournalTheme.primaryColor &&
                                mt.theme.secondaryColor == activeJournalTheme.secondaryColor &&
                                mt.theme.tertiaryColor == activeJournalTheme.tertiaryColor &&
                                mt.theme.neutralColor == activeJournalTheme.neutralColor
                            } ||
                            activeJournalTheme.neutralColor != null ||
                            activeJournalTheme.secondaryColor != null ||
                            activeJournalTheme.tertiaryColor != null
                        )

                        if (mediaColors.isNotEmpty() || isMediaPaletteSelected) {
                            val swatchesScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(swatchesScrollState),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(14.dp))
                                mediaColors.forEach { mc ->
                                    val isColorSelected = isChosenMedia && !isMediaPaletteSelected && effectiveSeedColor == mc.color
                                    ColorSwatch(
                                        color = mc.color,
                                        isSelected = isColorSelected,
                                        enabled = isCustomActive,
                                        onClick = {
                                            displayedMediaSource = mediaForSwatches
                                            onSourceSelect(mediaForSwatches)
                                            onThemeChange(
                                                JournalTheme(
                                                    primaryColor = mc.color,
                                                    style = currentPaletteStyle.name
                                                )
                                            )
                                        }
                                    )
                                }

                                if (isMediaPaletteSelected) {
                                    val paletteScheme = rememberDynamicColorScheme(
                                        primary = Color(activeJournalTheme.primaryColor),
                                        secondary = activeJournalTheme.secondaryColor?.let { Color(it) },
                                        tertiary = activeJournalTheme.tertiaryColor?.let { Color(it) },
                                        neutral = activeJournalTheme.neutralColor?.let { Color(it) },
                                        style = PaletteStyle.Content,
                                        isDark = isDark,
                                        isAmoled = isAmoled
                                    )
                                    val swatchColors = listOf(
                                        paletteScheme.primary,
                                        paletteScheme.primaryContainer,
                                        paletteScheme.secondary,
                                        paletteScheme.tertiary
                                    )

                                    PaletteSwatch(
                                        colors = swatchColors,
                                        isSelected = true,
                                        enabled = isCustomActive,
                                        onClick = {}
                                    )
                                }
                                Box(modifier = Modifier.width(14.dp))
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val currentDisplayColor = remember(localHue, localSaturation, localLightness) {
                    ColorUtils.HSLToColor(floatArrayOf(localHue, localSaturation, localLightness))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(currentDisplayColor))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Text(
                            text = String.format("#%06X", (0xFFFFFF and currentDisplayColor)),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val hueGradient = remember {
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFF0000),
                            Color(0xFFFFFF00),
                            Color(0xFF00FF00),
                            Color(0xFF00FFFF),
                            Color(0xFF0000FF),
                            Color(0xFFFF00FF),
                            Color(0xFFFF0000)
                        )
                    )
                }

                val saturationGradient = remember(localHue, localLightness) {
                    val startColor = ColorUtils.HSLToColor(floatArrayOf(localHue, 0f, localLightness))
                    val endColor = ColorUtils.HSLToColor(floatArrayOf(localHue, 1f, localLightness))
                    Brush.horizontalGradient(listOf(Color(startColor), Color(endColor)))
                }

                val lightnessGradient = remember(localHue, localSaturation) {
                    val black = ColorUtils.HSLToColor(floatArrayOf(localHue, localSaturation, 0f))
                    val midColor = ColorUtils.HSLToColor(floatArrayOf(localHue, localSaturation, 0.5f))
                    val white = ColorUtils.HSLToColor(floatArrayOf(localHue, localSaturation, 1f))
                    Brush.horizontalGradient(listOf(Color(black), Color(midColor), Color(white)))
                }

                JuneSliderTile(
                    label = "Hue",
                    value = localHue,
                    onValueChange = {
                        localHue = it
                        commitColor(it, localSaturation, localLightness)
                    },
                    valueRange = 0f..360f,
                    valueDisplay = "${localHue.toInt()}°",
                    trackBrush = hueGradient,
                    thumbColor = Color(ColorUtils.HSLToColor(floatArrayOf(localHue, 1f, 0.5f))),
                    enabled = isCustomActive
                )

                JuneSliderTile(
                    label = "Saturation",
                    value = localSaturation,
                    onValueChange = {
                        localSaturation = it
                        commitColor(localHue, it, localLightness)
                    },
                    valueRange = 0f..1f,
                    valueDisplay = "${(localSaturation * 100).toInt()}%",
                    trackBrush = saturationGradient,
                    thumbColor = Color(ColorUtils.HSLToColor(floatArrayOf(localHue, localSaturation, localLightness))),
                    enabled = isCustomActive
                )

                JuneSliderTile(
                    label = "Lightness",
                    value = localLightness,
                    onValueChange = {
                        localLightness = it
                        commitColor(localHue, localSaturation, it)
                    },
                    valueRange = 0f..1f,
                    valueDisplay = "${(localLightness * 100).toInt()}%",
                    trackBrush = lightnessGradient,
                    thumbColor = Color(ColorUtils.HSLToColor(floatArrayOf(localHue, localSaturation, localLightness))),
                    enabled = isCustomActive
                )
            }
        }
    }
}

