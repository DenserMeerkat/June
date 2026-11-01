package com.denser.june.presentation.screens.settings.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.core.utils.SecurityUtils
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneDialog
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.components.JuneBackButton
import com.denser.june.presentation.components.plus.PlusSettingsItem
import com.denser.june.presentation.screens.settings.screens.lock.PasswordLockContent
import com.denser.june.presentation.screens.settings.screens.lock.PatternLockContent
import com.denser.june.presentation.screens.settings.screens.lock.PinLockContent
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.components.SettingsItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class PrimaryLockOption(
    val title: String,
    val subtitle: String? = null,
    val isSelected: Boolean = false,
    val onSelect: () -> Unit,
    @field:DrawableRes val iconRes: Int = R.drawable.lock_24px
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockMethodContent(
    title: String,
    description: String,
    activeLockType: LockType?,
    hasActiveCredentials: Boolean = false,
    pinHash: String? = null,
    passwordHash: String? = null,
    patternHash: String? = null,
    securityQuestion: String? = null,
    primaryOption: PrimaryLockOption? = null,
    onBackClick: () -> Unit,
    onSelectBiometric: () -> Unit,
    onSelectPin: () -> Unit,
    onSelectPassword: () -> Unit,
    onSelectPattern: () -> Unit,
    onSelectNone: (() -> Unit)? = null,
    onNavigateToRecovery: (LockType) -> Unit = {}
) {
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isVerifyingForRecovery by remember { mutableStateOf(false) }
    var verificationError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val safeSwitch: (() -> Unit) -> Unit = { action ->
        if (hasActiveCredentials) {
            pendingAction = action
        } else {
            action()
        }
    }

    if (isVerifyingForRecovery) {
        BackHandler {
            isVerifyingForRecovery = false
            verificationError = false
        }

        when (activeLockType) {
            LockType.PIN -> {
                PinLockContent(
                    title = stringResource(R.string.enter_pin),
                    isError = verificationError,
                    onErrorDismissed = { verificationError = false },
                    onBackClick = {
                        isVerifyingForRecovery = false
                        verificationError = false
                    },
                    onPinSubmitted = { pin ->
                        val inputHash = SecurityUtils.hashPin(pin)
                        if (inputHash == pinHash) {
                            onNavigateToRecovery(LockType.PIN)
                            coroutineScope.launch {
                                delay(500.milliseconds)
                                isVerifyingForRecovery = false
                                verificationError = false
                            }
                        } else {
                            verificationError = true
                        }
                    }
                )
            }
            LockType.PASSWORD -> {
                PasswordLockContent(
                    title = stringResource(R.string.enter_password),
                    isError = verificationError,
                    onErrorDismissed = { verificationError = false },
                    onBackClick = {
                        isVerifyingForRecovery = false
                        verificationError = false
                    },
                    onPasswordSubmitted = { password ->
                        val inputHash = SecurityUtils.hashPassword(password)
                        if (inputHash == passwordHash) {
                            onNavigateToRecovery(LockType.PASSWORD)
                            coroutineScope.launch {
                                delay(500.milliseconds)
                                isVerifyingForRecovery = false
                                verificationError = false
                            }
                        } else {
                            verificationError = true
                        }
                    }
                )
            }
            LockType.PATTERN -> {
                PatternLockContent(
                    title = stringResource(R.string.enter_pattern),
                    isError = verificationError,
                    onErrorDismissed = { verificationError = false },
                    onBackClick = {
                        isVerifyingForRecovery = false
                        verificationError = false
                    },
                    onPatternSubmitted = { pattern ->
                        val inputHash = SecurityUtils.hashPattern(pattern)
                        if (inputHash == patternHash) {
                            onNavigateToRecovery(LockType.PATTERN)
                            coroutineScope.launch {
                                delay(500.milliseconds)
                                isVerifyingForRecovery = false
                                verificationError = false
                            }
                        } else {
                            verificationError = true
                        }
                    }
                )
            }
            else -> {
                isVerifyingForRecovery = false
                onNavigateToRecovery(activeLockType ?: LockType.PIN)
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                JuneTopAppBar(
                    type = JuneAppBarType.Large,
                    title = { Text(title) },
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        JuneBackButton(onClick = onBackClick)
                    },
                )
            }
        ) { padding ->
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
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.size(24.dp))
                val waysToLockPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)

                if (primaryOption != null) {
                    SettingSection(
                        title=primaryOption.title
                    ) {
                        SettingsItem(
                            title = primaryOption.subtitle ?: stringResource(R.string.no_lock),
                            contentPadding = waysToLockPadding,
                            leadingContent = {
                                Icon(
                                    painter = painterResource(primaryOption.iconRes),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = primaryOption.isSelected,
                                    onClick = {
                                        if (!primaryOption.isSelected) safeSwitch(primaryOption.onSelect)
                                    }
                                )
                            },
                            onClick = {
                                if (!primaryOption.isSelected) safeSwitch(primaryOption.onSelect)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.size(24.dp))
                }

                SettingSection(
                    title = stringResource(R.string.ways_to_lock)
                ) {
                    val isBiometricSelected = activeLockType == LockType.BIOMETRIC
                    SettingsItem(
                        title = stringResource(R.string.same_as_screen_lock),
                        contentPadding = waysToLockPadding,
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.fingerprint_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            RadioButton(
                                selected = isBiometricSelected,
                                onClick = {
                                    if (!isBiometricSelected) safeSwitch(onSelectBiometric)
                                }
                            )
                        },
                        onClick = {
                            if (!isBiometricSelected) safeSwitch(onSelectBiometric)
                        }
                    )

                    val isPinSelected = activeLockType == LockType.PIN
                    SettingsItem(
                        title = stringResource(R.string.custom_pin),
                        contentPadding = waysToLockPadding,
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.pin_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        trailingContent = {
                            RadioButton(
                                selected = isPinSelected,
                                onClick = {
                                    if (!isPinSelected) safeSwitch(onSelectPin)
                                }
                            )
                        },
                        onClick = {
                            if (!isPinSelected) safeSwitch(onSelectPin)
                        }
                    )

                    val isPasswordSelected = activeLockType == LockType.PASSWORD
                    PlusSettingsItem(
                        title = stringResource(R.string.custom_password),
                        iconRes = R.drawable.password_24px,
                        contentPadding = waysToLockPadding,
                        trailingContent = {
                            RadioButton(
                                selected = isPasswordSelected,
                                onClick = {
                                    if (!isPasswordSelected) safeSwitch(onSelectPassword)
                                }
                            )
                        },
                        onClick = {
                            if (!isPasswordSelected) safeSwitch(onSelectPassword)
                        }
                    )

                    val isPatternSelected = activeLockType == LockType.PATTERN
                    PlusSettingsItem(
                        title = stringResource(R.string.custom_pattern),
                        iconRes = R.drawable.pattern_24px,
                        contentPadding = waysToLockPadding,
                        trailingContent = {
                            RadioButton(
                                selected = isPatternSelected,
                                onClick = {
                                    if (!isPatternSelected) safeSwitch(onSelectPattern)
                                }
                            )
                        },
                        onClick = {
                            if (!isPatternSelected) safeSwitch(onSelectPattern)
                        }
                    )

                    if (onSelectNone != null) {
                        val isNoLockSelected = activeLockType == null && (primaryOption == null || !primaryOption.isSelected)
                        SettingsItem(
                            title = stringResource(R.string.no_lock),
                            contentPadding = waysToLockPadding,
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.no_encryption_24px),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingContent = {
                                RadioButton(
                                    selected = isNoLockSelected,
                                    onClick = {
                                        if (!isNoLockSelected) safeSwitch(onSelectNone)
                                    }
                                )
                            },
                            onClick = {
                                if (!isNoLockSelected) safeSwitch(onSelectNone)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.size(24.dp))

                val isCustomCredentialLock = activeLockType == LockType.PIN ||
                        activeLockType == LockType.PASSWORD ||
                        activeLockType == LockType.PATTERN

                if (isCustomCredentialLock) {
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
                                    text = stringResource(R.string.important),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            val warningText = if (securityQuestion != null) {
                                stringResource(R.string.lock_recovery_configured_desc)
                            } else {
                                stringResource(R.string.lock_no_recovery_warning_desc)
                            }
                            Text(
                                text = warningText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { isVerifyingForRecovery = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (securityQuestion != null) {
                                        stringResource(R.string.change_recovery)
                                    } else {
                                        stringResource(R.string.setup_recovery)
                                    }
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
                            val action = pendingAction
                            pendingAction = null
                            action?.invoke()
                        }
                    ) {
                        Text(stringResource(R.string.change))
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { pendingAction = null }
                    ) {
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

