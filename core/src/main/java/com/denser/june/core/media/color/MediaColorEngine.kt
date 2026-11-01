package com.denser.june.core.media.color

import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.denser.june.core.domain.model.JournalTheme

object MediaColorEngine {

    fun extractCardColors(
        theme: JournalTheme?,
        palette: Palette?
    ): ExtractedCardColors {
        val isThemeValidSeed = theme != null && run {
            val hsl = floatArrayOf(0f, 0f, 0f)
            ColorUtils.colorToHSL(theme.primaryColor, hsl)
            hsl[1] > 0.05f && hsl[2] < 0.94f
        }

        val effectiveTheme = if (theme != null && isThemeValidSeed) {
            theme
        } else if (palette != null) {
            extractThemes(palette, "Song Art").firstOrNull()?.theme
        } else {
            null
        }

        if (effectiveTheme != null) {
            val primaryHsl = floatArrayOf(0f, 0f, 0f)
            ColorUtils.colorToHSL(effectiveTheme.primaryColor, primaryHsl)

            primaryHsl[1] = primaryHsl[1].coerceAtLeast(0.80f)
            primaryHsl[2] = 0.65f
            val primary = ColorUtils.HSLToColor(primaryHsl)

            val neutralColor = effectiveTheme.neutralColor
            val surface = if (neutralColor != null) {
                val neutralHsl = floatArrayOf(0f, 0f, 0f)
                ColorUtils.colorToHSL(neutralColor, neutralHsl)
                neutralHsl[2] = 0.12f
                neutralHsl[1] = (neutralHsl[1] * 0.5f).coerceIn(0.04f, 0.20f)
                ColorUtils.HSLToColor(neutralHsl)
            } else {
                val derivedSurfaceHsl = primaryHsl.copyOf()
                derivedSurfaceHsl[2] = 0.12f
                derivedSurfaceHsl[1] = (derivedSurfaceHsl[1] * 0.35f).coerceIn(0.04f, 0.18f)
                ColorUtils.HSLToColor(derivedSurfaceHsl)
            }

            val onSurface = android.graphics.Color.WHITE
            val onSurfaceVariant = ColorUtils.setAlphaComponent(android.graphics.Color.WHITE, (0.7f * 255).toInt())

            val onBtnHsl = primaryHsl.copyOf()
            onBtnHsl[1] = (onBtnHsl[1] * 0.8f).coerceAtMost(0.5f)
            onBtnHsl[2] = 0.15f
            val onPrimaryContainer = ColorUtils.HSLToColor(onBtnHsl)

            val secondaryContainer = ColorUtils.setAlphaComponent(android.graphics.Color.WHITE, (0.15f * 255).toInt())

            return ExtractedCardColors(
                surface = surface,
                onSurface = onSurface,
                onSurfaceVariant = onSurfaceVariant,
                primaryContainer = primary,
                onPrimaryContainer = onPrimaryContainer,
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = onSurface
            )
        }

        return ExtractedCardColors(
            surface = 0xFF1B1F23.toInt(),
            onSurface = android.graphics.Color.WHITE,
            onSurfaceVariant = ColorUtils.setAlphaComponent(android.graphics.Color.WHITE, (0.7f * 255).toInt()),
            primaryContainer = 0xFF5AC8F2.toInt(),
            onPrimaryContainer = 0xFF132F39.toInt(),
            secondaryContainer = ColorUtils.setAlphaComponent(android.graphics.Color.WHITE, (0.15f * 255).toInt()),
            onSecondaryContainer = android.graphics.Color.WHITE
        )
    }

    fun extractSeedColors(palette: Palette): List<Int> {
        val swatches = palette.swatches.sortedByDescending { it.population }
        val colors = mutableListOf<Int>()
        val seen = mutableSetOf<Int>()

        val primaryCandidates = listOfNotNull(
            palette.vibrantSwatch,
            palette.lightVibrantSwatch,
            palette.darkVibrantSwatch,
            palette.dominantSwatch,
            palette.mutedSwatch
        )

        for (swatch in primaryCandidates) {
            val color = swatch.rgb
            if (seen.none { isTooSimilarHue(it, color, 16f) }) {
                seen.add(color)
                colors.add(color)
            }
        }

        for (swatch in swatches) {
            val color = swatch.rgb
            if (seen.none { isTooSimilarHue(it, color, 16f) }) {
                seen.add(color)
                colors.add(color)
            }
        }

        return colors
    }

    fun extractThemes(
        palette: Palette,
        sourceLabel: String,
        mediaUri: Any? = null
    ): List<ExtractedMediaTheme> {
        val allSwatches = palette.swatches.sortedByDescending { it.population }
        if (allSwatches.isEmpty()) return emptyList()

        val dominant = palette.dominantSwatch ?: allSwatches.first()

        val backgroundNeutral = run {
            val hsl = floatArrayOf(0f, 0f, 0f)
            ColorUtils.colorToHSL(dominant.rgb, hsl)
            hsl[1] = (hsl[1] * 0.65f).coerceIn(0.12f, 0.40f)
            hsl[2] = 0.12f
            ColorUtils.HSLToColor(hsl)
        }

        val vibrant = palette.vibrantSwatch
        val lightVibrant = palette.lightVibrantSwatch
        val darkVibrant = palette.darkVibrantSwatch
        val muted = palette.mutedSwatch
        val darkMuted = palette.darkMutedSwatch
        val lightMuted = palette.lightMutedSwatch

        val foregroundAccents = allSwatches.filter { it.rgb != dominant.rgb }

        val mainForeground = vibrant
            ?: lightVibrant
            ?: darkVibrant
            ?: foregroundAccents.firstOrNull { !isTooSimilarHue(it.rgb, dominant.rgb, 20f) }
            ?: muted
            ?: dominant

        val list = mutableListOf<ExtractedMediaTheme>()
        val seenSignatures = mutableSetOf<String>()

        data class VariantBlueprint(
            val label: String,
            val primary: Palette.Swatch,
            val secondary: Palette.Swatch?,
            val tertiary: Palette.Swatch?,
            val neutral: Int
        )

        val blueprints = mutableListOf<VariantBlueprint>()

        blueprints.add(
            VariantBlueprint(
                label = "Cover Cohesion",
                primary = mainForeground,
                secondary = (lightVibrant ?: muted ?: foregroundAccents.getOrNull(1))?.takeIf { it.rgb != mainForeground.rgb },
                tertiary = (darkVibrant ?: darkMuted ?: foregroundAccents.getOrNull(2))?.takeIf { it.rgb != mainForeground.rgb },
                neutral = backgroundNeutral
            )
        )

        if (dominant.rgb != mainForeground.rgb) {
            blueprints.add(
                VariantBlueprint(
                    label = "Dominant Wash",
                    primary = dominant,
                    secondary = mainForeground,
                    tertiary = (vibrant ?: lightVibrant ?: muted)?.takeIf { it.rgb != dominant.rgb && it.rgb != mainForeground.rgb },
                    neutral = backgroundNeutral
                )
            )
        }

        val secondaryAccent = foregroundAccents.firstOrNull {
            it.rgb != mainForeground.rgb && !isTooSimilarHue(it.rgb, mainForeground.rgb, 20f)
        } ?: vibrant ?: lightVibrant

        if (secondaryAccent != null && secondaryAccent.rgb != mainForeground.rgb) {
            blueprints.add(
                VariantBlueprint(
                    label = "Vibrant Detail",
                    primary = secondaryAccent,
                    secondary = mainForeground,
                    tertiary = (darkMuted ?: lightMuted ?: foregroundAccents.getOrNull(2))?.takeIf { it.rgb != secondaryAccent.rgb },
                    neutral = backgroundNeutral
                )
            )
        }

        val highChromaAccent = allSwatches.maxByOrNull { getSaturation(it.rgb) }
        if (highChromaAccent != null && highChromaAccent.rgb != mainForeground.rgb && highChromaAccent.rgb != secondaryAccent?.rgb) {
            blueprints.add(
                VariantBlueprint(
                    label = "Pop Accent",
                    primary = highChromaAccent,
                    secondary = mainForeground,
                    tertiary = secondaryAccent,
                    neutral = backgroundNeutral
                )
            )
        }

        val mutedAccent = muted ?: lightMuted ?: darkMuted
        if (mutedAccent != null && mutedAccent.rgb != mainForeground.rgb && mutedAccent.rgb != dominant.rgb) {
            blueprints.add(
                VariantBlueprint(
                    label = "Muted Harmony",
                    primary = mutedAccent,
                    secondary = mainForeground,
                    tertiary = dominant,
                    neutral = backgroundNeutral
                )
            )
        }

        for (bp in blueprints) {
            val primaryColor = tunePixelAccent(bp.primary.rgb)
            val secondaryColor = bp.secondary?.rgb?.let { tunePixelAccent(it, minSat = 0.55f, targetLight = 0.60f) }?.takeIf {
                !isTooSimilarHue(primaryColor, it, 18f)
            }
            val tertiaryColor = bp.tertiary?.rgb?.let { tunePixelAccent(it, minSat = 0.45f, targetLight = 0.55f) }?.takeIf {
                it != primaryColor &&
                (secondaryColor == null || !isTooSimilarHue(secondaryColor, it, 18f)) &&
                !isTooSimilarHue(primaryColor, it, 18f)
            }

            val signature = "$primaryColor-$secondaryColor-$tertiaryColor-${bp.neutral}"
            if (seenSignatures.add(signature)) {
                list.add(
                    ExtractedMediaTheme(
                        theme = JournalTheme(
                            primaryColor = primaryColor,
                            style = "Content",
                            secondaryColor = secondaryColor,
                            tertiaryColor = tertiaryColor,
                            neutralColor = bp.neutral
                        ),
                        sourceLabel = sourceLabel,
                        variantLabel = bp.label,
                        mediaUri = mediaUri
                    )
                )
            }
        }

        return list
    }

    private fun tunePixelAccent(
        argb: Int,
        minSat: Float = 0.80f,
        targetLight: Float = 0.65f
    ): Int {
        val hsl = floatArrayOf(0f, 0f, 0f)
        ColorUtils.colorToHSL(argb, hsl)
        hsl[1] = hsl[1].coerceAtLeast(minSat)
        hsl[2] = targetLight
        return ColorUtils.HSLToColor(hsl)
    }

    private fun getSaturation(argb: Int): Float {
        val hsl = floatArrayOf(0f, 0f, 0f)
        ColorUtils.colorToHSL(argb, hsl)
        return hsl[1]
    }

    private fun boostSaturation(argb: Int, minSaturation: Float): Int {
        val hsl = floatArrayOf(0f, 0f, 0f)
        ColorUtils.colorToHSL(argb, hsl)
        return if (hsl[1] < minSaturation) {
            hsl[1] = minSaturation
            ColorUtils.HSLToColor(hsl)
        } else {
            argb
        }
    }

    private fun capSaturation(argb: Int, maxSaturation: Float): Int {
        val hsl = floatArrayOf(0f, 0f, 0f)
        ColorUtils.colorToHSL(argb, hsl)
        return if (hsl[1] > maxSaturation) {
            hsl[1] = maxSaturation
            ColorUtils.HSLToColor(hsl)
        } else {
            argb
        }
    }

    private fun isTooSimilarHue(colorA: Int, colorB: Int, thresholdDeg: Float): Boolean {
        val hslA = floatArrayOf(0f, 0f, 0f)
        val hslB = floatArrayOf(0f, 0f, 0f)
        ColorUtils.colorToHSL(colorA, hslA)
        ColorUtils.colorToHSL(colorB, hslB)
        val diff = Math.abs(hslA[0] - hslB[0])
        val hueDiff = if (diff > 180f) 360f - diff else diff
        return hueDiff < thresholdDeg
    }
}
