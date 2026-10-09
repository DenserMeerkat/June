package com.denser.june.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.domain.preferences.SyncPreferences
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.media.compress.CompressUtils
import com.denser.june.core.media.compress.model.CompressionPreset
import com.denser.june.core.media.exif.ExifUtils
import com.denser.june.presentation.components.lightbox.*
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JuneMediaLightbox(
    mediaPaths: List<String>,
    initialIndex: Int,
    dateTime: Long? = null,
    journalId: String? = null,
    allowCompression: Boolean = true,
    onDeleteMedia: ((String) -> Unit)? = null
) {
    val navigator = koinInject<AppNavigator>()
    val context = LocalContext.current
    val journalRepo = koinInject<JournalRepository>()
    val syncPrefs = koinInject<SyncPreferences>()
    val journalPrefs = koinInject<JournalPreferences>()
    val photoQualityPref by journalPrefs.getPhotoCompressionQuality().collectAsStateWithLifecycle(initialValue = 80)
    val videoQualityPref by journalPrefs.getVideoCompressionQuality().collectAsStateWithLifecycle(initialValue = 1080)
    val scope = rememberCoroutineScope()

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }
    DisposableEffect(imageLoader) {
        onDispose {
            imageLoader.shutdown()
        }
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val scrimColor = if (isDarkTheme) Color.Black else Color.White

    LightboxSystemBarsEffect(context = context, isDarkTheme = isDarkTheme)

    val pagerState = rememberPagerState(initialPage = initialIndex) { mediaPaths.size }
    var showUI by remember { mutableStateOf(true) }
    var isInfoVisible by remember { mutableStateOf(false) }
    var isVideoZoomed by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var newlyCompressedPaths by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(pagerState.currentPage) {
        isVideoZoomed = false
    }

    LaunchedEffect(mediaPaths) {
        if (mediaPaths.isEmpty()) {
            navigator.navigateBack()
        }
    }

    val currentPath = mediaPaths.getOrNull(pagerState.currentPage)

    val metadataState = rememberLightboxMediaMetadataState(
        context = context,
        currentPath = currentPath,
        journalId = journalId,
        fallbackDateTime = dateTime,
        refreshTrigger = refreshTrigger,
        newlyCompressedPaths = newlyCompressedPaths,
        journalRepo = journalRepo,
        syncPrefs = syncPrefs
    )

    val compressionState = rememberLightboxCompressionState(
        currentPath = currentPath,
        context = context,
        refreshTrigger = refreshTrigger,
        photoQualityPref = photoQualityPref,
        videoQualityPref = videoQualityPref
    )

    BackHandler(enabled = compressionState.isCompressMode || isInfoVisible) {
        if (compressionState.isCompressMode) {
            compressionState.cancelCompression()
        } else if (isInfoVisible) {
            isInfoVisible = false
        }
    }

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenHeight = with(density) { windowInfo.containerSize.height.toDp() }
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val minSheetHeight = screenHeight * 0.65f
    val sheetTop = screenHeight - minSheetHeight

    val mediaAspect = remember(metadataState.exifInfo) {
        val w = metadataState.exifInfo?.width ?: 0
        val h = metadataState.exifInfo?.height ?: 0
        if (w > 0 && h > 0) w.toFloat() / h.toFloat() else null
    }

    val displayedMediaHeight = remember(mediaAspect, screenWidth) {
        if (mediaAspect != null && mediaAspect > 0f) {
            screenWidth / mediaAspect
        } else {
            screenWidth
        }
    }

    val defaultMediaCenterY = screenHeight / 2
    val mediaBottom = defaultMediaCenterY + (displayedMediaHeight / 2)
    val sheetOverlap = 24.dp
    val computedTranslationY = remember(displayedMediaHeight, mediaBottom, sheetTop) {
        val targetBottom = sheetTop + sheetOverlap
        if (mediaBottom <= targetBottom) {
            0.dp
        } else {
            targetBottom - mediaBottom
        }
    }

    val animatedTranslationY by animateDpAsState(
        targetValue = if (isInfoVisible) computedTranslationY else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mediaTranslationY"
    )

    val uncompressedOrig = if (currentPath != null) CompressUtils.getUncompressedOriginalFile(context, currentPath) else null
    val canCompressFurther = compressionState.availablePresets.any { it != CompressionPreset.ORIGINAL } || uncompressedOrig != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrimColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = animatedTranslationY.toPx()
                }
        ) {
            LightboxMediaPager(
                pagerState = pagerState,
                mediaPaths = mediaPaths,
                context = context,
                imageLoader = imageLoader,
                compressionState = compressionState,
                showUI = showUI,
                isInfoVisible = isInfoVisible,
                isVideoZoomed = isVideoZoomed,
                isDarkTheme = isDarkTheme,
                refreshTrigger = refreshTrigger,
                onToggleUI = { showUI = !showUI },
                onShowInfo = { isInfoVisible = true },
                onHideInfo = { isInfoVisible = false },
                onVideoZoomChanged = { isVideoZoomed = it }
            )
        }

        LightboxInfoBottomSheet(
            isInfoVisible = isInfoVisible,
            onDismiss = { isInfoVisible = false },
            exifInfo = metadataState.exifInfo,
            dateTime = dateTime,
            parentJournal = metadataState.parentJournal,
            isJournalNavigationEnabled = journalId == null,
            isMediaBackedUp = metadataState.isMediaBackedUp,
            isPendingSync = metadataState.isPendingSync,
            syncProvider = metadataState.syncProvider,
            isCompressing = compressionState.isCompressing,
            canCompressFurther = canCompressFurther,
            allowCompression = allowCompression,
            onJournalClick = {
                metadataState.parentJournal?.let { navigator.navigateTo(Route.Editor(journalId = it.id)) }
            },
            onCompressClick = {
                if (currentPath != null && !compressionState.isCompressing && allowCompression) {
                    isInfoVisible = false
                    compressionState.startCompression()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        AnimatedVisibility(
            visible = showUI || compressionState.isCompressMode,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LightboxTopBar(
                isCompressMode = compressionState.isCompressMode,
                isInfoVisible = isInfoVisible,
                isDarkTheme = isDarkTheme,
                isGeneratingPreview = compressionState.isGeneratingPreview,
                isCompressing = compressionState.isCompressing,
                selectedPreset = compressionState.selectedPreset,
                appliedPreset = compressionState.appliedPreset,
                availablePresets = compressionState.availablePresets,
                currentPath = currentPath,
                exifInfo = metadataState.exifInfo,
                dateTime = dateTime,
                parentJournal = metadataState.parentJournal,
                currentPreview = compressionState.currentPreview,
                hasUncompressedOriginal = uncompressedOrig != null,
                uncompressedOriginalBytes = uncompressedOrig?.length(),
                allowCompression = allowCompression,
                onBackOrCancel = {
                    if (compressionState.isCompressMode) {
                        compressionState.cancelCompression()
                    } else if (isInfoVisible) {
                        isInfoVisible = false
                    } else {
                        navigator.navigateBack()
                    }
                },
                onSaveCompression = {
                    if (currentPath != null) {
                        scope.launch {
                            compressionState.isCompressing = true
                            val isVideo = ExifUtils.isVideoPath(currentPath)
                            val preset = compressionState.selectedPreset ?: CompressionPreset.MEDIUM
                            val success = withContext(Dispatchers.IO) {
                                if (isVideo) {
                                    CompressUtils.compressVideo(context, currentPath, preset)
                                } else {
                                    CompressUtils.compressPhoto(context, currentPath, preset)
                                }
                            }
                            if (success) {
                                newlyCompressedPaths = newlyCompressedPaths + currentPath
                                val fileName = File(currentPath).name
                                imageLoader.memoryCache?.keys?.filter { it.key.contains(fileName) }?.forEach { imageLoader.memoryCache?.remove(it) }
                                val globalCache = coil.Coil.imageLoader(context).memoryCache
                                globalCache?.keys?.filter { it.key.contains(fileName) }?.forEach { globalCache.remove(it) }
                                refreshTrigger++

                                val journalToUpdate = metadataState.parentJournal ?: withContext(Dispatchers.IO) {
                                    journalRepo.getAllJournalsIncludeDeletedSync().firstOrNull { j ->
                                        j.images.any { File(it).name == fileName }
                                    }
                                }
                                journalToUpdate?.let { j ->
                                    val updated = j.copy(
                                        updatedAt = System.currentTimeMillis(),
                                        syncedAt = null
                                    )
                                    withContext(Dispatchers.IO) {
                                        journalRepo.updateJournal(updated)
                                    }
                                }
                            }
                            compressionState.cleanup()
                            compressionState.isCompressing = false
                            compressionState.isCompressMode = false
                        }
                    }
                },
                onOpenInfo = {
                    isInfoVisible = true
                },
                onStartCompression = {
                    if (currentPath != null && !compressionState.isCompressing) {
                        isInfoVisible = false
                        compressionState.startCompression()
                    }
                },
                onDeleteMedia = onDeleteMedia?.let { callback ->
                    {
                        if (currentPath != null) {
                            CompressUtils.cleanUncompressedOriginalFile(context, currentPath)
                            if (mediaPaths.size <= 1) {
                                navigator.navigateBack()
                            }
                            callback(currentPath)
                        }
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = compressionState.isCompressMode && compressionState.selectedPreset != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            compressionState.selectedPreset?.let { currentPreset ->
                LightboxCompressionSliderCard(
                    selectedPreset = currentPreset,
                    isDarkTheme = isDarkTheme,
                    onPresetChanged = { preset ->
                        if (preset != compressionState.selectedPreset) {
                            compressionState.selectedPreset = preset
                        }
                    }
                )
            }
        }

        if (compressionState.isCompressing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}