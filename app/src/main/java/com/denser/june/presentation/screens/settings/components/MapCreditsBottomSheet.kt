package com.denser.june.presentation.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.denser.june.core.R
import com.denser.june.core.utils.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapCreditsBottomSheet(
    setShowSheet: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    ModalBottomSheet(
        onDismissRequest = { setShowSheet(false) },
        modifier = modifier
    ) {
        val items = listOf(
            AttributionItem("MapLibre SDK", Constants.MAPLIBRE_URL, "Open-source map rendering engine"),
            AttributionItem("MapTiler", Constants.MAPTILER_COPYRIGHT_URL, "© MapTiler © OpenStreetMap contributors"),
            AttributionItem("Stadia Maps", Constants.STADIA_ATTRIBUTION_URL, "© Stadia Maps © OpenStreetMap contributors"),
            AttributionItem("CARTO", Constants.CARTO_ATTRIBUTION_URL, "© CARTO © OpenStreetMap contributors"),
            AttributionItem("Mapbox", Constants.MAPBOX_ATTRIBUTION_URL, "© Mapbox © OpenStreetMap contributors"),
            AttributionItem("OpenStreetMap", Constants.OPENSTREETMAP_COPYRIGHT_URL, "© OpenStreetMap contributors")
        )

        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = "Map Credits",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 4.dp)
                        .fillMaxWidth()
                )
                Text(
                    text = "June uses vector tiles and geocoding services from various open data and mapping providers.",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
            items(items.size) { index ->
                val item = items[index]
                SettingsItem(
                    title = item.name,
                    subtitle = item.credit,
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
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private data class AttributionItem(
    val name: String,
    val url: String,
    val credit: String
)
