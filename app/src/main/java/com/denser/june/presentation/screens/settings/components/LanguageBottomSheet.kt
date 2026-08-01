package com.denser.june.presentation.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.utils.LanguageHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageBottomSheet(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val currentCode = remember { LanguageHelper.getCurrentLanguageCode() }
    var selectedCode by remember { mutableStateOf(currentCode) }
    val supportedLanguages = remember(context) { LanguageHelper.getSupportedLanguages(context) }
    val systemDefault = supportedLanguages.firstOrNull { it.code.isEmpty() } ?: supportedLanguages.first()
    val otherLanguages = supportedLanguages.filter { it.code.isNotEmpty() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    val isSystemSelected = selectedCode.isEmpty()
                    val shape = RoundedCornerShape(16.dp)
                    ListItem(
                        headlineContent = {
                            Text(
                                text = systemDefault.nativeName,
                                fontWeight = if (isSystemSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSystemSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingContent = {
                            if (isSystemSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.check_24px),
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSystemSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .clickable {
                                selectedCode = ""
                                scope.launch {
                                    delay(150.milliseconds)
                                    onLanguageSelected("")
                                    sheetState.hide()
                                    onDismiss()
                                }
                            }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                itemsIndexed(otherLanguages, key = { _, item -> item.code }) { index, language ->
                    val isSelected = language.code.equals(selectedCode, ignoreCase = true)
                    val shape = when {
                        otherLanguages.size == 1 -> RoundedCornerShape(16.dp)
                        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        index == otherLanguages.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                        else -> RoundedCornerShape(4.dp)
                    }

                    ListItem(
                        headlineContent = {
                            Text(
                                text = language.nativeName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingContent = {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.check_24px),
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .clickable {
                                selectedCode = language.code
                                scope.launch {
                                    delay(150.milliseconds)
                                    onLanguageSelected(language.code)
                                    sheetState.hide()
                                    onDismiss()
                                }
                            }
                    )
                }
            }
        }
    }
}
