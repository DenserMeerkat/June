package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppLockTile() {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()

    SettingsItem(
        title = stringResource(R.string.app_lock),
        subtitle = if (state.isAppLockEnabled) {
            when (state.lockType) {
                LockType.BIOMETRIC -> stringResource(R.string.same_as_screen_lock)
                LockType.PIN -> stringResource(R.string.custom_pin)
                LockType.PASSWORD -> stringResource(R.string.custom_password)
                LockType.PATTERN -> stringResource(R.string.custom_pattern)
            }
        } else {
            stringResource(R.string.no_lock)
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.mobile_lock_portrait_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        onClick = { navigator.navigateTo(Route.AppLockSettings) }
    )
}
