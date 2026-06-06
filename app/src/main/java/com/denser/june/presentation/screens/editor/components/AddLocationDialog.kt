package com.denser.june.presentation.screens.editor.components

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.denser.june.presentation.components.JuneFullScreenDialog
import androidx.core.content.ContextCompat
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import com.denser.june.core.R
import com.denser.june.core.domain.model.JournalLocation
import com.denser.june.core.domain.model.enums.MapTheme
import com.denser.june.presentation.components.MapControlColumn
import com.denser.june.presentation.components.MapLocationPin
import com.denser.june.presentation.components.MapAttributions
import com.denser.june.presentation.components.InternetRestrictedBanner
import com.denser.june.presentation.components.MapLibreInitializer
import com.denser.june.presentation.components.rememberMapDarkMode
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.domain.preferences.PrivacyPreferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import com.denser.june.presentation.utils.MapProviderUtils
import com.denser.june.presentation.utils.MapSearchResult
import com.denser.june.core.domain.model.enums.MapStyleProvider
import com.denser.june.presentation.utils.UiUtils
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.map.GestureOptions
import org.maplibre.spatialk.geojson.Position
import org.maplibre.android.geometry.LatLng
import org.maplibre.compose.style.BaseStyle
import java.util.Locale

@Composable
fun AddLocationDialog(
    existingLocation: JournalLocation? = null,
    onLocationSelected: (JournalLocation) -> Unit = {},
    onRemoveLocation: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val privacyPreferences = koinInject<PrivacyPreferences>()
    val journalPreferences = koinInject<JournalPreferences>()
    val isInternetAllowed by privacyPreferences.getIsInternetAllowedFlow()
        .collectAsStateWithLifecycle(initialValue = true)
    val savedMapTheme by journalPreferences.mapTheme()
        .collectAsStateWithLifecycle(initialValue = MapTheme.APP)

    val mapStyleProvider by journalPreferences.mapStyleProvider()
        .collectAsStateWithLifecycle(initialValue = MapStyleProvider.MAPTILER)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    MapLibreInitializer(isInternetAllowed)

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var isAddressLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<MapSearchResult>>(emptyList()) }
    var isSearchFocused by remember { mutableStateOf(false) }

    var currentLocation by remember {
        mutableStateOf(existingLocation ?: JournalLocation(0.0, 0.0, name = "Move map to select"))
    }

    val startPosition = remember {
        if (existingLocation != null && existingLocation.latitude != 0.0) {
            CameraPosition(
                target = Position(
                    longitude = existingLocation.longitude,
                    latitude = existingLocation.latitude
                ),
                zoom = 15.0
            )
        } else {
            CameraPosition(target = Position(longitude = 0.0, latitude = 0.0), zoom = 1.0)
        }
    }
    val cameraState = rememberCameraState(firstPosition = startPosition)

    val isAtSelectedLocation by remember(cameraState.position.target, existingLocation) {
        derivedStateOf {
            existingLocation?.let { existing ->
                val target = cameraState.position.target
                val latDiff = abs(target.latitude - existing.latitude)
                val lonDiff = abs(target.longitude - existing.longitude)
                latDiff < 0.00001 && lonDiff < 0.00001
            } ?: false
        }
    }

    var isMapDarkMode by rememberMapDarkMode(savedMapTheme)
    val mapStyleUrl by produceState("", mapStyleProvider, isMapDarkMode) {
        value = MapProviderUtils.getStyleUrl(
            provider = mapStyleProvider,
            isDark = isMapDarkMode
        )
    }

    val lastFetchedLatLng = remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(cameraState.position.target, isInternetAllowed) {
        val target = cameraState.position.target
        if (target.latitude == 0.0 && target.longitude == 0.0) return@LaunchedEffect

        val newLatLng = LatLng(target.latitude, target.longitude)
        val lastFetched = lastFetchedLatLng.value

        if (lastFetched != null) {
            val latDiff = abs(newLatLng.latitude - lastFetched.latitude)
            val lngDiff = abs(newLatLng.longitude - lastFetched.longitude)
            if (latDiff < 1e-5 && lngDiff < 1e-5) {
                return@LaunchedEffect
            }
        }

        isAddressLoading = true
        delay(800)
        currentLocation = MapProviderUtils.updateLocationFromCenter(context, newLatLng)
        lastFetchedLatLng.value = newLatLng
        isAddressLoading = false
    }

    fun animateToLocation(latLng: LatLng, zoom: Double = 15.0) {
        scope.launch {
            cameraState.animateTo(
                finalPosition = CameraPosition(
                    target = Position(longitude = latLng.longitude, latitude = latLng.latitude),
                    zoom = zoom
                ),
                duration = 1.5.seconds
            )
        }
    }

    fun performLocationFetch() {
        isFetchingLocation = true
        scope.launch {
            val location = MapProviderUtils.fetchCurrentLocation(context)
            if (location != null && (location.latitude != 0.0 || location.longitude != 0.0)) {
                animateToLocation(LatLng(location.latitude, location.longitude))
            }
            isFetchingLocation = false
        }
    }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) performLocationFetch()
        else isFetchingLocation = false
    }

    fun checkSettingsAndFetch() {
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(context)

        client.checkLocationSettings(builder.build())
            .addOnSuccessListener { performLocationFetch() }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        locationSettingsLauncher.launch(
                            IntentSenderRequest.Builder(exception.resolution).build()
                        )
                    } catch (e: Exception) {
                        isFetchingLocation = false
                    }
                } else isFetchingLocation = false
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) checkSettingsAndFetch() else isFetchingLocation = false
    }

    val onMyLocationClick = {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            checkSettingsAndFetch()
        } else {
            isFetchingLocation = true
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(Unit) {
        if (existingLocation == null && isInternetAllowed) {
            onMyLocationClick()
        }
    }

    val dialogBgColor = if (isMapDarkMode) Color(0xFF121212) else Color(0xFFFFFFFF)
    JuneFullScreenDialog(
        onDismissRequest = onDismiss,
        isDarkTheme = isMapDarkMode,
        windowBackgroundColor = dialogBgColor
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = dialogBgColor
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
            if (isInternetAllowed) {
                MaplibreMap(
                    modifier = Modifier.fillMaxSize(),
                    baseStyle = BaseStyle.Uri(mapStyleUrl),
                    cameraState = cameraState,
                    options = MapOptions(
                        ornamentOptions = OrnamentOptions(
                            isLogoEnabled = false,
                            isAttributionEnabled = false,
                            isCompassEnabled = false,
                            isScaleBarEnabled = false
                        ),
                        gestureOptions = GestureOptions(
                            isTiltEnabled = true,
                            isZoomEnabled = true,
                            isRotateEnabled = true,
                            isScrollEnabled = true
                        )
                    )
                )
            }

            if (isInternetAllowed) {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    MapLocationPin(
                        isMoving = isAddressLoading
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isInternetAllowed) {
                    MapControlColumn(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(end = 16.dp),
                        isDarkMode = isMapDarkMode,
                        onToggleDarkMode = { isMapDarkMode = !isMapDarkMode },
                        isFetchingLocation = isFetchingLocation,
                        onMyLocationClick = onMyLocationClick,
                        onZoomIn = {
                            scope.launch {
                                val currentPos = cameraState.position
                                cameraState.animateTo(
                                    currentPos.copy(zoom = currentPos.zoom + 1),
                                    300.milliseconds
                                )
                            }
                        },
                        onZoomOut = {
                            scope.launch {
                                val currentPos = cameraState.position
                                cameraState.animateTo(
                                    currentPos.copy(zoom = currentPos.zoom - 1),
                                    300.milliseconds
                                )
                            }
                        }
                    )
                    MapAttributions(
                        provider = mapStyleProvider,
                        modifier = Modifier.padding(start = 8.dp),
                        isDarkMode = isMapDarkMode
                    )
                }

                MapBottomBar(
                    location = if (!isInternetAllowed && currentLocation.name == "Move map to select") {
                        currentLocation.copy(name = "Network restricted")
                    } else currentLocation,
                    isLoading = isAddressLoading,
                    isAtTarget = isAtSelectedLocation,
                    onLocationIconClick = if (isInternetAllowed) {
                        {
                            existingLocation?.let {
                                animateToLocation(LatLng(it.latitude, it.longitude))
                            }
                        }
                    } else null,
                    onConfirm = {
                        onLocationSelected(currentLocation)
                        onDismiss()
                    },
                    onRemove = if (existingLocation != null) {
                        {
                            onRemoveLocation?.invoke()
                            onDismiss()
                        }
                    } else null,
                    enabled = isInternetAllowed
                )
            }

            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapSearchBar(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        if (it.isBlank()) {
                            searchResults = emptyList()
                        }
                    },
                    onSearch = {
                        focusManager.clearFocus()
                        scope.launch {
                            isSearching = true
                            searchResults = MapProviderUtils.searchLocation(context, searchQuery)
                            isSearching = false
                        }
                    },
                    isSearching = isSearching,
                    onBack = onDismiss,
                    enabled = isInternetAllowed,
                    onFocusChanged = { isSearchFocused = it }
                )

                if (isSearchFocused && searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            searchResults.forEach { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus()
                                            animateToLocation(LatLng(result.latitude, result.longitude))
                                            searchQuery = result.name
                                            searchResults = emptyList()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.location_on_24px),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = result.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = result.address,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isInternetAllowed) {
                    InternetRestrictedBanner(
                        description = "Maps and search require internet access."
                    )
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MapBottomBar(
    location: JournalLocation,
    isLoading: Boolean,
    isAtTarget: Boolean,
    onLocationIconClick: (() -> Unit)? = null,
    onConfirm: () -> Unit,
    onRemove: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(24.dp, 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLoading) "Locating..." else (location.name ?: "Unknown Place"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = if (isLoading) "Fetching address..." else (location.address ?: "No address available"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isLoading) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isAtTarget) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(48.dp),
                    onClick = { onLocationIconClick?.invoke() },
                    enabled = onLocationIconClick != null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            ContainedLoadingIndicator(
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            val icon = when {
                                isAtTarget -> R.drawable.location_on_24px_fill
                                else -> R.drawable.explore_24px
                            }
                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null,
                                tint = if (isAtTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.explore_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLoading) "Locating..." else "${
                            String.format(
                                Locale.US,
                                "%.5f",
                                location.latitude
                            )
                        }, ${String.format(Locale.US, "%.5f", location.longitude)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.my_location_24px_fill),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "GPS • ±5m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onRemove != null) {
                    FilledIconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(56.dp)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !isLoading && enabled,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.delete_24px),
                            contentDescription = "Remove Location",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !isLoading && enabled
                ) {
                    Text(
                        "Confirm Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MapSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    isSearching: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {}
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
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        onFocusChanged(focusState.isFocused)
                    },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors = UiUtils.getTransparentTextFieldColors(),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onSearch, enabled = !isSearching && enabled) {
                if (isSearching) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.search_24px),
                        contentDescription = "Search",
                        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}