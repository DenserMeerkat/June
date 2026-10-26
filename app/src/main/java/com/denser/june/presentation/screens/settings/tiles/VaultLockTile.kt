package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.koinInject

@Composable
fun VaultLockTile() {
    val privacyPreferences = koinInject<PrivacyPreferences>()
    val navigator = koinInject<AppNavigator>()

    val useCustomVaultLock by privacyPreferences.getVaultUseCustomLockFlow().collectAsStateWithLifecycle(initialValue = false)
    val vaultLockType by privacyPreferences.getVaultLockTypeFlow().collectAsStateWithLifecycle(initialValue = null)

    val subtitle = if (useCustomVaultLock && vaultLockType != null) {
        when (vaultLockType!!) {
            LockType.BIOMETRIC -> stringResource(R.string.same_as_screen_lock)
            LockType.PIN -> stringResource(R.string.custom_pin)
            LockType.PASSWORD -> stringResource(R.string.custom_password)
            LockType.PATTERN -> stringResource(R.string.custom_pattern)
        }
    } else {
        stringResource(R.string.app_lock)
    }

    SettingsItem(
        title = stringResource(R.string.vault),
        subtitle = subtitle,
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.lock_open_circle_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        onClick = { navigator.navigateTo(Route.VaultLockSettings) }
    )
}
