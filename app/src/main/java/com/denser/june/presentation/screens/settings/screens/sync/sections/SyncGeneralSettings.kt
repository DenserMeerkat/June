package com.denser.june.presentation.screens.settings.screens.sync.sections

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.denser.june.core.R
import com.denser.june.presentation.components.JuneSwitchTile
import com.denser.june.presentation.screens.settings.components.SettingSection

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
            JuneSwitchTile(
                title = stringResource(R.string.automatic_sync),
                iconRes = R.drawable.cloud_sync_24px,
                checked = isAutoSyncOn,
                onCheckedChange = onToggleAutoSync
            )

            JuneSwitchTile(
                title = stringResource(R.string.wifi_only_sync),
                iconRes = R.drawable.wifi_24px,
                checked = syncOnlyOnWifi,
                onCheckedChange = onToggleWifiOnly
            )
        }
    }
}
