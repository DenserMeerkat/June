package com.denser.june.presentation.screens.settings.screens
import com.denser.june.presentation.components.JuneBackButton

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.presentation.theme.FontPresets
import com.denser.june.core.domain.model.enums.FontType
import com.denser.june.core.domain.model.enums.Fonts
import com.denser.june.presentation.components.*
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.screens.settings.SettingsAction
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.components.font.FontPanel
import com.denser.june.presentation.components.font.TypographyPreview
import com.denser.june.presentation.components.plus.PlusBadge
import com.denser.june.presentation.plus.LocalIsPlus
import com.denser.june.presentation.plus.LocalPlusSheetController
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

    val savedFontName = remember(type, state.appTheme) {
        when (type) {
            FontType.APP -> state.appTheme.appFont
        }
    }
    val savedVariation = state.appTheme.appFontVariation

    var draftFontName by remember(savedFontName) {
        mutableStateOf(savedFontName)
    }
    var draftVariation by remember(savedVariation) {
        mutableStateOf(savedVariation)
    }

    LaunchedEffect(type) {
        viewModel.onAction(SettingsAction.SetFontType(type))
    }

    val isPlus = LocalIsPlus.current
    val plusSheetController = LocalPlusSheetController.current

    val isProFontSelected = remember(draftFontName) {
        draftFontName == Fonts.RECURSIVE.fullName || draftFontName == Fonts.SHANTELL_SANS.fullName
    }
    val isProStyleSelected = remember(draftFontName, draftVariation) {
        draftVariation != null && draftVariation != FontPresets.getDefaultVariation(draftFontName)
    }
    val isProSelection = isProFontSelected || isProStyleSelected

    val isDirty = draftFontName != savedFontName || draftVariation != savedVariation

    val saveFontsAction = {
        if (draftFontName != savedFontName) {
            viewModel.onAction(SettingsAction.OnFontSelect(draftFontName))
        }
        if (draftVariation != savedVariation) {
            viewModel.onAction(SettingsAction.OnAppFontVariationChange(draftVariation))
        }
    }

    Scaffold(
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Small,
                title = {
                    val title = when (type) {
                        FontType.APP -> "App Font"
                    }
                    Text(title)
                },
                navigationIcon = {
                    JuneBackButton(onClick = { navigator.navigateBack() })
                },
                actions = {
                    if (isDirty) {
                        Button(
                            onClick = {
                                if (isProSelection && !isPlus) {
                                    plusSheetController.show {
                                        saveFontsAction()
                                    }
                                } else {
                                    saveFontsAction()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            if (isProSelection && !isPlus) {
                                PlusBadge(
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            )
        }
    ) { padding ->
        val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val sheetHeight = 380.dp + 16.dp + navBottomPadding

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            TypographyPreview(
                fontName = draftFontName,
                variation = draftVariation,
                contentBottomPadding = sheetHeight,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
            )


            FontPanel(
                selectedFontName = draftFontName,
                currentVariation = draftVariation,
                onFontSelect = { newFont ->
                    if (newFont != null) {
                        draftFontName = newFont
                        draftVariation = FontPresets.getDefaultVariation(newFont)
                    }
                },
                onVariationChange = { draftVariation = it },
                contentHeight = 380.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
            )
        }
    }
}
