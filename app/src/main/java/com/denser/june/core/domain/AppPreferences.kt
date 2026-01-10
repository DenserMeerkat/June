package com.denser.june.core.domain

import com.denser.june.core.domain.enums.AppTheme
import com.denser.june.core.domain.enums.Fonts
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.flow.Flow

interface AppPreferences {
    suspend fun resetAppTheme()

    fun getAppThemePrefFlow(): Flow<AppTheme>
    suspend fun updateAppThemePref(pref: AppTheme)

    fun getSeedColorFlow(): Flow<Int>
    suspend fun updateSeedColor(newCardContent: Int)

    fun getAmoledPrefFlow(): Flow<Boolean>
    suspend fun updateAmoledPref(amoled: Boolean)

    fun getPaletteStyle(): Flow<PaletteStyle>
    suspend fun updatePaletteStyle(style: PaletteStyle)

    fun getMaterialYouFlow(): Flow<Boolean>
    suspend fun updateMaterialTheme(pref: Boolean)

    fun getFontFlow(): Flow<Fonts>
    suspend fun updateFont(font: Fonts)

    fun getOnboardingDoneFlow(): Flow<Boolean>
    suspend fun updateOnboardingDone(done: Boolean)

    fun getAppLockFlow(): Flow<Boolean>
    suspend fun updateAppLock(enabled: Boolean)
}