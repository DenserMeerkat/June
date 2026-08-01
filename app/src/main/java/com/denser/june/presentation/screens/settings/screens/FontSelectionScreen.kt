package com.denser.june.presentation.screens.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.FontType
import com.denser.june.presentation.components.*
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.settings.components.FontSelector
import com.denser.june.presentation.theme.LocalInternetAllowed
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSelectionScreen(
    type: FontType = FontType.APP
) {
    val viewModel: SettingsVM = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = koinInject<AppNavigator>()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isInternetAllowed = LocalInternetAllowed.current

    LaunchedEffect(type) {
        viewModel.onAction(SettingsAction.SetFontType(type))
    }

    val currentFontName = remember(type, state.appTheme) {
        when (type) {
            FontType.APP -> state.appTheme.appFont
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                scrollBehavior = scrollBehavior,
                title = {
                    val title = when (type) {
                        FontType.APP -> "App Font"
                    }
                    Text(title)
                },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            if (!isInternetAllowed) {
                InternetRestrictedBanner(
                    description = stringResource(R.string.online_fonts_require_internet),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            FontSelector(
                selectedFontName = currentFontName,
                onFontSelect = { viewModel.onAction(SettingsAction.OnFontSelect(it)) },
                modifier = Modifier.fillMaxSize(),
                bottomPadding = padding.calculateBottomPadding()
            )
        }
    }
}
