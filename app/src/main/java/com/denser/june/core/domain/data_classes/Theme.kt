package com.denser.june.core.domain.data_classes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.denser.june.core.domain.enums.AppTheme
import com.denser.june.core.domain.enums.Fonts
import com.materialkolor.PaletteStyle

data class Theme(
    val seedColor: Int = Color.White.toArgb(),
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val withAmoled: Boolean = false,
    val style: PaletteStyle = PaletteStyle.Neutral,
    val materialTheme: Boolean = false,
    val font: Fonts = Fonts.FIGTREE
)