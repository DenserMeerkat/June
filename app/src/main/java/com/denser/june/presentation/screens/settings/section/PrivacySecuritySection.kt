package com.denser.june.presentation.screens.settings.section

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsState
import org.koin.compose.koinInject

@Composable
fun PrivacySecuritySection(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val navigator = koinInject<AppNavigator>()

    SettingSection(title = "Privacy & Security") {
        SettingsItem(
            title = "App Lock",
            subtitle = if (state.isAppLockEnabled) {
                if (state.lockType == LockType.PIN) "Custom PIN" else "Same as screen lock"
            } else {
                "No lock"
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.lock_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            onClick = { navigator.navigateTo(Route.LockMethod) }
        )

        SettingsItem(
            title = "Screen Privacy",
            subtitle = "Prevent screenshots and hide app content in recents",
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.preview_off_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            trailingContent = {
                Switch(
                    checked = state.isScreenPrivacyEnabled,
                    onCheckedChange = { onAction(SettingsAction.OnScreenPrivacyToggle(it)) }
                )
            },
            onClick = { onAction(SettingsAction.OnScreenPrivacyToggle(!state.isScreenPrivacyEnabled)) }
        )

        SettingsItem(
            title = "Permissions",
            subtitle = "Manage app permissions",
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.security_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            onClick = { navigator.navigateTo(Route.Permissions) }
        )
    }
}
