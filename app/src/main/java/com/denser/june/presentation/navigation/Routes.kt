package com.denser.june.presentation.navigation

import com.denser.june.core.domain.model.enums.FontType
import com.denser.june.core.domain.model.enums.LockType
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Home : Route

    @Serializable
    data class Editor(
        val journalId: String? = null,
        val initialDate: Long? = null,
        val initialTags: List<String>? = null,
        val initialTitle: String? = null,
        val initialContent: String? = null,
        val initialEmoji: String? = null
    ) : Route

    @Serializable
    data class JournalMedia(val journalId: String) : Route

    @Serializable
    data object AddSong : Route

    @Serializable
    data class RecordAudio(val initialAudioId: String? = null) : Route

    @Serializable
    data class JournalMediaDetail(
        val journalId: String,
        val initialIndex: Int,
        val dateTime: Long? = null
    ) : Route

    @Serializable
    data class MediaViewerRoute(
        val mediaPaths: List<String>,
        val initialIndex: Int,
        val dateTime: Long? = null
    ) : Route

    @Serializable
    data object Bin : Route

    @Serializable
    data object Vault : Route

    @Serializable
    data object VaultSettings : Route

    @Serializable
    data object SyncSettings : Route

    @Serializable
    data object ReminderSettings : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object GeneralSettings : Route

    @Serializable
    data object EditorSettings : Route

    @Serializable
    data object AppearanceSettings : Route

    @Serializable
    data object PrivacySecuritySettings : Route

    @Serializable
    data object SyncBackupSettings : Route

    @Serializable
    data object AboutSettings : Route



    @Serializable
    data object AppLockSettings : Route

    @Serializable
    data object VaultLockSettings : Route

    @Serializable
    data class PinSetup(val forVault: Boolean = false) : Route

    @Serializable
    data class PasswordSetup(val forVault: Boolean = false) : Route

    @Serializable
    data class PatternSetup(val forVault: Boolean = false) : Route

    @Serializable
    data class RecoverySetup(val lockType: LockType = LockType.PIN, val forVault: Boolean = false) : Route

    @Serializable
    data class Fonts(val type: FontType = FontType.APP) : Route

    @Serializable
    data object MapSettings : Route

    @Serializable
    data object DeveloperSettings : Route

    @Serializable
    data object ConsoleLogs : Route
}