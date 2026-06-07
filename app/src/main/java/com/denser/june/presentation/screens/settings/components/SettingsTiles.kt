package com.denser.june.presentation.screens.settings.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.core.domain.model.enums.ThemeMode
import com.denser.june.presentation.screens.settings.SettingsState
import com.denser.june.presentation.screens.settings.tiles.*
import java.time.format.TextStyle
import com.denser.june.core.domain.model.enums.TimeFormat

data class SettingsTriggers(
    val onDeleteAllJournals: () -> Unit = {},
    val onColorPickerClick: () -> Unit = {},
    val onLicenseClick: () -> Unit = {},
    val onMapAttributionsClick: () -> Unit = {},
    val onAboutLibrariesClick: () -> Unit = {},
    val onChangelogClick: () -> Unit = {},
    val onCheckForUpdatesClick: () -> Unit = {},
    val onAboutHeaderClick: () -> Unit = {}
)

val LocalSettingsTriggers = staticCompositionLocalOf { SettingsTriggers() }

data class SettingTile(
    val key: String,
    val title: String,
    val subtitle: (Context, SettingsState) -> String?,
    val category: String,
    val keywords: List<String> = emptyList(),
    val content: @Composable () -> Unit
)

object SettingsTileRegistry {
    @Composable
    fun getTiles(): List<SettingTile> {
        val appThemeTitle = stringResource(R.string.app_theme)
        val amoledTitle = stringResource(R.string.amoled)
        val amoledDesc = stringResource(R.string.amoled_desc)
        val materialThemeTitle = stringResource(R.string.material_theme)
        val materialThemeDesc = stringResource(R.string.material_theme_desc)
        val seedColorTitle = stringResource(R.string.seed_color)
        val seedColorDesc = stringResource(R.string.seed_color_desc)
        val aboutLibrariesTitle = stringResource(R.string.about_libraries)

        return remember(
            appThemeTitle, amoledTitle, amoledDesc, materialThemeTitle,
            materialThemeDesc, seedColorTitle, seedColorDesc, aboutLibrariesTitle
        ) {
            listOf(
                SettingTile(
                    key = "REMINDERS",
                    title = "Reminders",
                    subtitle = { _, _ -> "Set journaling reminders" },
                    category = "General",
                    keywords = listOf("reminder", "notification", "schedule", "alert"),
                    content = { RemindersTile() }
                ),
                SettingTile(
                    key = "INCLUDE_TIME",
                    title = "Include time",
                    subtitle = { _, _ -> "Always enable time when creating journals" },
                    category = "General",
                    keywords = listOf("time", "include", "journal", "auto"),
                    content = { IncludeTimeTile() }
                ),
                SettingTile(
                    key = "TIME_FORMAT",
                    title = "Time format",
                    subtitle = { _, state -> if (state.timeFormat == TimeFormat.TWELVE_HOUR) "12-Hour" else "24-Hour" },
                    category = "General",
                    keywords = listOf("time", "clock", "hour", "format", "12", "24"),
                    content = { TimeFormatTile() }
                ),
                SettingTile(
                    key = "MAP_SETTINGS",
                    title = "Map settings",
                    subtitle = { _, _ -> "Select style and manage API keys" },
                    category = "General",
                    keywords = listOf("map", "maptiler", "stadia", "mapbox", "key", "style", "settings", "maptiler", "mapbox", "stadia", "carto"),
                    content = { MapSettingsTile() }
                ),
                SettingTile(
                    key = "MARKDOWN_EDITOR",
                    title = "Markdown editor",
                    subtitle = { _, state -> if (state.isMarkdownEnabled) "Rich text formatting enabled" else "Plain text mode" },
                    category = "General",
                    keywords = listOf("markdown", "editor", "rich", "text", "format", "plain"),
                    content = { MarkdownEditorTile() }
                ),
                SettingTile(
                    key = "START_OF_WEEK",
                    title = "Start of the week",
                    subtitle = { _, state -> state.startOfWeek.getDisplayName(TextStyle.FULL, java.util.Locale.getDefault()) },
                    category = "General",
                    keywords = listOf("start", "week", "day", "calendar", "sunday", "monday"),
                    content = { StartOfWeekTile() }
                ),
                SettingTile(
                    key = "DELETE_ALL_JOURNALS",
                    title = "Delete all journals",
                    subtitle = { _, _ -> "Move all entries to Bin" },
                    category = "General",
                    keywords = listOf("delete", "remove", "erase", "all", "journals", "clear"),
                    content = { DeleteAllJournalsTile() }
                ),
                SettingTile(
                    key = "APP_THEME",
                    title = appThemeTitle,
                    subtitle = { context, state -> context.getString(state.appTheme.themeMode.stringRes) },
                    category = "Appearance",
                    keywords = listOf("theme", "dark", "light", "mode", "amoled", "color"),
                    content = { AppThemeTile() }
                ),
                SettingTile(
                    key = "APP_FONT",
                    title = "App Font",
                    subtitle = { _, state -> state.appTheme.appFont },
                    category = "Appearance",
                    keywords = listOf("font", "typography", "text", "style", "size"),
                    content = { AppFontTile() }
                ),
                SettingTile(
                    key = "AMOLED",
                    title = amoledTitle,
                    subtitle = { _, _ -> amoledDesc },
                    category = "Appearance",
                    keywords = listOf("amoled", "black", "dark", "oled", "battery"),
                    content = { AmoledTile() }
                ),
                SettingTile(
                    key = "MATERIAL_THEME",
                    title = materialThemeTitle,
                    subtitle = { _, _ -> materialThemeDesc },
                    category = "Appearance",
                    keywords = listOf("material", "you", "dynamic", "color", "wallpaper"),
                    content = { MaterialThemeTile() }
                ),
                SettingTile(
                    key = "SEED_COLOR",
                    title = seedColorTitle,
                    subtitle = { _, _ -> seedColorDesc },
                    category = "Appearance",
                    keywords = listOf("seed", "color", "picker", "accent", "custom"),
                    content = { SeedColorTile() }
                ),
                SettingTile(
                    key = "PALETTE_SELECTION",
                    title = "Palette Selection",
                    subtitle = { _, _ -> "Choose theme variant palette" },
                    category = "Appearance",
                    keywords = listOf("palette", "style", "theme", "tonal", "scheme"),
                    content = { PaletteSelectionSettingsItem() }
                ),
                SettingTile(
                    key = "APP_LOCK",
                    title = "App Lock",
                    subtitle = { _, state ->
                        if (state.isAppLockEnabled) {
                            if (state.lockType == LockType.PIN) "Custom PIN" else "Same as screen lock"
                        } else {
                            "No lock"
                        }
                    },
                    category = "Privacy & Security",
                    keywords = listOf("lock", "security", "pin", "biometric", "password", "privacy"),
                    content = { AppLockTile() }
                ),
                SettingTile(
                    key = "SCREEN_PRIVACY",
                    title = "Screen Privacy",
                    subtitle = { _, _ -> "Prevent screenshots and hide app content in recents" },
                    category = "Privacy & Security",
                    keywords = listOf("screenshot", "privacy", "screen", "recents", "secure"),
                    content = { ScreenPrivacyTile() }
                ),
                SettingTile(
                    key = "PERMISSIONS",
                    title = "Permissions",
                    subtitle = { _, _ -> "Manage app permissions" },
                    category = "Privacy & Security",
                    keywords = listOf("permission", "location", "notification", "internet", "gps"),
                    content = { PermissionsTile() }
                ),
                SettingTile(
                    key = "CLOUD_SYNC",
                    title = "Cloud Sync",
                    subtitle = { _, _ -> "Sync across devices via Cloud" },
                    category = "Sync & Backup",
                    keywords = listOf("cloud", "sync", "google", "drive", "backup", "webdav"),
                    content = { CloudSyncTile() }
                ),
                SettingTile(
                    key = "LOCAL_BACKUP",
                    title = "Local Backup",
                    subtitle = { _, _ -> "Restore or export data locally" },
                    category = "Sync & Backup",
                    keywords = listOf("backup", "restore", "export", "import", "local", "json"),
                    content = { LocalBackupTile() }
                ),
                SettingTile(
                    key = "ABOUT_HEADER",
                    title = "About June",
                    subtitle = { _, _ -> "version and github link" },
                    category = "About",
                    keywords = listOf("about", "version", "github", "developer", "author"),
                    content = { AboutHeaderTile() }
                ),
                SettingTile(
                    key = "DEVELOPER",
                    title = "Developer profile",
                    subtitle = { _, _ -> "Denser Meerkat" },
                    category = "About",
                    keywords = listOf("developer", "author", "meerkat", "denser", "github", "email"),
                    content = { DeveloperTile() }
                ),
                SettingTile(
                    key = "LICENSE",
                    title = "License",
                    subtitle = { _, _ -> "GPL-3.0 License" },
                    category = "About",
                    keywords = listOf("license", "gpl", "open", "source", "terms"),
                    content = { LicenseTile() }
                ),
                SettingTile(
                    key = "ABOUT_LIBRARIES",
                    title = aboutLibrariesTitle,
                    subtitle = { _, _ -> aboutLibrariesTitle },
                    category = "About",
                    keywords = listOf("libraries", "licenses", "open", "source", "dependency"),
                    content = { AboutLibrariesTile() }
                ),
                SettingTile(
                    key = "MAP_CREDITS",
                    title = "Map Credits",
                    subtitle = { _, _ -> "Credits and licenses for map data" },
                    category = "About",
                    keywords = listOf("map", "attributions", "licenses", "credits", "osm", "maptiler", "mapbox", "stadia", "carto"),
                    content = { MapCreditsTile() }
                ),
                SettingTile(
                    key = "CHANGELOG",
                    title = "Changelog",
                    subtitle = { _, _ -> "View release history" },
                    category = "About",
                    keywords = listOf("changelog", "release", "history", "notes", "version", "updates"),
                    content = { ChangelogTile() }
                ),
                SettingTile(
                    key = "CHECK_FOR_UPDATES",
                    title = "Check for Updates",
                    subtitle = { _, _ -> "Check if a newer version is available" },
                    category = "About",
                    keywords = listOf("update", "check", "version", "github", "playstore", "latest"),
                    content = { CheckForUpdatesTile() }
                ),
            )
        }
    }

    @Composable
    fun getTilesForCategory(category: String): List<SettingTile> {
        return getTiles().filter { it.category == category }
    }

    @Composable
    fun getTile(key: String): SettingTile? {
        return getTiles().find { it.key == key }
    }
}
