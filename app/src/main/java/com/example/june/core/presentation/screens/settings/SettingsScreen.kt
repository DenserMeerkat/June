package com.example.june.core.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.june.R
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.Route
import com.example.june.core.presentation.components.JuneAppBarType
import com.example.june.core.presentation.components.JuneTopAppBar
import org.koin.compose.koinInject

import com.example.june.core.presentation.screens.settings.section.AppearanceSection
import com.example.june.core.presentation.screens.settings.section.GeneralSection
import com.example.june.core.presentation.screens.settings.section.SettingSection
import com.example.june.core.presentation.screens.settings.section.SettingsItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
) {
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
                .clip(RoundedCornerShape(24.dp))
        ) {
            item(key = "general_section") {
                GeneralSection(
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
                SettingSection(
                    title = "About"
                ) {
                    SettingsItem(
                        title = stringResource(R.string.about_libraries),
                        subtitle = stringResource(R.string.about_libraries),
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.info_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        onClick = { navigator.navigateTo(Route.AboutLibraries) }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}