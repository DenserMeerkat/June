package com.denser.june.presentation.screens.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.SettingsItem
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.denser.june.presentation.components.JuneDialog
import com.denser.june.presentation.components.JuneTextField
import com.denser.june.presentation.components.PinLockScreen
import com.denser.june.core.utils.SecurityUtils
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockMethodScreen() {
    val settingsVM: SettingsVM = koinViewModel()
    val state = settingsVM.state.collectAsStateWithLifecycle().value
    val onAction = settingsVM::onAction
    val navigator = koinInject<AppNavigator>()
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isVerifyingPinForSetup by remember { mutableStateOf(false) }
    var pinVerificationError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val safeSwitch: (() -> Unit) -> Unit = { action ->
        if (state.isAppLockEnabled && state.lockType == LockType.PIN) {
            pendingAction = action
        } else {
            action()
        }
    }

    if (isVerifyingPinForSetup) {
        BackHandler {
            isVerifyingPinForSetup = false
            pinVerificationError = false
        }
        PinLockScreen(
            title = stringResource(R.string.enter_pin_to_verify),
            isError = pinVerificationError,
            onPinSubmitted = { pin ->
                val inputHash = SecurityUtils.hashPin(pin)
                if (inputHash == state.pinHash) {
                    navigator.navigateTo(Route.RecoverySetup)
                    coroutineScope.launch {
                        delay(500)
                        isVerifyingPinForSetup = false
                        pinVerificationError = false
                    }
                } else {
                    pinVerificationError = true
                }
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                title = { Text(stringResource(R.string.lock_your_journal)) },
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
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.lock_journal_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.size(24.dp))
            SettingSection(title = stringResource(R.string.ways_to_lock)) {

                val isBiometricSelected =
                    state.isAppLockEnabled && state.lockType == LockType.BIOMETRIC
                val onBiometricClick = {
                    onAction(SettingsAction.UpdateLockType(LockType.BIOMETRIC))
                    onAction(SettingsAction.OnAppLockToggle(true))
                }
                SettingsItem(title = stringResource(R.string.same_as_screen_lock), leadingContent = {
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
                SettingsItem(title = stringResource(R.string.custom_pin), leadingContent = {
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
                SettingsItem(title = stringResource(R.string.no_lock), leadingContent = {
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

            val isPinSelected = state.isAppLockEnabled && state.lockType == LockType.PIN
            if (isPinSelected) {
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
                        val warningText = if (state.securityQuestion != null) {
                            "If you forget your Custom PIN, you can recover/reset it using your configured security question."
                        } else {
                            "If you forget your Custom PIN, you will lose access to your journal. There is no recovery option. Please configure a recovery question."
                        }
                        Text(
                            text = warningText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { isVerifyingPinForSetup = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (state.securityQuestion != null) "Change recovery" else "Setup recovery"
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp + padding.calculateBottomPadding()))
        }
    }

        if (pendingAction != null) {
            JuneDialog(
                onDismissRequest = { pendingAction = null },
                title = stringResource(R.string.change_lock_title),
                icon = R.drawable.warning_24px,
                confirmButton = {
                    Button(
                        onClick = {
                            pendingAction?.invoke()
                            pendingAction = null
                        }) {
                        Text(stringResource(R.string.change))
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { pendingAction = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                text = {
                    Text(stringResource(R.string.switch_lock_warning))
                }
            )
        }
    }
}