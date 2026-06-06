package com.denser.june.presentation.screens.settings.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalUriHandler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.utils.Constants
import com.denser.june.core.domain.model.enums.MapStyleProvider
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTextField
import com.denser.june.presentation.components.InternetRestrictedBanner
import com.denser.june.presentation.components.JuneTopAppBar
import com.denser.june.presentation.components.MapLibreInitializer
import com.denser.june.presentation.components.MapViewLifecycleEffect
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.screens.settings.components.SettingSection
import com.denser.june.presentation.screens.settings.tiles.DefaultMapThemeTile
import com.denser.june.presentation.theme.LocalInternetAllowed
import com.denser.june.presentation.utils.MapProviderUtils
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MapSettingsScreen() {
    val navigator = koinInject<AppNavigator>()
    val journalPreferences = koinInject<JournalPreferences>()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current

    val currentProvider by journalPreferences.mapStyleProvider()
        .collectAsStateWithLifecycle(initialValue = MapStyleProvider.CARTO)

    val mapTilerKey by journalPreferences.maptilerKey()
        .collectAsStateWithLifecycle(initialValue = "")

    val stadiaKey by journalPreferences.stadiaKey()
        .collectAsStateWithLifecycle(initialValue = "")

    val mapboxKey by journalPreferences.mapboxkey()
        .collectAsStateWithLifecycle(initialValue = "")

    var maptilerKeyInput by remember(mapTilerKey) { mutableStateOf(mapTilerKey) }
    var stadiaKeyInput by remember(stadiaKey) { mutableStateOf(stadiaKey) }
    var mapboxKeyInput by remember(mapboxKey) { mutableStateOf(mapboxKey) }

    var isMaptilerKeyLocked by remember { mutableStateOf(true) }
    var isMaptilerKeyObscured by remember { mutableStateOf(true) }
    var isStadiaKeyLocked by remember { mutableStateOf(true) }
    var isStadiaKeyObscured by remember { mutableStateOf(true) }
    var isMapboxKeyLocked by remember { mutableStateOf(true) }
    var isMapboxKeyObscured by remember { mutableStateOf(true) }

    val tabs = listOf("CARTO", "MapTiler", "Stadia", "Mapbox")
    val selectedTabIndex = when (currentProvider) {
        MapStyleProvider.CARTO -> 0
        MapStyleProvider.MAPTILER -> 1
        MapStyleProvider.STADIA -> 2
        MapStyleProvider.MAPBOX -> 3
    }

    var keyTestStatus by remember(selectedTabIndex) { mutableStateOf<Boolean?>(null) }
    var isTestingKey by remember(selectedTabIndex) { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val isInternetAllowed = LocalInternetAllowed.current
    val isSystemDark = isSystemInDarkTheme()
    var isPreviewDarkMode by remember { mutableStateOf(isSystemDark) }

    MapLibreInitializer(isInternetAllowed)

    val isTilerConfigured = mapTilerKey.isNotBlank()
    val isStadiaConfigured = stadiaKey.isNotBlank()
    val isMapboxConfigured = mapboxKey.isNotBlank()

    val isCurrentProviderConfigured = when (currentProvider) {
        MapStyleProvider.CARTO -> true
        MapStyleProvider.MAPTILER -> isTilerConfigured
        MapStyleProvider.STADIA -> isStadiaConfigured
        MapStyleProvider.MAPBOX -> isMapboxConfigured
    }

    val isCurrentKeyUnlocked = when (currentProvider) {
        MapStyleProvider.CARTO -> false
        MapStyleProvider.MAPTILER -> !isMaptilerKeyLocked
        MapStyleProvider.STADIA -> !isStadiaKeyLocked
        MapStyleProvider.MAPBOX -> !isMapboxKeyLocked
    }

    var isMapLoading by remember { mutableStateOf(false) }
    var mapLoadError by remember { mutableStateOf<String?>(null) }

    val locations = remember {
        listOf(
            PreviewLocation("Amsterdam", LatLng(52.3730, 4.8926), 14.5),
            PreviewLocation("Central Park", LatLng(40.785091, -73.968285), 12.0),
            PreviewLocation("SF", LatLng(37.7950, -122.4020), 11.5),
            PreviewLocation("Sydney", LatLng(-33.8568, 151.2153), 13.0),
        )
    }
    var selectedLocationIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Large,
                title = { Text("Map Settings") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Back",
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            if (!isInternetAllowed) {
                InternetRestrictedBanner(
                    description = "Maps require internet access.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            SettingSection {
                DefaultMapThemeTile()
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Switch(
                            checked = isPreviewDarkMode,
                            onCheckedChange = { isPreviewDarkMode = it },
                            enabled = !isMapLoading,
                            modifier = Modifier.scale(0.75f),
                            thumbContent = {
                                Icon(
                                    painterResource(if (isPreviewDarkMode) R.drawable.dark_mode_24px else R.drawable.light_mode_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isPreviewDarkMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.5f
                                ),
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.4f
                                ),
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                }

                val previewOnSurfaceColor = if (isPreviewDarkMode) Color.White else Color.Black
                if (isInternetAllowed) {
                    val previewMapView = remember {
                        MapView(context).apply {
                            isClickable = true
                            isFocusable = true
                            setOnTouchListener { view, event ->
                                when (event.action) {
                                    android.view.MotionEvent.ACTION_DOWN,
                                    android.view.MotionEvent.ACTION_MOVE -> {
                                        view.parent?.requestDisallowInterceptTouchEvent(true)
                                    }
                                    android.view.MotionEvent.ACTION_UP,
                                    android.view.MotionEvent.ACTION_CANCEL -> {
                                        view.parent?.requestDisallowInterceptTouchEvent(false)
                                    }
                                }
                                false
                            }

                            addOnDidFailLoadingMapListener { error ->
                                isMapLoading = false
                                mapLoadError = error
                            }
                        }
                    }

                    var currentStyleUrl by remember { mutableStateOf("") }

                    LaunchedEffect(selectedLocationIndex) {
                        previewMapView.getMapAsync { mapboxMap ->
                            val loc = locations[selectedLocationIndex]
                            mapboxMap.animateCamera(
                                org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(loc.latLng)
                                        .zoom(loc.zoom)
                                        .build()
                                ),
                                800
                            )
                        }
                    }

                    MapViewLifecycleEffect(previewMapView)
                    val mapStyleUrl = remember(currentProvider, isPreviewDarkMode, mapTilerKey, stadiaKey, mapboxKey) {
                        MapProviderUtils.getStyleUrlWithKeys(
                            provider = currentProvider,
                            isDark = isPreviewDarkMode,
                            mapTilerKey = mapTilerKey,
                            stadiaKey = stadiaKey,
                            mapboxKey = mapboxKey
                        )
                    }
                      Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                     ) {
                         if (isCurrentProviderConfigured) {
                            AndroidView(
                                factory = { previewMapView },
                                modifier = Modifier.fillMaxSize(),
                                update = { map ->
                                     val styleUrl = mapStyleUrl
                                    if (isCurrentKeyUnlocked) {
                                        currentStyleUrl = ""
                                        isMapLoading = false
                                    } else if (styleUrl != currentStyleUrl) {
                                        currentStyleUrl = styleUrl
                                        isMapLoading = true
                                        mapLoadError = null
                                        map.getMapAsync { mapboxMap ->
                                            mapboxMap.uiSettings.isAttributionEnabled = false
                                            mapboxMap.uiSettings.isLogoEnabled = false
                                            mapboxMap.uiSettings.isCompassEnabled = false
                                            mapboxMap.setStyle(styleUrl) {
                                                isMapLoading = false
                                                val loc = locations[selectedLocationIndex]
                                                mapboxMap.moveCamera(
                                                    org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                                        CameraPosition.Builder()
                                                            .target(loc.latLng)
                                                            .zoom(loc.zoom)
                                                            .build()
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        isMapLoading = false
                                    }
                                }
                            )
                            if (isMapLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ContainedLoadingIndicator()
                                }
                            } else if (mapLoadError != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (isPreviewDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.warning_24px),
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Failed to load map",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = previewOnSurfaceColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Check your API key or internet connection",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = previewOnSurfaceColor.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 24.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }


                        } else {
                            isMapLoading = false
                            val providerName = stringResource(currentProvider.displayNameRes)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isPreviewDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.warning_24px),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = previewOnSurfaceColor.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$providerName not configured",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = previewOnSurfaceColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Configure and test API key below to enable preview",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = previewOnSurfaceColor.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        if (isCurrentProviderConfigured) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                tonalElevation = 3.dp,
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            selectedLocationIndex = if (selectedLocationIndex > 0) {
                                                selectedLocationIndex - 1
                                            } else {
                                                locations.size - 1
                                            }
                                        },
                                        enabled = !isMapLoading,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.chevron_left_24px),
                                            contentDescription = "Previous Location",
                                            tint = if (!isMapLoading) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        locations.forEachIndexed { index, _ ->
                                            val isSelected = index == selectedLocationIndex
                                            Box(
                                                modifier = Modifier
                                                    .size(if (isSelected) 6.dp else 4.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        if (isSelected) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                        }
                                                    )
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            selectedLocationIndex = if (selectedLocationIndex < locations.size - 1) {
                                                selectedLocationIndex + 1
                                            } else {
                                                0
                                            }
                                        },
                                        enabled = !isMapLoading,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.chevron_right_24px),
                                            contentDescription = "Next Location",
                                            tint = if (!isMapLoading) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isPreviewDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.wifi_off_24px),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = previewOnSurfaceColor.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Internet connection required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = previewOnSurfaceColor.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Connect to the internet to load map preview",
                                style = MaterialTheme.typography.bodySmall,
                                color = previewOnSurfaceColor.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

            }

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(12.dp))

                    tabs.forEachIndexed { index, title ->
                        val tabProvider = when (index) {
                            0 -> MapStyleProvider.CARTO
                            1 -> MapStyleProvider.MAPTILER
                            2 -> MapStyleProvider.STADIA
                            3 -> MapStyleProvider.MAPBOX
                            else -> MapStyleProvider.CARTO
                        }
                        val isSelected = selectedTabIndex == index
                        val isActive = currentProvider == tabProvider

                        Surface(
                            onClick = {
                                if (!isMapLoading) {
                                    scope.launch {
                                        journalPreferences.setMapStyleProvider(tabProvider)
                                    }
                                }
                            },
                            enabled = !isMapLoading,
                            shape = RoundedCornerShape(24.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp, 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                    )
                                )
                                if (isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        painter = painterResource(R.drawable.check_circle_24px_fill),
                                        contentDescription = "Active",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.secondary
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                when (selectedTabIndex) {
                    0 -> {
                        Text(
                            text = "CARTO Voyager (Light) and Dark Matter vector styles are resolved automatically. No API key required.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                        )
                    }

                    1 -> {
                        ApiCredentialsSection(
                            keyInput = maptilerKeyInput,
                            onKeyChange = {
                                maptilerKeyInput = it
                                scope.launch {
                                    journalPreferences.setMaptilerKey(it.trim())
                                    journalPreferences.setMapProviderVerified(
                                        MapStyleProvider.MAPTILER,
                                        false
                                    )
                                }
                                keyTestStatus = null
                            },
                            isKeyLocked = isMaptilerKeyLocked,
                            onKeyLockedChange = { isMaptilerKeyLocked = it },
                            isKeyObscured = isMaptilerKeyObscured,
                            onKeyObscuredChange = { isMaptilerKeyObscured = it },
                            placeholder = "MapTiler API key",
                            isTesting = isTestingKey,
                            testStatus = keyTestStatus,
                            onTest = {
                                isTestingKey = true
                                keyTestStatus = null
                                scope.launch {
                                    val isValid = MapProviderUtils.testApiKey(
                                        MapStyleProvider.MAPTILER,
                                        maptilerKeyInput.trim()
                                    )
                                    keyTestStatus = isValid
                                    journalPreferences.setMapProviderVerified(
                                        MapStyleProvider.MAPTILER,
                                        isValid
                                    )
                                    if (isValid) {
                                        journalPreferences.setMapStyleProvider(MapStyleProvider.MAPTILER)
                                    }
                                    isTestingKey = false
                                }
                            },
                            isActionsEnabled = !isMapLoading,
                            providerUrl = Constants.MAPTILER_CLOUD_URL
                        )
                    }

                    2 -> {
                        ApiCredentialsSection(
                            keyInput = stadiaKeyInput,
                            onKeyChange = {
                                stadiaKeyInput = it
                                scope.launch {
                                    journalPreferences.setStadiaKey(it.trim())
                                    journalPreferences.setMapProviderVerified(
                                        MapStyleProvider.STADIA,
                                        false
                                    )
                                }
                                keyTestStatus = null
                            },
                            isKeyLocked = isStadiaKeyLocked,
                            onKeyLockedChange = { isStadiaKeyLocked = it },
                            isKeyObscured = isStadiaKeyObscured,
                            onKeyObscuredChange = { isStadiaKeyObscured = it },
                            placeholder = "Stadia API key",
                            isTesting = isTestingKey,
                            testStatus = keyTestStatus,
                            onTest = {
                                isTestingKey = true
                                keyTestStatus = null
                                scope.launch {
                                    val isValid = MapProviderUtils.testApiKey(
                                        MapStyleProvider.STADIA,
                                        stadiaKeyInput.trim()
                                    )
                                    keyTestStatus = isValid
                                    journalPreferences.setMapProviderVerified(
                                        MapStyleProvider.STADIA,
                                        isValid
                                    )
                                    if (isValid) {
                                        journalPreferences.setMapStyleProvider(MapStyleProvider.STADIA)
                                    }
                                    isTestingKey = false
                                }
                            },
                            isActionsEnabled = !isMapLoading,
                            providerUrl = Constants.STADIA_CLIENT_URL
                        )
                    }

                    3 -> {
                        ApiCredentialsSection(
                            keyInput = mapboxKeyInput,
                            onKeyChange = { boxKey ->
                                mapboxKeyInput = boxKey
                                scope.launch {
                                    journalPreferences.setMapboxkey(boxKey.trim())
                                    journalPreferences.setMapProviderVerified(
                                        MapStyleProvider.MAPBOX,
                                        false
                                    )
                                }
                                keyTestStatus = null
                            },
                            isKeyLocked = isMapboxKeyLocked,
                            onKeyLockedChange = { isMapboxKeyLocked = it },
                            isKeyObscured = isMapboxKeyObscured,
                            onKeyObscuredChange = { isMapboxKeyObscured = it },
                            placeholder = "Mapbox access token",
                            isTesting = isTestingKey,
                            testStatus = keyTestStatus,
                            onTest = {
                                isTestingKey = true
                                keyTestStatus = null
                                scope.launch {
                                    val isValid = MapProviderUtils.testApiKey(
                                        MapStyleProvider.MAPBOX,
                                        mapboxKeyInput.trim()
                                    )
                                    keyTestStatus = isValid
                                    journalPreferences.setMapProviderVerified(
                                        MapStyleProvider.MAPBOX,
                                        isValid
                                    )
                                    if (isValid) {
                                        journalPreferences.setMapStyleProvider(MapStyleProvider.MAPBOX)
                                    }
                                    isTestingKey = false
                                }
                            },
                            isActionsEnabled = !isMapLoading,
                            providerUrl = Constants.MAPBOX_ACCOUNT_URL
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InlineKeyTestSection(
    key: String,
    isTesting: Boolean,
    status: Boolean?,
    onTest: () -> Unit,
    isActionsEnabled: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onTest,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isTesting && key.isNotBlank() && isActionsEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            if (isTesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Testing Key...")
            } else {
                Icon(
                    painter = painterResource(R.drawable.sync_24px),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test API Key")
            }
        }

        status?.let { isValid ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (isValid) {
                    Icon(
                        painter = painterResource(R.drawable.check_24px),
                        contentDescription = "Valid",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.api_key_valid),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.warning_24px),
                        contentDescription = "Invalid",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.api_key_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ApiCredentialsSection(
    keyInput: String,
    onKeyChange: (String) -> Unit,
    isKeyLocked: Boolean,
    onKeyLockedChange: (Boolean) -> Unit,
    isKeyObscured: Boolean,
    onKeyObscuredChange: (Boolean) -> Unit,
    placeholder: String,
    isTesting: Boolean,
    testStatus: Boolean?,
    onTest: () -> Unit,
    isActionsEnabled: Boolean = true,
    providerUrl: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "API Credentials",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Switch(
                checked = isKeyLocked,
                onCheckedChange = onKeyLockedChange,
                enabled = isActionsEnabled,
                modifier = Modifier.scale(0.75f),
                thumbContent = {
                    Icon(
                        painterResource(if (isKeyLocked) R.drawable.lock_24px else R.drawable.lock_open_right_24px),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (isKeyLocked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        JuneTextField(
            value = keyInput,
            onValueChange = onKeyChange,
            label = "",
            placeholder = placeholder,
            placeholderStyle = MaterialTheme.typography.bodyMedium,
            enabled = !isKeyLocked && isActionsEnabled,
            keyboardType = KeyboardType.Password,
            visualTransformation = if (isKeyObscured) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                IconButton(
                    onClick = { onKeyObscuredChange(!isKeyObscured) },
                    enabled = isActionsEnabled
                ) {
                    Icon(
                        painter = painterResource(if (isKeyObscured) R.drawable.visibility_off_24px else R.drawable.visibility_24px),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        InlineKeyTestSection(
            key = keyInput,
            isTesting = isTesting,
            status = testStatus,
            onTest = onTest,
            isActionsEnabled = isActionsEnabled
        )

        providerUrl?.let { url ->
            Spacer(modifier = Modifier.height(8.dp))
            val uriHandler = LocalUriHandler.current
            TextButton(
                onClick = { uriHandler.openUri(url) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.open_in_new_24px),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Get API key or access token",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private data class PreviewLocation(
    val name: String,
    val latLng: LatLng,
    val zoom: Double
)

