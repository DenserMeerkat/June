package com.denser.june.presentation.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

internal const val PALETTE_BITMAP_SIZE = 300
internal const val PALETTE_MAX_COLORS = 24

internal suspend fun loadBitmapForPalette(
    context: Context,
    loader: ImageLoader,
    data: Any
): Bitmap? {
    val request = ImageRequest.Builder(context)
        .data(data)
        .size(PALETTE_BITMAP_SIZE)
        .allowHardware(false)
        .build()
    return when (val result = loader.execute(request)) {
        is SuccessResult -> (result.drawable as? BitmapDrawable)?.bitmap
        else -> null
    }
}

internal fun buildPalette(bitmap: Bitmap): Palette =
    Palette.from(bitmap)
        .maximumColorCount(PALETTE_MAX_COLORS)
        .generate()
