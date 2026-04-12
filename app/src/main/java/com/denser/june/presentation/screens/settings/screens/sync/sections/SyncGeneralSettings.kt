package com.denser.june.presentation.screens.settings.screens.sync.sections

import androidx.compose.animation.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.denser.june.core.R
import com.denser.june.presentation.screens.settings.section.SettingSection
import com.denser.june.presentation.screens.settings.section.SettingsItem

@Composable
fun SyncGeneralSettings(
    isVisible: Boolean,
    isAutoSyncOn: Boolean,
    syncOnlyOnWifi: Boolean,
    onToggleAutoSync: (Boolean) -> Unit,
    onToggleWifiOnly: (Boolean) -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        SettingSection(title = "General Settings") {
            SettingsItem(
                title = "Automatic Sync",
                subtitle = "Sync changes as they happen",
                leadingContent = {
                    Icon(painterResource(R.drawable.cloud_sync_24px), null)
                },
                trailingContent = {
                    Switch(
                        checked = isAutoSyncOn,
                        onCheckedChange = onToggleAutoSync
                    )
                },
                onClick = { onToggleAutoSync(!isAutoSyncOn) }
            )

            SettingsItem(
                title = "Auto-sync on Wi-Fi only",
                subtitle = "Avoid syncing over mobile data",
                leadingContent = {
                    Icon(painterResource(R.drawable.wifi_24px), null)
                },
                trailingContent = {
                    Switch(
                        checked = syncOnlyOnWifi,
                        onCheckedChange = onToggleWifiOnly
                    )
                },
                onClick = { onToggleWifiOnly(!syncOnlyOnWifi) }
            )
        }
    }
}
