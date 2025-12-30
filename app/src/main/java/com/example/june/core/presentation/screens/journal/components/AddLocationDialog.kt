package com.example.june.core.presentation.screens.journal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.june.R
import com.example.june.core.domain.data_classes.JournalLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.net.URL

@Composable
fun AddLocationDialog(
    existingLocation: JournalLocation? = null,
    onLocationSelected: (JournalLocation) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isMapMoving by remember { mutableStateOf(false) }
    var reverseGeocodeJob by remember { mutableStateOf<Job?>(null) }

    var currentLocation by remember {
        mutableStateOf(existingLocation ?: JournalLocation(0.0, 0.0, name = "Searching..."))
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            controller.setZoom(if (existingLocation != null) 17.0 else 3.0)
            minZoomLevel = 3.0
        }
    }

    DisposableEffect(mapView) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                isMapMoving = true
                reverseGeocodeJob?.cancel()

                reverseGeocodeJob = scope.launch {
                    delay(800)
                    isMapMoving = false
                    updateLocationFromCenter(mapView.mapCenter as GeoPoint) { loc ->
                        currentLocation = loc
                    }
                }
                return true
            }
            override fun onZoom(event: ZoomEvent?): Boolean = true
        }
        mapView.addMapListener(listener)
        onDispose {
            mapView.removeMapListener(listener)
            mapView.onDetach()
        }
    }

    LaunchedEffect(Unit) {
        val startPoint = existingLocation?.let { GeoPoint(it.latitude, it.longitude) }
            ?: GeoPoint(0.0, 0.0)

        mapView.controller.setCenter(startPoint)

        if (existingLocation == null) {
            currentLocation = JournalLocation(0.0, 0.0, name = "Move map to select")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OsmSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        focusManager.clearFocus()
                        scope.launch {
                            isSearching = true
                            val result = searchNominatim(searchQuery)
                            if (result != null) {
                                mapView.controller.animateTo(result)
                                mapView.controller.setZoom(17.0)
                                updateLocationFromCenter(result) { currentLocation = it }
                            }
                            isSearching = false
                        }
                    },
                    isSearching = isSearching,
                    onBack = onDismiss
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-24).dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.location_on_24px_fill),
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.25f),
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = 4.dp)
                )
                Icon(
                    painter = painterResource(R.drawable.location_on_24px_fill),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                OsmLocationCard(
                    location = currentLocation,
                    isLoading = isMapMoving,
                    onConfirm = {
                        onLocationSelected(currentLocation)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OsmSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(R.drawable.arrow_back_24px),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "Search location...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onSearch, enabled = !isSearching) {
                if (isSearching) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.search_24px),
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OsmLocationCard(
    location: JournalLocation,
    isLoading: Boolean,
    onConfirm: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Icon(
                                painterResource(R.drawable.location_on_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLoading) "Locating..." else (location.name ?: "Pinned Location"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isLoading) "Fetching address..." else (location.locality ?: location.address ?: "No address"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = !isLoading
            ) {
                Text(
                    "Confirm Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private suspend fun searchNominatim(query: String): GeoPoint? = withContext(Dispatchers.IO) {
    try {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=1"
        val connection = URL(url).openConnection()
        connection.setRequestProperty("User-Agent", "JuneApp/1.0")
        connection.connectTimeout = 5000
        val response = connection.getInputStream().bufferedReader().readText()
        val jsonArray = JSONArray(response)
        if (jsonArray.length() > 0) {
            val obj = jsonArray.getJSONObject(0)
            GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"))
        } else null
    } catch (e: Exception) {
        null
    }
}

private suspend fun updateLocationFromCenter(
    center: GeoPoint,
    onResult: (JournalLocation) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val url = "https://nominatim.openstreetmap.org/reverse?lat=${center.latitude}&lon=${center.longitude}&format=json"
        val connection = URL(url).openConnection()
        connection.setRequestProperty("User-Agent", "JuneApp/1.0")
        connection.connectTimeout = 5000
        val response = connection.getInputStream().bufferedReader().readText()
        val json = org.json.JSONObject(response)

        val addressObj = json.optJSONObject("address")
        val displayName = json.optString("display_name", "Unknown Location")

        val city = addressObj?.optString("city")
            ?: addressObj?.optString("town")
            ?: addressObj?.optString("village")
            ?: addressObj?.optString("state")

        val specificName = addressObj?.optString("road")
            ?: addressObj?.optString("suburb")
            ?: addressObj?.optString("neighbourhood")
            ?: "Pinned Location"

        withContext(Dispatchers.Main) {
            onResult(
                JournalLocation(
                    latitude = center.latitude,
                    longitude = center.longitude,
                    name = specificName,
                    address = displayName,
                    locality = city
                )
            )
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            onResult(
                JournalLocation(
                    latitude = center.latitude,
                    longitude = center.longitude,
                    name = "Pinned Location",
                    address = "Lat: ${String.format("%.4f", center.latitude)}, Lon: ${String.format("%.4f", center.longitude)}"
                )
            )
        }
    }
}