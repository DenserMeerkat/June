package com.denser.june.presentation.utils

import android.content.Context
import coil.ImageLoader
import com.denser.june.core.media.color.ExtractedMediaColor
import com.denser.june.core.media.color.ExtractedMediaTheme
import com.denser.june.core.media.color.MediaColorEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

typealias ExtractedMediaColor = ExtractedMediaColor
typealias ExtractedMediaTheme = ExtractedMediaTheme

object MediaColorExtractor {

    suspend fun extractColors(
        context: Context,
        imagePaths: List<String>,
        songThumbnailPath: String? = null
    ): List<ExtractedMediaColor> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ExtractedMediaColor>()
        val seenColors = mutableSetOf<Int>()
        val loader = ImageLoader(context)

        songThumbnailPath?.let { songPath ->
            extractColorsFromSource(context, loader, songPath)?.forEach { color ->
                if (seenColors.add(color)) {
                    results.add(ExtractedMediaColor(color, "Song Art", songPath))
                }
            }
        }

        imagePaths.forEachIndexed { index, path ->
            val label = if (imagePaths.size == 1) "Photo" else "Photo ${index + 1}"
            extractColorsFromSource(context, loader, path)?.forEach { color ->
                if (seenColors.add(color)) {
                    results.add(ExtractedMediaColor(color, label, path))
                }
            }
        }

        results
    }

    suspend fun extractThemes(
        context: Context,
        imagePaths: List<String>,
        songThumbnailPath: String? = null
    ): List<ExtractedMediaTheme> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ExtractedMediaTheme>()
        val loader = ImageLoader(context)

        songThumbnailPath?.let { songPath ->
            extractThemesFromSource(context, loader, songPath, "Song Art").let { themes ->
                results.addAll(themes)
            }
        }

        imagePaths.forEachIndexed { index, path ->
            val label = if (imagePaths.size == 1) "Photo" else "Photo ${index + 1}"
            extractThemesFromSource(context, loader, path, label).let { themes ->
                results.addAll(themes)
            }
        }

        results
    }

    private suspend fun extractColorsFromSource(
        context: Context,
        loader: ImageLoader,
        data: Any
    ): List<Int>? {
        return try {
            val bitmap = loadBitmapForPalette(context, loader, data) ?: return null
            val palette = buildPalette(bitmap)
            MediaColorEngine.extractSeedColors(palette)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun extractThemesFromSource(
        context: Context,
        loader: ImageLoader,
        data: Any,
        sourceLabel: String
    ): List<ExtractedMediaTheme> {
        return try {
            val bitmap = loadBitmapForPalette(context, loader, data) ?: return emptyList()
            val palette = buildPalette(bitmap)
            MediaColorEngine.extractThemes(palette, sourceLabel, data)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
