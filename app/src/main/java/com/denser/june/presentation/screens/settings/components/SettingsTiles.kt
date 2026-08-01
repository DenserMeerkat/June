package com.denser.june.presentation.screens.settings.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.denser.june.core.domain.model.enums.EditorLayoutDirection
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.presentation.screens.settings.SettingsState
import com.denser.june.presentation.screens.settings.tiles.*
import java.time.format.TextStyle
import com.denser.june.core.domain.model.enums.TimeFormat
import com.denser.june.core.utils.LanguageHelper
import androidx.compose.ui.res.stringResource
import com.denser.june.core.R

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
        return listOf(
            SettingTile(
                key = "ALWAYS_OPEN_NEW_NOTE",
                title = stringResource(R.string.always_open_new_note),
                subtitle = { context, _ -> context.getString(R.string.always_open_new_note_desc) },
                category = "General",
                keywords = listOf("open", "new", "note", "start", "startup", "always", "editor"),
                content = { AlwaysOpenNewNoteTile() }
            ),
            SettingTile(
                key = "REMINDERS",
                title = stringResource(R.string.reminders),
                subtitle = { context, _ -> context.getString(R.string.reminders_desc) },
                category = "General",
                keywords = listOf("reminder", "notification", "schedule", "alert"),
                content = { RemindersTile() }
            ),
            SettingTile(
                key = "INCLUDE_TIME",
                title = stringResource(R.string.include_time),
                subtitle = { context, _ -> context.getString(R.string.include_time_desc) },
                category = "General",
                keywords = listOf("time", "include", "journal", "auto"),
                content = { IncludeTimeTile() }
            ),
            SettingTile(
                key = "TIME_FORMAT",
                title = stringResource(R.string.time_format),
                subtitle = { context, state ->
                    if (state.timeFormat == TimeFormat.TWELVE_HOUR)
                        context.getString(R.string.time_format_12h)
                    else
                        context.getString(R.string.time_format_24h)
                },
                category = "General",
                keywords = listOf("time", "clock", "hour", "format", "12", "24"),
                content = { TimeFormatTile() }
            ),
            SettingTile(
                key = "MAP_SETTINGS",
                title = stringResource(R.string.map_settings),
                subtitle = { context, _ -> context.getString(R.string.map_settings_desc) },
                category = "General",
                keywords = listOf("map", "maptiler", "stadia", "mapbox", "key", "style", "settings", "carto"),
                content = { MapSettingsTile() }
            ),
            SettingTile(
                key = "MARKDOWN_EDITOR",
                title = stringResource(R.string.markdown_editor),
                subtitle = { context, state ->
                    if (state.isMarkdownEnabled)
                        context.getString(R.string.markdown_editor_desc)
                    else
                        context.getString(R.string.plain_text_mode)
                },
                category = "General",
                keywords = listOf("markdown", "editor", "rich", "text", "format", "plain"),
                content = { MarkdownEditorTile() }
            ),
            SettingTile(
                key = "KEYBOARD_CAPITALIZATION",
                title = stringResource(R.string.keyboard_capitalization),
                subtitle = { context, state ->
                    if (state.isKeyboardCapitalizationEnabled)
                        context.getString(R.string.keyboard_capitalization_desc)
                    else
                        context.getString(R.string.disabled)
                },
                category = "General",
                keywords = listOf("keyboard", "capitalization", "casing", "auto", "letters", "sentences"),
                content = { KeyboardCapitalizationTile() }
            ),
            SettingTile(
                key = "KEYBOARD_AUTOCORRECT",
                title = stringResource(R.string.keyboard_autocorrect),
                subtitle = { context, state ->
                    if (state.isKeyboardAutocorrectEnabled)
                        context.getString(R.string.keyboard_autocorrect_desc)
                    else
                        context.getString(R.string.disabled)
                },
                category = "General",
                keywords = listOf("keyboard", "autocorrect", "correction", "spellcheck", "spelling", "auto"),
                content = { KeyboardAutocorrectTile() }
            ),
            SettingTile(
                key = "EDITOR_DIRECTION",
                title = stringResource(R.string.editor_direction),
                subtitle = { context, state ->
                    if (state.editorLayoutDirection == EditorLayoutDirection.AUTO)
                        context.getString(R.string.editor_direction_auto)
                    else
                        state.editorLayoutDirection.name
                },
                category = "General",
                keywords = listOf("editor", "direction", "rtl", "ltr", "writing", "auto", "text"),
                content = { EditorDirectionTile() }
            ),
            SettingTile(
                key = "START_OF_WEEK",
                title = stringResource(R.string.start_of_week),
                subtitle = { _, state -> state.startOfWeek.getDisplayName(TextStyle.FULL, java.util.Locale.getDefault()) },
                category = "General",
                keywords = listOf("start", "week", "day", "calendar", "sunday", "monday"),
                content = { StartOfWeekTile() }
            ),
            SettingTile(
                key = "DELETE_ALL_JOURNALS",
                title = stringResource(R.string.delete_all_journals),
                subtitle = { context, _ -> context.getString(R.string.delete_all_journals_desc) },
                category = "General",
                keywords = listOf("delete", "remove", "erase", "all", "journals", "clear"),
                content = { DeleteAllJournalsTile() }
            ),
            SettingTile(
                key = "LANGUAGE",
                title = stringResource(R.string.language),
                subtitle = { context, _ -> LanguageHelper.getCurrentLanguageNativeName(context) },
                category = "Appearance",
                keywords = listOf("language", "locale", "translation", "i18n", "arabic", "spanish", "german", "french", "persian"),
                content = { LanguageTile() }
            ),
            SettingTile(
                key = "FORCE_LTR_APP_LAYOUT",
                title = stringResource(R.string.force_ltr_layout),
                subtitle = { context, _ -> context.getString(R.string.force_ltr_layout_desc) },
                category = "Appearance",
                keywords = listOf("force", "ltr", "rtl", "layout", "direction", "app", "navigation", "arabic", "persian", "hebrew"),
                content = { ForceLtrAppLayoutTile() }
            ),
            SettingTile(
                key = "APP_THEME",
                title = stringResource(R.string.app_theme),
                subtitle = { context, state -> context.getString(state.appTheme.themeMode.stringRes) },
                category = "Appearance",
                keywords = listOf("theme", "dark", "light", "mode", "amoled", "color"),
                content = { AppThemeTile() }
            ),
            SettingTile(
                key = "APP_FONT",
                title = stringResource(R.string.app_font),
                subtitle = { _, state -> state.appTheme.appFont },
                category = "Appearance",
                keywords = listOf("font", "typography", "text", "style", "size"),
                content = { AppFontTile() }
            ),
            SettingTile(
                key = "AMOLED",
                title = stringResource(R.string.amoled),
                subtitle = { context, _ -> context.getString(R.string.amoled_desc) },
                category = "Appearance",
                keywords = listOf("amoled", "black", "dark", "oled", "battery"),
                content = { AmoledTile() }
            ),
            SettingTile(
                key = "MATERIAL_THEME",
                title = stringResource(R.string.material_theme),
                subtitle = { context, _ -> context.getString(R.string.material_theme_desc) },
                category = "Appearance",
                keywords = listOf("material", "you", "dynamic", "color", "wallpaper"),
                content = { MaterialThemeTile() }
            ),
            SettingTile(
                key = "SEED_COLOR",
                title = stringResource(R.string.seed_color),
                subtitle = { context, _ -> context.getString(R.string.seed_color_desc) },
                category = "Appearance",
                keywords = listOf("seed", "color", "picker", "accent", "custom"),
                content = { SeedColorTile() }
            ),
            SettingTile(
                key = "PALETTE_SELECTION",
                title = stringResource(R.string.palette_selection),
                subtitle = { context, _ -> context.getString(R.string.palette_selection_desc) },
                category = "Appearance",
                keywords = listOf("palette", "style", "theme", "tonal", "scheme"),
                content = { PaletteSelectionSettingsItem() }
            ),
            SettingTile(
                key = "APP_LOCK",
                title = stringResource(R.string.app_lock),
                subtitle = { context, state ->
                    if (state.isAppLockEnabled) {
                        if (state.lockType == LockType.PIN) context.getString(R.string.custom_pin) else context.getString(R.string.same_as_screen_lock)
                    } else {
                        context.getString(R.string.no_lock)
                    }
                },
                category = "Privacy & Security",
                keywords = listOf("lock", "security", "pin", "biometric", "password", "privacy"),
                content = { AppLockTile() }
            ),
            SettingTile(
                key = "SCREEN_PRIVACY",
                title = stringResource(R.string.screen_privacy),
                subtitle = { context, _ -> context.getString(R.string.screen_privacy_desc) },
                category = "Privacy & Security",
                keywords = listOf("screenshot", "privacy", "screen", "recents", "secure"),
                content = { ScreenPrivacyTile() }
            ),
            SettingTile(
                key = "PERMISSIONS",
                title = stringResource(R.string.permissions),
                subtitle = { context, _ -> context.getString(R.string.permissions_desc) },
                category = "Privacy & Security",
                keywords = listOf("permission", "location", "notification", "internet", "gps"),
                content = { PermissionsTile() }
            ),
            SettingTile(
                key = "CLOUD_SYNC",
                title = stringResource(R.string.cloud_sync),
                subtitle = { context, _ -> context.getString(R.string.cloud_sync_desc) },
                category = "Sync & Backup",
                keywords = listOf("cloud", "sync", "google", "drive", "backup", "webdav"),
                content = { CloudSyncTile() }
            ),
            SettingTile(
                key = "ABOUT_HEADER",
                title = stringResource(R.string.about_june),
                subtitle = { context, _ -> context.getString(R.string.about_june_desc) },
                category = "About",
                keywords = listOf("about", "version", "github", "developer", "author"),
                content = { AboutHeaderTile() }
            ),
            SettingTile(
                key = "DEVELOPER",
                title = stringResource(R.string.developer_profile),
                subtitle = { _, _ -> "Denser Meerkat" },
                category = "About",
                keywords = listOf("developer", "author", "meerkat", "denser", "github", "email"),
                content = { DeveloperTile() }
            ),
            SettingTile(
                key = "LICENSE",
                title = stringResource(R.string.license),
                subtitle = { context, _ -> context.getString(R.string.gpl_license_desc) },
                category = "About",
                keywords = listOf("license", "gpl", "open", "source", "terms"),
                content = { LicenseTile() }
            ),
            SettingTile(
                key = "ABOUT_LIBRARIES",
                title = stringResource(R.string.about_libraries),
                subtitle = { context, _ -> context.getString(R.string.about_libraries_desc) },
                category = "About",
                keywords = listOf("libraries", "licenses", "open", "source", "dependency"),
                content = { AboutLibrariesTile() }
            ),
            SettingTile(
                key = "MAP_CREDITS",
                title = stringResource(R.string.map_credits),
                subtitle = { context, _ -> context.getString(R.string.map_credits_desc) },
                category = "About",
                keywords = listOf("map", "attributions", "licenses", "credits", "osm", "maptiler", "mapbox", "stadia", "carto"),
                content = { MapCreditsTile() }
            ),
            SettingTile(
                key = "CHANGELOG",
                title = stringResource(R.string.changelog),
                subtitle = { context, _ -> context.getString(R.string.changelog_desc) },
                category = "About",
                keywords = listOf("changelog", "release", "history", "notes", "version", "updates"),
                content = { ChangelogTile() }
            ),
            SettingTile(
                key = "TRANSLATE",
                title = stringResource(R.string.translate_app),
                subtitle = { context, _ -> context.getString(R.string.help_translate_desc) },
                category = "About",
                keywords = listOf("translate", "weblate", "translation", "i18n", "languages", "crowd", "community"),
                content = { TranslateTile() }
            ),
            SettingTile(
                key = "CHECK_FOR_UPDATES",
                title = stringResource(R.string.check_for_updates),
                subtitle = { context, _ -> context.getString(R.string.check_for_updates_desc) },
                category = "About",
                keywords = listOf("update", "check", "version", "github", "playstore", "latest"),
                content = { CheckForUpdatesTile() }
            ),
        )
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
