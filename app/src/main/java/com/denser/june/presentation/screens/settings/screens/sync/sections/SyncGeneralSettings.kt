package com.denser.june.presentation.screens.settings.screens.sync.sections

import androidx.compose.animation.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.denser.june.core.R
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.SettingsItem

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
        SettingSection(title = stringResource(R.string.general_settings)) {
            SettingsItem(
                title = stringResource(R.string.automatic_sync),
                subtitle = stringResource(R.string.automatic_sync_desc),
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
                title = stringResource(R.string.wifi_only_sync),
                subtitle = stringResource(R.string.wifi_only_sync_desc),
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
