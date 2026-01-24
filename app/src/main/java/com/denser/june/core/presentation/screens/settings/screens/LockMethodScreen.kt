package com.denser.june.core.presentation.screens.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.denser.june.R
import com.denser.june.core.domain.enums.LockType
import com.denser.june.core.navigation.AppNavigator
import com.denser.june.core.navigation.Route
import com.denser.june.core.presentation.components.JuneAppBarType
import com.denser.june.core.presentation.components.JuneTopAppBar
import com.denser.june.core.presentation.screens.settings.SettingsAction
import com.denser.june.core.presentation.screens.settings.SettingsState
import com.denser.june.core.presentation.screens.settings.section.SettingSection
import com.denser.june.core.presentation.screens.settings.section.SettingsItem
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockMethodScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
) {
    val navigator = koinInject<AppNavigator>()
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val safeSwitch: (() -> Unit) -> Unit = { action ->
        if (state.isAppLockEnabled && state.lockType == LockType.PIN) {
            pendingAction = action
        } else {
            action()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                title = { Text("Lock your journal") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Back",
                        )
                    }
                },
            )
        }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Keep your journal private by adding an extra layer of security",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.size(24.dp))
            SettingSection(title = "Ways to lock") {

                val isBiometricSelected =
                    state.isAppLockEnabled && state.lockType == LockType.BIOMETRIC
                val onBiometricClick = {
                    onAction(SettingsAction.UpdateLockType(LockType.BIOMETRIC))
                    onAction(SettingsAction.OnAppLockToggle(true))
                }
                SettingsItem(title = "Same as screen lock", leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.fingerprint_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }, trailingContent = {
                    RadioButton(
                        selected = isBiometricSelected,
                        onClick = { safeSwitch(onBiometricClick) })
                }, onClick = { safeSwitch(onBiometricClick) })

                val isPinSelected = state.isAppLockEnabled && state.lockType == LockType.PIN
                SettingsItem(title = "Custom PIN", leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.password_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }, trailingContent = {
                    RadioButton(
                        selected = isPinSelected, onClick = {
                            if (!isPinSelected) navigator.navigateTo(Route.PinSetup)
                        })
                }, onClick = {
                    if (!isPinSelected) navigator.navigateTo(Route.PinSetup)
                })

                val isNoLockSelected = !state.isAppLockEnabled
                val onNoLockClick = {
                    onAction(SettingsAction.OnAppLockToggle(false))
                }
                SettingsItem(title = "No lock", leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.no_encryption_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }, trailingContent = {
                    RadioButton(
                        selected = isNoLockSelected, onClick = { safeSwitch(onNoLockClick) })
                }, onClick = { safeSwitch(onNoLockClick) })
            }
            Spacer(modifier = Modifier.size(24.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.warning_24px),
                            contentDescription = "Warning",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Important",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "If you forget your Custom PIN, you will lose access to your journal. There is no recovery option.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (pendingAction != null) {
            AlertDialog(onDismissRequest = { pendingAction = null }, icon = {
                Icon(painterResource(R.drawable.warning_24px), contentDescription = null)
            }, title = {
                Text(text = "Change lock method?")
            }, text = {
                Text("You are switching away from Custom PIN. Your current PIN will be removed, and you will need to set it up again if you switch back.")
            }, confirmButton = {
                Button(
                    onClick = {
                        pendingAction?.invoke()
                        pendingAction = null
                    }) {
                    Text("Change")
                }
            }, dismissButton = {
                OutlinedButton(
                    onClick = { pendingAction = null }) {
                    Text("Cancel")
                }
            })
        }
    }
}