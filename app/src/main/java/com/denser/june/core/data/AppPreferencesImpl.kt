package com.denser.june.core.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.denser.june.core.domain.AppPreferences
import com.denser.june.core.domain.enums.AppTheme
import com.denser.june.core.domain.enums.Fonts
import com.denser.june.core.domain.enums.LockType
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class AppPreferencesImpl(
    private val dataStore: DataStore<Preferences>
) : AppPreferences {

    override suspend fun resetAppTheme() {
        dataStore.edit { preferences ->
            preferences[seedColor] = Color.White.toArgb()
            preferences[amoledPref] = false
            preferences[paletteStyle] = PaletteStyle.TonalSpot.name
            preferences[materialTheme] = false
            preferences[selectedFont] = Fonts.FIGTREE.name
        }
    }

    companion object {
        private val seedColor = intPreferencesKey("seed_color")
        private val appTheme = stringPreferencesKey("app_theme")
        private val amoledPref = booleanPreferencesKey("with_amoled")
        private val paletteStyle = stringPreferencesKey("palette_style")
        private val materialTheme = booleanPreferencesKey("material_theme")
        private val onboardingDone = booleanPreferencesKey("onboarding_done")
        private val selectedFont = stringPreferencesKey("font")
        private val appLock = booleanPreferencesKey("app_lock")
        private val lockType = stringPreferencesKey("lock_type")
        private val pinHash = stringPreferencesKey("pin_hash")
    }

    override fun getAppThemePrefFlow(): Flow<AppTheme> = dataStore.data
        .map { preferences ->
            val theme = preferences[appTheme] ?: AppTheme.SYSTEM.name
            AppTheme.valueOf(theme)
        }

    override suspend fun updateAppThemePref(pref: AppTheme) {
        dataStore.edit {
            it[appTheme] = pref.name
        }
    }

    override fun getSeedColorFlow(): Flow<Int> = dataStore.data
        .map { preferences -> preferences[seedColor] ?: Color.White.toArgb() }

    override suspend fun updateSeedColor(newCardContent: Int) {
        dataStore.edit { settings ->
            settings[seedColor] = newCardContent
        }
    }

    override fun getAmoledPrefFlow(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[amoledPref] == true }

    override suspend fun updateAmoledPref(amoled: Boolean) {
        dataStore.edit { settings ->
            settings[amoledPref] = amoled
        }
    }

    override fun getPaletteStyle(): Flow<PaletteStyle> = dataStore.data
        .map { preferences ->
            PaletteStyle.valueOf(preferences[paletteStyle] ?: PaletteStyle.TonalSpot.name)
        }

    override suspend fun updatePaletteStyle(style: PaletteStyle) {
        dataStore.edit { settings ->
            settings[paletteStyle] = style.name
        }
    }

    override fun getMaterialYouFlow(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[materialTheme] == true }

    override suspend fun updateMaterialTheme(pref: Boolean) {
        dataStore.edit { settings ->
            settings[materialTheme] = pref
        }
    }

    override fun getFontFlow(): Flow<Fonts> = dataStore.data
        .map { prefs ->
            val font = prefs[selectedFont] ?: Fonts.FIGTREE.name
            Fonts.valueOf(font)
        }

    override suspend fun updateFont(font: Fonts) {
        dataStore.edit { settings ->
            settings[selectedFont] = font.name
        }
    }

    override fun getOnboardingDoneFlow(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[onboardingDone] == true }

    override suspend fun updateOnboardingDone(done: Boolean) {
        dataStore.edit { settings ->
            settings[onboardingDone] = done
        }
    }

    override fun getAppLockFlow(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[appLock] == true }

    override suspend fun updateAppLock(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[appLock] = enabled
        }
    }

    override fun getLockTypeFlow(): Flow<LockType> = dataStore.data
        .map { preferences ->
            val typeName = preferences[lockType] ?: LockType.BIOMETRIC.name
            LockType.valueOf(typeName)
        }

    override suspend fun updateLockType(type: LockType) {
        dataStore.edit { it[lockType] = type.name }
    }

    override fun getPinHashFlow(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[pinHash] }

    override suspend fun updatePinHash(hash: String?) {
        dataStore.edit {
            if (hash == null) it.remove(pinHash) else it[pinHash] = hash
        }
    }
}