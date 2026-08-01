package com.denser.june.presentation.screens.settings.screens

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverySetupScreen() {
    val settingsVM: SettingsVM = koinViewModel()
    val state = settingsVM.state.collectAsStateWithLifecycle().value
    val onAction = settingsVM::onAction
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current

    val customQuestionTitle = stringResource(R.string.question_custom)
    val predefinedItems = remember { SecurityQuestionUtils.PREDEFINED_QUESTIONS }

    val questionItems = predefinedItems.map {
        stringResource(it.questionResId) to stringResource(it.placeholderResId)
    } + (customQuestionTitle to stringResource(R.string.placeholder_default_answer))

    val predefinedIndex = remember(state.securityQuestion) {
        SecurityQuestionUtils.findPredefinedQuestionIndex(context, state.securityQuestion)
    }
    val isCustomStored = remember(state.securityQuestion, predefinedIndex) {
        state.securityQuestion != null && predefinedIndex == null
    }

    var selectedQuestion by remember(state.securityQuestion, predefinedIndex, customQuestionTitle) {
        mutableStateOf(
            if (isCustomStored) customQuestionTitle
            else if (predefinedIndex != null) questionItems[predefinedIndex].first
            else questionItems.first().first
        )
    }
    var customQuestionText by remember(state.securityQuestion, isCustomStored) {
        mutableStateOf(if (isCustomStored) state.securityQuestion ?: "" else "")
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
                actions = {
                    OutlinedButton(
                        onClick = {
                            onAction(SettingsAction.UpdateSecurityQuestionAndAnswer(null, null))
                            onAction(SettingsAction.UpdateLockType(LockType.PIN))
                            onAction(SettingsAction.OnAppLockToggle(true))
                            navigator.navigateBack()
                        }
                    ) {
                        Text(stringResource(R.string.skip), color = MaterialTheme.colorScheme.primary)
                    }
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

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        questionItems.forEach { (question, _) ->
                            val isCustomOption = question == customQuestionTitle

                            DropdownMenuItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp)),
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
                        onAction(SettingsAction.UpdateSecurityQuestionAndAnswer(finalQuestion, answerHash))
                        onAction(SettingsAction.UpdateLockType(LockType.PIN))
                        onAction(SettingsAction.OnAppLockToggle(true))
                        navigator.navigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_and_enable_lock))
            }
        }
    }
}
