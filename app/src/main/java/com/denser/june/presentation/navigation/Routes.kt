package com.denser.june.presentation.navigation

import com.denser.june.core.domain.model.enums.FontType
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data class Editor(
        val journalId: String? = null,
        val initialDate: Long? = null,
        val initialTags: List<String>? = null
    ) : Route

    @Serializable
    data class JournalMedia(val journalId: String) : Route

    @Serializable
    data class JournalMediaDetail(
        val journalId: String,
        val initialIndex: Int
    ) : Route

    @Serializable
    data class MediaViewerRoute(
        val mediaPaths: List<String>,
        val initialIndex: Int
    ) : Route

    @Serializable
    data object Search : Route

    @Serializable
    data object Bin : Route

    @Serializable
    data object SyncSettings : Route

    @Serializable
    data object ReminderSettings : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object GeneralSettings : Route

    @Serializable
    data object AppearanceSettings : Route

    @Serializable
    data object PrivacySecuritySettings : Route

    @Serializable
    data object SyncBackupSettings : Route

    @Serializable
    data object AboutSettings : Route

    @Serializable
    data object Backup : Route

    @Serializable
    data object Permissions : Route

    @Serializable
    data object LockMethod : Route

    @Serializable
    data object PinSetup : Route

    @Serializable
    data class Fonts(val type: FontType = FontType.APP) : Route

    @Serializable
    data object MapSettings : Route
}