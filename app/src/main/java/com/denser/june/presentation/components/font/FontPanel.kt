package com.denser.june.presentation.components.font

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import com.denser.june.core.R
import com.denser.june.core.domain.model.FontVariationConfig
import com.denser.june.core.utils.FontUtils
import com.denser.june.presentation.components.JuneFloatingAction
import com.denser.june.presentation.components.JuneFloatingActionBar

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FontPanel(
    selectedFontName: String?,
    currentVariation: FontVariationConfig?,
    onFontSelect: (String?) -> Unit,
    onVariationChange: (FontVariationConfig?) -> Unit,
    modifier: Modifier = Modifier,
    showAppDefault: Boolean = false,
    contentHeight: Dp = 380.dp,
    initialTab: FontTab = FontTab.FONTS
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val floatingBarBottomInset = 64.dp + navBottomPadding
    val totalHeight = contentHeight + 16.dp + navBottomPadding

    val isVariableFont = FontUtils.isVariableFont(selectedFontName)

    LaunchedEffect(isVariableFont) {
        if (!isVariableFont && selectedTab == FontTab.STYLE) {
            selectedTab = FontTab.FONTS
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = sheetShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                when (selectedTab) {
                    FontTab.FONTS -> {
                        FontListTab(
                            selectedFontName = selectedFontName,
                            onFontSelect = onFontSelect,
                            showAppDefault = showAppDefault,
                            modifier = Modifier.fillMaxSize(),
                            bottomPadding = floatingBarBottomInset
                        )
                    }
                    FontTab.STYLE -> {
                        FontAxesTab(
                            fontName = selectedFontName ?: "",
                            currentVariation = currentVariation,
                            onVariationChange = onVariationChange,
                            modifier = Modifier.fillMaxSize(),
                            bottomPadding = floatingBarBottomInset
                        )
                    }
                }
            }
        }

        JuneFloatingActionBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            FontTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val isTabEnabled = tab == FontTab.FONTS || isVariableFont
                val label = when (tab) {
                    FontTab.FONTS -> stringResource(R.string.fonts)
                    FontTab.STYLE -> stringResource(R.string.style)
                }
                val iconRes = when (tab) {
                    FontTab.FONTS -> R.drawable.format_size_24px
                    FontTab.STYLE -> R.drawable.tune_24px
                }

                JuneFloatingAction(
                    selected = isSelected,
                    enabled = isTabEnabled,
                    onClick = { if (isTabEnabled) selectedTab = tab },
                    label = label,
                    icon = if (isSelected) {
                        {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

