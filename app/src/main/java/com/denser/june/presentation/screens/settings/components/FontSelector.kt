package com.denser.june.presentation.screens.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denser.june.core.R
import com.denser.june.core.domain.model.enums.Fonts
import com.denser.june.core.domain.model.enums.FontCategory
import com.denser.june.presentation.components.JuneRadioTile
import com.denser.june.presentation.components.JuneRadioTilePosition
import com.denser.june.presentation.theme.googleFontsMetadata
import com.denser.june.presentation.theme.getAppFontFamily

@Composable
fun FontSelector(
    modifier: Modifier = Modifier,
    selectedFontName: String,
    onFontSelect: (String) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    var selectedCategory by remember { mutableStateOf<FontCategory?>(null) }

    val filteredMetadata = remember(selectedCategory) {
        googleFontsMetadata.filter { metadata ->
            selectedCategory == null || metadata.category == selectedCategory
        }
    }

    val bundledFonts = remember { Fonts.entries }
    val allFontNames = remember(filteredMetadata, selectedCategory) {
        val bundledMatch = bundledFonts.filter { selectedCategory == null || it.category == selectedCategory }
        bundledMatch.map { it.fullName } + filteredMetadata.map { it.name }
    }

    val selectedFontFamily = getAppFontFamily(selectedFontName)

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = selectedFontName,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "The quick brown fox jumps over the lazy dog",
                    style = TextStyle(
                        fontFamily = selectedFontFamily,
                        fontSize = 20.sp,
                        lineHeight = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text(stringResource(R.string.all)) }
                )
            }
            items(FontCategory.entries) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.displayName) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = 8.dp + bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selectedCategory == null) {
                item {
                    Text(
                        text = "All Fonts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            items(allFontNames, key = { it }) { name ->
                val isBundled = remember(name) { bundledFonts.any { it.fullName == name } }
                val fontFamily = getAppFontFamily(name)
                JuneRadioTile(
                    selected = name == selectedFontName,
                    title = name,
                    badge = if (isBundled) "Bundled" else null,
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
}
