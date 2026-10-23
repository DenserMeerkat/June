package com.denser.june.presentation.components.font

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.FontPresets
import com.denser.june.core.domain.model.FontVariationConfig
import com.denser.june.core.utils.FontUtils
import com.denser.june.presentation.components.JuneConnectedButtonGroup
import com.denser.june.presentation.components.JuneSliderTile
import kotlin.math.round

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FontStyleContent(
    fontName: String,
    currentVariation: FontVariationConfig?,
    onVariationChange: (FontVariationConfig?) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp
) {
    val isGoogleSansFlex = FontUtils.isGoogleSansFlex(fontName)
    val isRobotoFlex = FontUtils.isRobotoFlex(fontName)
    val isRecursive = FontUtils.isRecursive(fontName)
    val isShantellSans = FontUtils.isShantellSans(fontName)
    val isVariableFont = FontUtils.isVariableFont(fontName)

    val defaultConfig = remember(fontName) {
        FontPresets.getDefaultVariation(fontName)
    }

    val weight = currentVariation?.weight ?: (defaultConfig.weight ?: 400f)
    val width = currentVariation?.width ?: (defaultConfig.width ?: 100f)
    val roundness = currentVariation?.roundness ?: (defaultConfig.roundness ?: 100f)
    val grade = currentVariation?.grade ?: (defaultConfig.grade ?: 0f)
    val opticalSize = currentVariation?.opticalSize ?: (defaultConfig.opticalSize ?: 14f)
    val slant = currentVariation?.slant ?: (defaultConfig.slant ?: 0f)

    val monospace = currentVariation?.monospace ?: (defaultConfig.monospace ?: 0f)
    val casual = currentVariation?.casual ?: (defaultConfig.casual ?: 0f)
    val cursive = currentVariation?.cursive ?: (defaultConfig.cursive ?: 0.5f)

    val bounce = currentVariation?.bounce ?: (defaultConfig.bounce ?: 0f)
    val informality = currentVariation?.informality ?: (defaultConfig.informality ?: 0f)
    val spacing = currentVariation?.spacing ?: (defaultConfig.spacing ?: 0f)

    val counterWidth = currentVariation?.counterWidth ?: (defaultConfig.counterWidth ?: 468f)
    val thickStroke = currentVariation?.thickStroke ?: (defaultConfig.thickStroke ?: 96f)
    val thinStroke = currentVariation?.thinStroke ?: (defaultConfig.thinStroke ?: 79f)
    val uppercaseHeight = currentVariation?.uppercaseHeight ?: (defaultConfig.uppercaseHeight ?: 712f)
    val lowercaseHeight = currentVariation?.lowercaseHeight ?: (defaultConfig.lowercaseHeight ?: 514f)
    val ascenderHeight = currentVariation?.ascenderHeight ?: (defaultConfig.ascenderHeight ?: 750f)
    val descenderDepth = currentVariation?.descenderDepth ?: (defaultConfig.descenderDepth ?: -203f)
    val figureHeight = currentVariation?.figureHeight ?: (defaultConfig.figureHeight ?: 738f)

    var showParametricAxes by remember { mutableStateOf(false) }

    val presets = remember(fontName) {
        FontPresets.getPresetsForFont(fontName)
    }

    val activePreset = remember(
        currentVariation,
        fontName,
        weight,
        width,
        roundness,
        grade,
        opticalSize,
        slant,
        monospace,
        casual,
        cursive,
        bounce,
        informality,
        spacing
    ) {
        presets.find { preset ->
            val c = preset.config
            when {
                isGoogleSansFlex -> c.width == width && c.roundness == roundness && c.grade == grade && (c.opticalSize ?: 18f) == opticalSize && (c.slant ?: 0f) == slant
                isRobotoFlex -> c.weight == weight && c.width == width && c.grade == grade && (c.opticalSize ?: 14f) == opticalSize && (c.slant ?: 0f) == slant
                isRecursive -> c.weight == weight && (c.slant ?: 0f) == slant && kotlin.math.abs((c.monospace ?: 0f) - monospace) < 0.01f && kotlin.math.abs((c.casual ?: 0f) - casual) < 0.01f && (c.cursive ?: 0.5f) == cursive
                isShantellSans -> c.weight == weight && (c.bounce ?: 0f) == bounce && (c.informality ?: 0f) == informality && (c.spacing ?: 0f) == spacing
                else -> false
            }
        }
    }

    val currentConfig = currentVariation ?: FontVariationConfig(
        weight = weight,
        width = width,
        roundness = roundness,
        grade = grade,
        opticalSize = opticalSize,
        slant = slant,
        monospace = monospace,
        casual = casual,
        cursive = cursive,
        bounce = bounce,
        informality = informality,
        spacing = spacing,
        counterWidth = counterWidth,
        thickStroke = thickStroke,
        thinStroke = thinStroke,
        uppercaseHeight = uppercaseHeight,
        lowercaseHeight = lowercaseHeight,
        ascenderHeight = ascenderHeight,
        descenderDepth = descenderDepth,
        figureHeight = figureHeight
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (presets.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.presets),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    JuneConnectedButtonGroup(
                        options = presets,
                        selected = { it == activePreset },
                        onSelect = { preset ->
                            onVariationChange(preset.config)
                        },
                        enabled = true,
                        label = { it.label },
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.38f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        ),
                        border = null
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isRecursive) {
                JuneSliderTile(
                    label = stringResource(R.string.monospace),
                    valueDisplay = when {
                        monospace < 0.1f -> "Sans (0%)"
                        monospace > 0.9f -> "Mono (100%)"
                        else -> "${(monospace * 100).toInt()}%"
                    },
                    value = monospace,
                    valueRange = 0f..1f,
                    enabled = true,
                    onValueChange = {
                        val snapped = (round(it * 100) / 100f)
                        onVariationChange(currentConfig.copy(monospace = snapped))
                    }
                )

                JuneSliderTile(
                    label = stringResource(R.string.casual),
                    valueDisplay = when {
                        casual < 0.1f -> "Linear (0%)"
                        casual > 0.9f -> "Casual (100%)"
                        else -> "${(casual * 100).toInt()}%"
                    },
                    value = casual,
                    valueRange = 0f..1f,
                    enabled = true,
                    onValueChange = {
                        val snapped = (round(it * 100) / 100f)
                        onVariationChange(currentConfig.copy(casual = snapped))
                    }
                )

                JuneSliderTile(
                    label = stringResource(R.string.cursive),
                    valueDisplay = when {
                        cursive < 0.25f -> "Off (0)"
                        cursive > 0.75f -> "On (1)"
                        else -> "Auto (0.5)"
                    },
                    value = cursive,
                    valueRange = 0f..1f,
                    steps = 1,
                    enabled = true,
                    onValueChange = {
                        val snapped = when {
                            it < 0.25f -> 0f
                            it > 0.75f -> 1f
                            else -> 0.5f
                        }
                        onVariationChange(currentConfig.copy(cursive = snapped))
                    }
                )

                JuneSliderTile(
                    label = stringResource(R.string.weight),
                    valueDisplay = "${weight.toInt()}",
                    value = weight,
                    valueRange = 300f..1000f,
                    enabled = true,
                    onValueChange = {
                        val snapped = round(it)
                        onVariationChange(currentConfig.copy(weight = snapped))
                    }
                )

                JuneSliderTile(
                    label = stringResource(R.string.slant),
                    valueDisplay = "${slant.toInt()}°",
                    value = slant,
                    valueRange = 0f..15f,
                    enabled = true,
                    onValueChange = {
                        val snapped = round(it)
                        onVariationChange(currentConfig.copy(slant = snapped))
                    }
                )
            }

            if (isShantellSans) {
                JuneSliderTile(
                    label = stringResource(R.string.weight),
                    valueDisplay = "${weight.toInt()}",
                    value = weight,
                    valueRange = 300f..800f,
                    enabled = true,
                    onValueChange = {
                        val snapped = round(it)
                        onVariationChange(currentConfig.copy(weight = snapped))
                    }
                )

                JuneSliderTile(
                    label = stringResource(R.string.bounce),
                    valueDisplay = "${bounce.toInt()}",
                    value = bounce,
                    valueRange = -100f..100f,
                    enabled = true,
                    onValueChange = {
                        val snapped = round(it)
                        onVariationChange(currentConfig.copy(bounce = snapped))
                    }
                )

                JuneSliderTile(
                    label = stringResource(R.string.informality),
                    valueDisplay = "${informality.toInt()}%",
                    value = informality,
                    valueRange = 0f..100f,
                    enabled = true,
                    onValueChange = {
                        val snapped = round(it)
                        onVariationChange(currentConfig.copy(informality = snapped))
                    }
                )

                JuneSliderTile(
                    label = stringResource(R.string.spacing),
                    valueDisplay = "${spacing.toInt()}",
                    value = spacing,
                    valueRange = 0f..100f,
                    enabled = true,
                    onValueChange = {
                        val snapped = round(it)
                        onVariationChange(currentConfig.copy(spacing = snapped))
                    }
                )
            }

            if (isGoogleSansFlex || isRobotoFlex) {
                if (isRobotoFlex) {
                    JuneSliderTile(
                        label = stringResource(R.string.weight),
                        valueDisplay = "${weight.toInt()}",
                        value = weight,
                        valueRange = 100f..1000f,
                        enabled = true,
                        onValueChange = {
                            val snapped = round(it)
                            onVariationChange(currentConfig.copy(weight = snapped))
                        }
                    )
                }

                JuneSliderTile(
                    label = stringResource(R.string.width),
                    valueDisplay = "${width.toInt()}%",
                    value = width,
                    valueRange = 25f..151f,
                    enabled = isVariableFont,
                    onValueChange = {
                        val snapped = round(it)
                        onVariationChange(currentConfig.copy(width = snapped))
                    }
                )

                if (isGoogleSansFlex) {
                    JuneSliderTile(
                        label = stringResource(R.string.roundness),
                        valueDisplay = "${roundness.toInt()}",
                        value = roundness,
                        valueRange = 0f..100f,
                        enabled = isGoogleSansFlex,
                        onValueChange = {
                            val snapped = round(it)
                            onVariationChange(currentConfig.copy(roundness = snapped))
                        }
                    )
                }

                JuneSliderTile(
                    label = stringResource(R.string.grade),
                    valueDisplay = "${grade.toInt()}",
                    value = grade,
                    valueRange = if (isRobotoFlex) -200f..150f else 0f..100f,
                    enabled = isVariableFont,
                    onValueChange = {
                        val snapped = round(it)
                        onVariationChange(currentConfig.copy(grade = snapped))
                    }
                )

                JuneSliderTile(
                    label = stringResource(R.string.optical_size),
                    valueDisplay = "${opticalSize.toInt()} pt",
                    value = opticalSize,
                    valueRange = if (isRobotoFlex) 8f..144f else 6f..144f,
                    enabled = isVariableFont,
                    onValueChange = {
                        val snapped = round(it)
                        onVariationChange(currentConfig.copy(opticalSize = snapped))
                    }
                )

                JuneSliderTile(
                    label = stringResource(R.string.slant),
                    valueDisplay = "${slant.toInt()}°",
                    value = slant,
                    valueRange = 0f..10f,
                    enabled = isVariableFont,
                    onValueChange = {
                        val snapped = round(it)
                        onVariationChange(currentConfig.copy(slant = snapped))
                    }
                )
            }

            if (isRobotoFlex) {
                val chevronRotation by animateFloatAsState(
                    targetValue = if (showParametricAxes) -180f else 0f,
                    label = "Parametric Chevron Rotation"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { showParametricAxes = !showParametricAxes }
                            .padding(start = 16.dp, end = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.parametric_axes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(10.dp))
                        VerticalDivider(
                            modifier = Modifier.height(18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(R.drawable.keyboard_arrow_down_24px),
                            contentDescription = if (showParametricAxes) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { rotationZ = chevronRotation }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showParametricAxes,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        JuneSliderTile(
                            label = stringResource(R.string.counter_width),
                            valueDisplay = "${counterWidth.toInt()}",
                            value = counterWidth,
                            valueRange = 323f..603f,
                            enabled = true,
                            onValueChange = {
                                val snapped = round(it)
                                onVariationChange(currentConfig.copy(counterWidth = snapped))
                            }
                        )

                        JuneSliderTile(
                            label = stringResource(R.string.thick_stroke),
                            valueDisplay = "${thickStroke.toInt()}",
                            value = thickStroke,
                            valueRange = 27f..175f,
                            enabled = true,
                            onValueChange = {
                                val snapped = round(it)
                                onVariationChange(currentConfig.copy(thickStroke = snapped))
                            }
                        )

                        JuneSliderTile(
                            label = stringResource(R.string.thin_stroke),
                            valueDisplay = "${thinStroke.toInt()}",
                            value = thinStroke,
                            valueRange = 25f..135f,
                            enabled = true,
                            onValueChange = {
                                val snapped = round(it)
                                onVariationChange(currentConfig.copy(thinStroke = snapped))
                            }
                        )

                        JuneSliderTile(
                            label = stringResource(R.string.lowercase_height),
                            valueDisplay = "${lowercaseHeight.toInt()}",
                            value = lowercaseHeight,
                            valueRange = 416f..570f,
                            enabled = true,
                            onValueChange = {
                                val snapped = round(it)
                                onVariationChange(currentConfig.copy(lowercaseHeight = snapped))
                            }
                        )

                        JuneSliderTile(
                            label = stringResource(R.string.uppercase_height),
                            valueDisplay = "${uppercaseHeight.toInt()}",
                            value = uppercaseHeight,
                            valueRange = 528f..760f,
                            enabled = true,
                            onValueChange = {
                                val snapped = round(it)
                                onVariationChange(currentConfig.copy(uppercaseHeight = snapped))
                            }
                        )

                        JuneSliderTile(
                            label = stringResource(R.string.ascender_height),
                            valueDisplay = "${ascenderHeight.toInt()}",
                            value = ascenderHeight,
                            valueRange = 649f..854f,
                            enabled = true,
                            onValueChange = {
                                val snapped = round(it)
                                onVariationChange(currentConfig.copy(ascenderHeight = snapped))
                            }
                        )

                        JuneSliderTile(
                            label = stringResource(R.string.descender_depth),
                            valueDisplay = "${descenderDepth.toInt()}",
                            value = descenderDepth,
                            valueRange = -305f..-98f,
                            enabled = true,
                            onValueChange = {
                                val snapped = round(it)
                                onVariationChange(currentConfig.copy(descenderDepth = snapped))
                            }
                        )

                        JuneSliderTile(
                            label = stringResource(R.string.figure_height),
                            valueDisplay = "${figureHeight.toInt()}",
                            value = figureHeight,
                            valueRange = 560f..788f,
                            enabled = true,
                            onValueChange = {
                                val snapped = round(it)
                                onVariationChange(currentConfig.copy(figureHeight = snapped))
                            }
                        )
                    }
                }
            }
        }
    }
}
