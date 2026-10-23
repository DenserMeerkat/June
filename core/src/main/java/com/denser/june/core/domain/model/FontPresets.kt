package com.denser.june.core.domain.model

import com.denser.june.core.domain.model.enums.Fonts
import com.denser.june.core.utils.FontUtils

data class FontPresetItem(
    val label: String,
    val config: FontVariationConfig
)

object FontPresets {
    fun getDefaultVariation(fontName: String?): FontVariationConfig {
        return when (fontName) {
            Fonts.GOOGLE_SANS_FLEX.fullName ->
                FontVariationConfig(width = 100f, roundness = 100f, grade = 0f, opticalSize = 18f, slant = 0f)
            Fonts.ROBOTO_FLEX.fullName ->
                FontVariationConfig(weight = 400f, width = 100f, opticalSize = 14f, grade = 0f, slant = 0f)
            Fonts.RECURSIVE.fullName ->
                FontVariationConfig(weight = 400f, slant = 0f, monospace = 0f, casual = 0f, cursive = 0.5f)
            Fonts.SHANTELL_SANS.fullName ->
                FontVariationConfig(weight = 400f, bounce = 0f, informality = 0f, spacing = 0f)
            else -> FontVariationConfig.DEFAULT
        }
    }

    val googleSansPresets = listOf(
        FontPresetItem(
            "Default",
            FontVariationConfig(width = 100f, roundness = 100f, grade = 0f, opticalSize = 18f, slant = 0f)
        ),
        FontPresetItem(
            "Zenith",
            FontVariationConfig(width = 115f, roundness = 40f, grade = 50f, opticalSize = 18f, slant = 0f)
        ),
        FontPresetItem(
            "Classic",
            FontVariationConfig(width = 100f, roundness = 0f, grade = 0f, opticalSize = 18f, slant = 0f)
        ),
        FontPresetItem(
            "Airy",
            FontVariationConfig(width = 120f, roundness = 30f, grade = -20f, opticalSize = 18f, slant = 0f)
        )
    )

    val robotoFlexPresets = listOf(
        FontPresetItem(
            "Default",
            FontVariationConfig(weight = 400f, width = 100f, opticalSize = 14f, grade = 0f, slant = 0f)
        ),
        FontPresetItem(
            "Editorial",
            FontVariationConfig(weight = 375f, width = 108f, opticalSize = 24f, grade = -15f, slant = 0f)
        ),
        FontPresetItem(
            "Novel",
            FontVariationConfig(weight = 425f, width = 96f, opticalSize = 12f, grade = 35f, slant = 0f)
        ),
        FontPresetItem(
            "Dispatch",
            FontVariationConfig(weight = 450f, width = 88f, opticalSize = 14f, grade = 10f, slant = 0f)
        ),
        FontPresetItem(
            "Cadence",
            FontVariationConfig(weight = 400f, width = 102f, opticalSize = 16f, grade = 0f, slant = 7f)
        )
    )

    val recursivePresets = listOf(
        FontPresetItem(
            "Default",
            FontVariationConfig(weight = 400f, slant = 0f, monospace = 0f, casual = 0f, cursive = 0.5f)
        ),
        FontPresetItem(
            "Casual",
            FontVariationConfig(weight = 420f, slant = 0f, monospace = 0f, casual = 1f, cursive = 0.5f)
        ),
        FontPresetItem(
            "Mono",
            FontVariationConfig(weight = 400f, slant = 0f, monospace = 1f, casual = 0f, cursive = 0f)
        ),
        FontPresetItem(
            "Retro",
            FontVariationConfig(weight = 420f, slant = 0f, monospace = 0.6f, casual = 0.7f, cursive = 0.5f)
        ),
        FontPresetItem(
            "Script",
            FontVariationConfig(weight = 400f, slant = 8f, monospace = 0f, casual = 0.8f, cursive = 1f)
        )
    )

    val shantellSansPresets = listOf(
        FontPresetItem(
            "Default",
            FontVariationConfig(weight = 400f, bounce = 0f, informality = 0f, spacing = 0f)
        ),
        FontPresetItem(
            "Bouncy",
            FontVariationConfig(weight = 400f, bounce = 50f, informality = 30f, spacing = 0f)
        ),
        FontPresetItem(
            "Loose",
            FontVariationConfig(weight = 400f, bounce = 20f, informality = 100f, spacing = 20f)
        ),
        FontPresetItem(
            "Neat",
            FontVariationConfig(weight = 450f, bounce = 0f, informality = 0f, spacing = 0f)
        )
    )

    fun getPresetsForFont(fontName: String?): List<FontPresetItem> = when {
        FontUtils.isGoogleSansFlex(fontName) -> googleSansPresets
        FontUtils.isRobotoFlex(fontName) -> robotoFlexPresets
        FontUtils.isRecursive(fontName) -> recursivePresets
        FontUtils.isShantellSans(fontName) -> shantellSansPresets
        else -> emptyList()
    }
}
