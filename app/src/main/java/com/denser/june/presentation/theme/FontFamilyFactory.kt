package com.denser.june.presentation.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.denser.june.core.domain.model.FontVariationConfig
import com.denser.june.core.domain.model.enums.Fonts
import kotlin.math.abs

@OptIn(ExperimentalTextApi::class)
fun buildVariationSettings(
    baseWeight: Int,
    variation: FontVariationConfig?,
    activeFont: Fonts
): FontVariation.Settings {
    val list = mutableListOf<FontVariation.Setting>()

    val targetWeight = variation?.weight?.toInt() ?: baseWeight
    val weightRange = when (activeFont) {
        Fonts.SHANTELL_SANS -> 300..800
        Fonts.RECURSIVE -> 300..1000
        Fonts.ROBOTO_FLEX -> 100..1000
        Fonts.GOOGLE_SANS_FLEX -> 1..1000
    }
    list.add(FontVariation.weight(targetWeight.coerceIn(weightRange)))

    when (activeFont) {
        Fonts.GOOGLE_SANS_FLEX -> {
            variation?.width?.let { list.add(FontVariation.width(it.coerceIn(25f, 151f))) }
            variation?.opticalSize?.let { list.add(FontVariation.Setting("opsz", it.coerceIn(6f, 144f))) }
            variation?.slant?.let { list.add(FontVariation.Setting("slnt", (-abs(it)).coerceIn(-10f, 0f))) }
            variation?.grade?.let { list.add(FontVariation.Setting("GRAD", it.coerceIn(0f, 100f))) }
            variation?.roundness?.let { list.add(FontVariation.Setting("ROND", it.coerceIn(0f, 100f))) }
        }
        Fonts.ROBOTO_FLEX -> {
            variation?.width?.let { list.add(FontVariation.width(it.coerceIn(25f, 151f))) }
            variation?.opticalSize?.let { list.add(FontVariation.Setting("opsz", it.coerceIn(8f, 144f))) }
            variation?.slant?.let { list.add(FontVariation.Setting("slnt", (-abs(it)).coerceIn(-10f, 0f))) }
            variation?.grade?.let { list.add(FontVariation.Setting("GRAD", it.coerceIn(-200f, 150f))) }
            variation?.counterWidth?.let { list.add(FontVariation.Setting("XTRA", it.coerceIn(323f, 603f))) }
            variation?.thickStroke?.let { list.add(FontVariation.Setting("XOPQ", it.coerceIn(27f, 175f))) }
            variation?.thinStroke?.let { list.add(FontVariation.Setting("YOPQ", it.coerceIn(25f, 135f))) }
            variation?.uppercaseHeight?.let { list.add(FontVariation.Setting("YTUC", it.coerceIn(528f, 760f))) }
            variation?.lowercaseHeight?.let { list.add(FontVariation.Setting("YTLC", it.coerceIn(416f, 570f))) }
            variation?.ascenderHeight?.let { list.add(FontVariation.Setting("YTAS", it.coerceIn(649f, 854f))) }
            variation?.descenderDepth?.let { list.add(FontVariation.Setting("YTDE", it.coerceIn(-305f, -98f))) }
            variation?.figureHeight?.let { list.add(FontVariation.Setting("YTFI", it.coerceIn(560f, 788f))) }
        }
        Fonts.RECURSIVE -> {
            variation?.slant?.let { list.add(FontVariation.Setting("slnt", (-abs(it)).coerceIn(-15f, 0f))) }
            variation?.monospace?.let { list.add(FontVariation.Setting("MONO", it.coerceIn(0f, 1f))) }
            variation?.casual?.let { list.add(FontVariation.Setting("CASL", it.coerceIn(0f, 1f))) }
            variation?.cursive?.let { list.add(FontVariation.Setting("CRSV", it.coerceIn(0f, 1f))) }
        }
        Fonts.SHANTELL_SANS -> {
            variation?.bounce?.let { list.add(FontVariation.Setting("BNCE", it.coerceIn(-100f, 100f))) }
            variation?.informality?.let { list.add(FontVariation.Setting("INFM", it.coerceIn(0f, 100f))) }
            variation?.spacing?.let { list.add(FontVariation.Setting("SPAC", it.coerceIn(0f, 100f))) }
        }
    }

    variation?.italic?.let { list.add(FontVariation.italic(it.coerceIn(0f, 1f))) }

    return FontVariation.Settings(*list.toTypedArray())
}

@OptIn(ExperimentalTextApi::class)
fun createBundledFontFamily(
    activeFont: Fonts,
    variation: FontVariationConfig?
): FontFamily {
    val customWeight = variation?.weight
    return if (customWeight != null) {
        val w = customWeight.toInt().coerceIn(1, 1000)
        FontFamily(
            Font(
                resId = activeFont.font,
                weight = FontWeight(w),
                variationSettings = buildVariationSettings(w, variation, activeFont)
            )
        )
    } else {
        FontFamily(
            Font(activeFont.font, FontWeight.Normal, variationSettings = buildVariationSettings(400, variation, activeFont)),
            Font(activeFont.font, FontWeight.Medium, variationSettings = buildVariationSettings(500, variation, activeFont)),
            Font(activeFont.font, FontWeight.SemiBold, variationSettings = buildVariationSettings(600, variation, activeFont)),
            Font(activeFont.font, FontWeight.Bold, variationSettings = buildVariationSettings(700, variation, activeFont))
        )
    }
}

fun createGoogleFontFamily(fontName: String): FontFamily {
    val googleFont = GoogleFont(fontName.trim())
    return FontFamily(
        Font(googleFont = googleFont, fontProvider = provider, weight = FontWeight.Normal),
        Font(googleFont = googleFont, fontProvider = provider, weight = FontWeight.Medium),
        Font(googleFont = googleFont, fontProvider = provider, weight = FontWeight.SemiBold),
        Font(googleFont = googleFont, fontProvider = provider, weight = FontWeight.Bold)
    )
}
