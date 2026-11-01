package com.denser.june.presentation.screens.settings.screens
import com.denser.june.presentation.components.JuneBackButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import com.denser.june.presentation.components.JuneDropdownMenu
import com.denser.june.presentation.components.JuneDropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.LockType
import com.denser.june.core.utils.SecurityUtils
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTextField
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

import com.denser.june.presentation.utils.SecurityQuestionUtils

import androidx.compose.runtime.rememberCoroutineScope
import com.denser.june.core.domain.preferences.PrivacyPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverySetupScreen(lockType: LockType = LockType.PIN, forVault: Boolean = false) {
    val settingsVM: SettingsVM = koinViewModel()
    val state = settingsVM.state.collectAsStateWithLifecycle().value
    val onAction = settingsVM::onAction
    val privacyPreferences = koinInject<PrivacyPreferences>()
    val coroutineScope = rememberCoroutineScope()
    val vaultQuestion by privacyPreferences.getVaultSecurityQuestionFlow()
        .collectAsStateWithLifecycle(initialValue = null)
    val effectiveQuestion = if (forVault) vaultQuestion else state.securityQuestion
    val vaultPinHash by privacyPreferences.getVaultPinHashFlow().collectAsStateWithLifecycle(initialValue = null)
    val vaultPasswordHash by privacyPreferences.getVaultPasswordHashFlow().collectAsStateWithLifecycle(initialValue = null)
    val vaultPatternHash by privacyPreferences.getVaultPatternHashFlow().collectAsStateWithLifecycle(initialValue = null)

    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current
    val lockSetSuccessMsg = stringResource(R.string.lock_set_successfully)

    val customQuestionTitle = stringResource(R.string.question_custom)
    val predefinedItems = remember { SecurityQuestionUtils.PREDEFINED_QUESTIONS }

    val questionItems = predefinedItems.map {
        stringResource(it.questionResId) to stringResource(it.placeholderResId)
    } + (customQuestionTitle to stringResource(R.string.placeholder_default_answer))

    val predefinedIndex = remember(effectiveQuestion) {
        SecurityQuestionUtils.findPredefinedQuestionIndex(context, effectiveQuestion)
    }
    val isCustomStored = remember(effectiveQuestion, predefinedIndex) {
        effectiveQuestion != null && predefinedIndex == null
    }

    var selectedQuestion by remember(effectiveQuestion, predefinedIndex, customQuestionTitle) {
        mutableStateOf(
            if (isCustomStored) customQuestionTitle
            else if (predefinedIndex != null) questionItems[predefinedIndex].first
            else questionItems.first().first
        )
    }
    var customQuestionText by remember(effectiveQuestion, isCustomStored) {
        mutableStateOf(if (isCustomStored) effectiveQuestion ?: "" else "")
    }
    var answer by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var answerError by remember { mutableStateOf(false) }
    var customQuestionError by remember { mutableStateOf(false) }

    val currentPlaceholder = remember(selectedQuestion, questionItems) {
        questionItems.find { it.first == selectedQuestion }?.second
            ?: questionItems.last().second
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                title = { Text(stringResource(R.string.security_question)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    JuneBackButton(onClick = { navigator.navigateBack() })
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.recovery_setup_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.recovery_question),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                    modifier = Modifier.padding(start = 4.dp)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { isDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedQuestion,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.keyboard_arrow_down_24px),
                                contentDescription = "Select question",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    JuneDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        questionItems.forEach { (question, _) ->
                            val isCustomOption = question == customQuestionTitle

                            JuneDropdownMenuItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                text = {
                                    Text(
                                        text = question,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isCustomOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedQuestion = question
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (selectedQuestion == customQuestionTitle) {
                Spacer(modifier = Modifier.height(24.dp))
                JuneTextField(
                    value = customQuestionText,
                    onValueChange = {
                        customQuestionText = it
                        customQuestionError = false
                    },
                    label = stringResource(R.string.custom_question_label),
                    placeholder = stringResource(R.string.custom_question_placeholder),
                    errorText = if (customQuestionError) stringResource(R.string.question_cannot_be_empty) else null
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            JuneTextField(
                value = answer,
                onValueChange = {
                    answer = it
                    answerError = false
                },
                label = stringResource(R.string.your_answer_label),
                placeholder = currentPlaceholder,
                errorText = if (answerError) stringResource(R.string.answer_cannot_be_empty) else null
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val finalQuestion = if (selectedQuestion == customQuestionTitle) {
                        customQuestionText.trim()
                    } else {
                        selectedQuestion
                    }

                    if (selectedQuestion == customQuestionTitle && finalQuestion.isEmpty()) {
                        customQuestionError = true
                    } else if (answer.trim().isEmpty()) {
                        answerError = true
                    } else {
                        val answerHash = SecurityUtils.hashAnswer(answer)
                        if (forVault) {
                            coroutineScope.launch {
                                privacyPreferences.setVaultCustomLock(
                                    type = lockType,
                                    pinHash = if (lockType == LockType.PIN) vaultPinHash else null,
                                    passwordHash = if (lockType == LockType.PASSWORD) vaultPasswordHash else null,
                                    patternHash = if (lockType == LockType.PATTERN) vaultPatternHash else null,
                                    securityQuestion = finalQuestion,
                                    securityAnswerHash = answerHash
                                )
                                android.widget.Toast.makeText(
                                    context,
                                    lockSetSuccessMsg,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                navigator.navigateBack()
                            }
                        } else {
                            onAction(SettingsAction.UpdateSecurityQuestionAndAnswer(finalQuestion, answerHash))
                            onAction(SettingsAction.UpdateLockType(lockType))
                            onAction(SettingsAction.OnAppLockToggle(true))
                            android.widget.Toast.makeText(
                                context,
                                lockSetSuccessMsg,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            navigator.navigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_and_enable_lock))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    if (forVault) {
                        coroutineScope.launch {
                            privacyPreferences.setVaultCustomLock(
                                type = lockType,
                                pinHash = if (lockType == LockType.PIN) vaultPinHash else null,
                                passwordHash = if (lockType == LockType.PASSWORD) vaultPasswordHash else null,
                                patternHash = if (lockType == LockType.PATTERN) vaultPatternHash else null,
                                securityQuestion = null,
                                securityAnswerHash = null
                            )
                            android.widget.Toast.makeText(
                                context,
                                lockSetSuccessMsg,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            navigator.navigateBack()
                        }
                    } else {
                        onAction(SettingsAction.UpdateSecurityQuestionAndAnswer(null, null))
                        onAction(SettingsAction.UpdateLockType(lockType))
                        onAction(SettingsAction.OnAppLockToggle(true))
                        android.widget.Toast.makeText(
                            context,
                            lockSetSuccessMsg,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        navigator.navigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.skip_and_enable_lock))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
