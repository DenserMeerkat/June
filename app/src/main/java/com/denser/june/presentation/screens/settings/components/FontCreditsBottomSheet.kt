package com.denser.june.presentation.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.Fonts
import com.denser.june.presentation.components.JuneConnectedItem
import com.denser.june.presentation.components.JuneConnectedList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontCreditsBottomSheet(
    setShowSheet: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    ModalBottomSheet(
        onDismissRequest = { setShowSheet(false) },
        modifier = modifier
    ) {
        val fontCredits = listOf(
            FontCreditItem(
                font = Fonts.GOOGLE_SANS_FLEX,
                designer = "Google",
                license = "SIL Open Font License 1.1",
                url = "https://github.com/google/fonts"
            ),
            FontCreditItem(
                font = Fonts.ROBOTO_FLEX,
                designer = "David Berlow, Santiago Orozco, Font Bureau, Christian Robertson",
                license = "SIL Open Font License 1.1",
                url = "https://github.com/googlefonts/roboto-flex"
            ),
            FontCreditItem(
                font = Fonts.RECURSIVE,
                designer = "Stephen Nixon (Arrow Type)",
                license = "SIL Open Font License 1.1",
                url = "https://www.recursive.design/"
            ),
            FontCreditItem(
                font = Fonts.SHANTELL_SANS,
                designer = "Shantell Martin, Stephen Nixon, Anya Danilova",
                license = "SIL Open Font License 1.1",
                url = "https://shantellsans.com/"
            )
        )

        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.font_credits),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 4.dp)
                        .fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.font_credits_body),
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
            item {
                JuneConnectedList {
                    fontCredits.forEach { item ->
                        JuneConnectedItem(
                            title = item.font.fullName,
                            subtitle = item.designer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            trailingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.open_in_new_24px),
                                    contentDescription = "Open Link",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = { uriHandler.openUri(item.url) }
                        ) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.license,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private data class FontCreditItem(
    val font: Fonts,
    val designer: String,
    val license: String,
    val url: String
)
