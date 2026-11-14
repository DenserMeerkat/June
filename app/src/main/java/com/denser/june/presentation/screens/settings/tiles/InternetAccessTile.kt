package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.presentation.components.JuneSwitchTile
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun InternetAccessTile() {
    val privacyPreferences = koinInject<PrivacyPreferences>()
    val isInternetAllowed by privacyPreferences.getIsInternetAllowedFlow()
        .collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()

    JuneSwitchTile(
        title = stringResource(R.string.internet_access),
        checked = isInternetAllowed,
        onCheckedChange = { allowed ->
            scope.launch { privacyPreferences.updateIsInternetAllowed(allowed) }
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.wifi_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    )
}
