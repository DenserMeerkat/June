package com.denser.june.core.domain.model

import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.core.domain.preferences.FontPreferences
import com.denser.june.core.domain.preferences.ThemePreferences
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AppTheme(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val seedColor: Int = -1,
    val withAmoled: Boolean = false,
    val style: PaletteStyle = PaletteStyle.TonalSpot,
    val materialTheme: Boolean = false,
    val appFont: String = "Google Sans Flex",
)

fun ThemePreferences.getAppThemeFlow(fontPrefs: FontPreferences): Flow<AppTheme> {
    return combine(
        getSeedColorFlow(),
        getThemeMode(),
        getAmoledPrefFlow(),
        getPaletteStyle(),
        getMaterialYouFlow()
    ) { seed, themeMode, amoled, style, matYou ->
        AppTheme(
            seedColor = seed,
            themeMode = themeMode,
            withAmoled = amoled,
            style = style,
            materialTheme = matYou
        )
    }.combine(fontPrefs.getAppFont()) { theme, font ->
        theme.copy(appFont = font)
    }
}
