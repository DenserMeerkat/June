package com.denser.june.presentation.components.font

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.Fonts
import com.denser.june.core.utils.FontUtils
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneRadioTile
import com.denser.june.presentation.components.JuneRadioTilePosition
import com.denser.june.presentation.theme.SYSTEM_FONT_NAME
import com.denser.june.presentation.theme.getAppFontFamily
import com.denser.june.presentation.theme.googleFontsMetadata

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FontSelector(
    modifier: Modifier = Modifier,
    selectedFontName: String?,
    onFontSelect: (String?) -> Unit,
    showAppDefault: Boolean = false,
    bottomPadding: Dp = 0.dp
) {
    val bundledFonts = remember { Fonts.entries }
    val allFontNames = remember {
        listOf(SYSTEM_FONT_NAME) + bundledFonts.map { it.fullName } + googleFontsMetadata.map { it.name }
    }

    val isJournalFontActive = selectedFontName != null
    val fontsEnabled = !showAppDefault || isJournalFontActive

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = 8.dp + bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showAppDefault) {
            item(key = "__app_font_switch__") {
                JuneConnectedItem(
                    title = if (isJournalFontActive) {
                        stringResource(R.string.custom_font)
                    } else {
                        stringResource(R.string.default_font)
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentPadding = PaddingValues(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = {
                        if (isJournalFontActive) {
                            onFontSelect(null)
                        } else {
                            onFontSelect(Fonts.GOOGLE_SANS_FLEX.fullName)
                        }
                    },
                    trailingContent = {
                        Switch(
                            checked = isJournalFontActive,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    onFontSelect(Fonts.GOOGLE_SANS_FLEX.fullName)
                                } else {
                                    onFontSelect(null)
                                }
                            }
                        )
                    }
                )
            }
        }

        items(allFontNames, key = { it }) { name ->
            val fontFamily = getAppFontFamily(name)
            val isVariableFont = remember(name) {
                FontUtils.isVariableFont(name)
            }

            JuneRadioTile(
                selected = fontsEnabled && name == selectedFontName,
                enabled = fontsEnabled,
                title = name,
                badge = null,
                badgeContent = if (isVariableFont) {
                    {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(
                                alpha = if (fontsEnabled) 0.5f else 0.25f
                            ),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.tune_24px),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                    alpha = if (fontsEnabled) 1f else 0.38f
                                )
                            )
                        }
                    }
                } else null,
                titleTextStyle = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = 18.sp
                ),
                radioPosition = JuneRadioTilePosition.Trailing,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                onClick = { onFontSelect(name) }
            )
        }
    }
}

