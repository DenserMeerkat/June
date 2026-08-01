package com.denser.june.presentation.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.utils.SecurityUtils
import com.denser.june.presentation.utils.SecurityQuestionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinRecoveryScreen(
    question: String,
    storedAnswerHash: String,
    onBackClick: () -> Unit,
    onPinResetSuccess: () -> Unit
) {
    val context = LocalContext.current
    BackHandler {
        onBackClick()
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                title = { Text(stringResource(R.string.reset_pin)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        onClick = onBackClick,
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
                }
            )
        }
    ) { padding ->
        var answerInput by remember { mutableStateOf("") }
        var isAnswerError by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.pin_recovery_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            val displayedQuestion = remember(question) {
                SecurityQuestionUtils.resolveLocalizedQuestion(context, question)
            }

            Text(
                text = displayedQuestion,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            JuneTextField(
                value = answerInput,
                onValueChange = {
                    answerInput = it
                    isAnswerError = false
                },
                label = stringResource(R.string.answer_label),
                placeholder = stringResource(R.string.answer_placeholder),
                errorText = if (isAnswerError) stringResource(R.string.incorrect_answer_try_again) else null
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val inputHash = SecurityUtils.hashAnswer(answerInput)
                    if (inputHash == storedAnswerHash) {
                        onPinResetSuccess()
                        Toast.makeText(context, context.getString(R.string.pin_reset_successfully), Toast.LENGTH_LONG).show()
                    } else {
                        isAnswerError = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.verify_and_reset_pin))
            }
        }
    }
}
