package com.denser.june.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.denser.june.core.domain.model.FontVariationConfig
import com.denser.june.core.domain.model.enums.Fonts
import com.denser.june.core.utils.FontUtils

val SYSTEM_FONT_NAME = FontUtils.SYSTEM_FONT_NAME

fun isSystemFont(fontName: String?): Boolean = FontUtils.isSystemFont(fontName)

@Composable
fun getAppFontFamily(
    fontName: String,
    variation: FontVariationConfig? = null
): FontFamily {
    if (isSystemFont(fontName)) return FontFamily.Default

    val isInternetAllowed = LocalInternetAllowed.current
    return remember(fontName, variation, isInternetAllowed) {
        val bundled = Fonts.entries.find { it.fullName == fontName }
        if (bundled != null || !isInternetAllowed) {
            val activeFont = bundled ?: Fonts.GOOGLE_SANS_FLEX
            createBundledFontFamily(
                activeFont = activeFont,
                variation = variation
            )
        } else {
            createGoogleFontFamily(fontName)
        }
    }
}

@Composable
fun getJournalFontFamily(
    fontName: String?,
    variation: FontVariationConfig? = null
): FontFamily? {
    if (fontName.isNullOrBlank()) return null
    if (isSystemFont(fontName)) return FontFamily.Default

    val isInternetAllowed = LocalInternetAllowed.current
    return remember(fontName, variation, isInternetAllowed) {
        val bundled = Fonts.entries.find { it.fullName == fontName }
        when {
            bundled != null -> createBundledFontFamily(
                activeFont = bundled,
                variation = variation
            )
            isInternetAllowed -> createGoogleFontFamily(fontName)
            else -> null
        }
    }
}

@Composable
fun provideTypography(
    scale: Float = 1f,
    font: String = "Google Sans Flex",
    variation: FontVariationConfig? = null,
): Typography {
    val displayFont = getAppFontFamily(font, variation)
    val bodyFont = getAppFontFamily(font, variation)

    fun style(
        family: FontFamily,
        sizeSp: Double,
        lineHeightSp: Double,
        weight: FontWeight,
        letterSpacingSp: Double = 0.0
    ) = TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = (sizeSp * scale).sp,
        lineHeight = (lineHeightSp * scale).sp,
        letterSpacing = letterSpacingSp.sp
    )

    return Typography(
        displayLarge = style(displayFont, 57.0, 64.0, FontWeight.Bold, -0.25),
        displayMedium = style(displayFont, 45.0, 52.0, FontWeight.Bold),
        displaySmall = style(displayFont, 36.0, 44.0, FontWeight.Bold),

        headlineLarge = style(displayFont, 32.0, 40.0, FontWeight.Bold),
        headlineMedium = style(displayFont, 28.0, 36.0, FontWeight.Bold),
        headlineSmall = style(displayFont, 24.0, 32.0, FontWeight.Bold),

        titleLarge = style(bodyFont, 22.0, 28.0, FontWeight.SemiBold),
        titleMedium = style(bodyFont, 16.0, 24.0, FontWeight.SemiBold, 0.15),
        titleSmall = style(bodyFont, 14.0, 20.0, FontWeight.Medium, 0.1),

        bodyLarge = style(bodyFont, 16.0, 24.0, FontWeight.Normal, 0.5),
        bodyMedium = style(bodyFont, 14.0, 20.0, FontWeight.Normal, 0.25),
        bodySmall = style(bodyFont, 12.0, 16.0, FontWeight.Normal, 0.4),

        labelLarge = style(bodyFont, 14.0, 20.0, FontWeight.SemiBold, 0.1),
        labelMedium = style(bodyFont, 12.0, 16.0, FontWeight.SemiBold, 0.5),
        labelSmall = style(bodyFont, 11.0, 16.0, FontWeight.SemiBold, 0.5)
    )
}