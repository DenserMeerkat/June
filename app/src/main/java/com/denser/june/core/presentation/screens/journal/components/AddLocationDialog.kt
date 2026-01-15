package com.denser.june.core.presentation.screens.journal.components

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.denser.june.R
import com.denser.june.core.domain.data_classes.JournalLocation
import com.denser.june.core.presentation.components.MapControlColumn
import com.denser.june.core.presentation.components.MapLocationPin
import com.denser.june.core.presentation.components.MapTilerAttribution
import com.denser.june.core.presentation.utils.MapTilerUtils
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
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
    isEditMode: Boolean = true,
    onLocationSelected: (JournalLocation) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    remember { MapLibre.getInstance(context) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var isAddressLoading by remember { mutableStateOf(false) }

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

    val initialMapTheme = isSystemInDarkTheme()
    var isMapDarkMode by remember { mutableStateOf(initialMapTheme) }
    val mapStyleUrl = remember(isMapDarkMode) {
        if (isMapDarkMode) MapTilerUtils.STYLE_DARK else MapTilerUtils.STYLE_LIGHT
    }

    LaunchedEffect(cameraState.position.target) {
        if (isEditMode) {
            isAddressLoading = true
            delay(800)
            val target = cameraState.position.target
            if (target.latitude != 0.0 || target.longitude != 0.0) {
                val latLng = LatLng(target.latitude, target.longitude)
                currentLocation = MapTilerUtils.updateLocationFromCenter(context, latLng)
            }
            isAddressLoading = false
        }
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
            val location = MapTilerUtils.fetchCurrentLocation(context)
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
        if (existingLocation == null && isEditMode) {
            onMyLocationClick()
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

            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (isEditMode) {
                    MapSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = {
                            focusManager.clearFocus()
                            scope.launch {
                                isSearching = true
                                MapTilerUtils.searchLocation(context, searchQuery)?.let { result ->
                                    animateToLocation(LatLng(result.latitude, result.longitude))
                                }
                                isSearching = false
                            }
                        },
                        isSearching = isSearching,
                        onBack = onDismiss
                    )
                } else {
                    FilledIconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(painterResource(R.drawable.close_24px), "Close")
                    }
                }
            }
            Box(modifier = Modifier.align(Alignment.Center)) {
                MapLocationPin(
                    isMoving = isAddressLoading
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MapControlColumn(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 16.dp),
                    isDarkMode = isMapDarkMode,
                    onToggleDarkMode = { isMapDarkMode = !isMapDarkMode },
                    isFetchingLocation = isFetchingLocation,
                    onMyLocationClick = if (isEditMode) onMyLocationClick else null,
                    isTerrainMode = false,
                    onToggleTerrain = {},
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
                MapTilerAttribution(
                    modifier = Modifier.padding(start = 8.dp),
                    isDarkMode = isMapDarkMode
                )
                MapBottomBar(
                    location = currentLocation,
                    isLoading = isAddressLoading,
                    isEditMode = isEditMode,
                    isAtTarget = isAtSelectedLocation,
                    onLocationIconClick = {
                        existingLocation?.let {
                            animateToLocation(LatLng(it.latitude, it.longitude))
                        }
                    },
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
fun MapBottomBar(
    location: JournalLocation,
    isLoading: Boolean,
    isEditMode: Boolean,
    isAtTarget: Boolean,
    onLocationIconClick: (() -> Unit)? = null,
    onConfirm: () -> Unit
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
                        text = if (isLoading && isEditMode) "Locating..." else (location.name
                            ?: "Unknown Place"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!isLoading) {
                        Text(
                            text = location.address ?: "No address available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (isAtTarget) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(48.dp),
                    onClick = { onLocationIconClick?.invoke() },
                    enabled = !isEditMode && onLocationIconClick != null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading && isEditMode) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            val icon = when {
                                isEditMode -> R.drawable.add_location_24px
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
                        text = "${
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
                        text = if (isEditMode) "GPS • ±5m" else "Saved Location",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            if (isEditMode) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !isLoading
                ) {
                    Text(
                        "Confirm Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
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
                    disabledContainerColor = Color.Transparent,
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