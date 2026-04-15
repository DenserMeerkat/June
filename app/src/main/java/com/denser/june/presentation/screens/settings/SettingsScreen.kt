package com.denser.june.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.screens.settings.section.PrivacySecuritySection
import com.denser.june.presentation.screens.settings.section.SyncBackupSection
import com.denser.june.presentation.screens.settings.section.JournalSection
import com.denser.june.presentation.screens.settings.section.AboutSection
import com.denser.june.presentation.screens.settings.section.AppearanceSection
import org.koin.compose.koinInject
import com.denser.june.core.R
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
) {
    val settingsVM: SettingsVM = koinViewModel()
    val state by settingsVM.state.collectAsStateWithLifecycle()
    val onAction = settingsVM::onAction
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = "Settings") },
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
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item(key = "privacy_security_section") {
                PrivacySecuritySection(
                    state = state,
                    onAction = onAction
                )
            }
            item(key = "sync_backup_section") {
                SyncBackupSection(
                    state = state,
                    onAction = onAction
                )
            }
            item(key = "journal_section") {
                JournalSection(
                    state = state,
                    onAction = onAction
                )
            }
            item(key = "appearance_section") {
                AppearanceSection(
                    state = state,
                    onAction = onAction
                )
            }
            item(key = "about_section") {
                AboutSection()
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
