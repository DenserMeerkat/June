package com.example.june.core.presentation.screens.journal.components

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import com.example.june.R
import com.example.june.core.domain.data_classes.JournalLocation
import com.example.june.core.presentation.utils.OsmMapUtils
import com.example.june.core.presentation.utils.rememberManagedOsmMapView
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

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

    var isDarkMode by remember { mutableStateOf(false) }
    var isTerrainMode by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isMapMoving by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var currentLocation by remember {
        mutableStateOf(existingLocation ?: JournalLocation(0.0, 0.0, name = "Move map to select"))
    }

    val mapView = rememberManagedOsmMapView(
        initialLocation = existingLocation,
        context = context,
        isDarkMode = isDarkMode,
        isTerrainMode = isTerrainMode,
        onMapStateChange = { moving ->
            if (isEditMode) isMapMoving = moving
        },
        onCenterChanged = { center ->
            if (isEditMode) {
                scope.launch {
                    currentLocation = OsmMapUtils.updateLocationFromCenter(center)
                }
            }
        }
    )

    fun performLocationFetch() {
        isFetchingLocation = true
        scope.launch {
            delay(500)
            val location = OsmMapUtils.fetchCurrentLocation(context)
            if (location != null && (location.latitude != 0.0 || location.longitude != 0.0)) {
                currentLocation = location
                mapView.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                mapView.controller.setZoom(17.0)
            }
            isFetchingLocation = false
        }
    }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            performLocationFetch()
        } else {
            isFetchingLocation = false
        }
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
                    } catch (e: IntentSender.SendIntentException) {
                        isFetchingLocation = false
                    }
                } else {
                    isFetchingLocation = false
                }
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkSettingsAndFetch()
        } else {
            isFetchingLocation = false
        }
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
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            if (isEditMode) {
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
                                OsmMapUtils.searchNominatim(searchQuery)?.let { result ->
                                    mapView.controller.animateTo(result)
                                    mapView.controller.setZoom(17.0)
                                    currentLocation = OsmMapUtils.updateLocationFromCenter(result)
                                }
                                isSearching = false
                            }
                        },
                        isSearching = isSearching,
                        onBack = onDismiss
                    )
                }

                MapControlColumn(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 80.dp, end = 16.dp),
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode },
                    isTerrainMode = isTerrainMode,
                    onToggleTerrain = { isTerrainMode = !isTerrainMode },
                    isFetchingLocation = isFetchingLocation,
                    onMyLocationClick = onMyLocationClick,
                    onZoomIn = { mapView.controller.zoomIn() },
                    onZoomOut = { mapView.controller.zoomOut() }
                )

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
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    FilledIconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(painterResource(R.drawable.close_24px), "Close")
                    }
                }

                MapControlColumn(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 16.dp, end = 16.dp),
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode },
                    isTerrainMode = isTerrainMode,
                    onToggleTerrain = { isTerrainMode = !isTerrainMode },
                    isFetchingLocation = false,
                    onMyLocationClick = null,
                    onZoomIn = { mapView.controller.zoomIn() },
                    onZoomOut = { mapView.controller.zoomOut() }
                )
                Box(modifier = Modifier.align(Alignment.Center)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    ) {}
                }
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
                    isEditMode = isEditMode,
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
fun MapControlColumn(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    isTerrainMode: Boolean,
    onToggleTerrain: () -> Unit,
    isFetchingLocation: Boolean,
    onMyLocationClick: (() -> Unit)?,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledIconButton(
            onClick = onToggleDarkMode,
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(if (isDarkMode) R.drawable.light_mode_24px else R.drawable.dark_mode_24px),
                contentDescription = "Toggle Dark Mode",
                modifier = Modifier.size(20.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledIconButton(
                onClick = onZoomIn,
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = 4.dp, bottomEnd = 4.dp
                ),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_24px),
                    contentDescription = "Zoom In",
                    modifier = Modifier.size(24.dp)
                )
            }

            FilledIconButton(
                onClick = onZoomOut,
                shape = RoundedCornerShape(
                    topStart = 4.dp, topEnd = 4.dp,
                    bottomStart = 16.dp, bottomEnd = 16.dp
                ),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.remove_24px),
                    contentDescription = "Zoom Out",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (onMyLocationClick != null) {
            FilledIconButton(
                onClick = onMyLocationClick,
                enabled = !isFetchingLocation,
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.size(56.dp)
            ) {
                if (isFetchingLocation) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.my_location_24px_fill),
                        contentDescription = "Current Location",
                        modifier = Modifier.size(24.dp)
                    )
                }
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
    isEditMode: Boolean,
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading && isEditMode) {
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
                        text = if (isLoading && isEditMode) "Locating..." else (location.name
                            ?: "Selected Location"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!isLoading || !isEditMode) {
                        location.locality?.let { locality ->
                            Text(
                                text = locality,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "Fetching address...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isEditMode) {
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
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}