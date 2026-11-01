package com.denser.june.presentation.plus

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.denser.june.BuildConfig
import com.denser.june.core.utils.Constants
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.billing.BillingHandler
import com.denser.june.presentation.components.InternetRestrictedBanner
import com.denser.june.presentation.components.JuneCloseButton
import com.denser.june.presentation.components.plus.PlusBadge
import com.denser.june.presentation.theme.LocalInternetAllowed
import kotlinx.coroutines.launch
import com.denser.june.presentation.components.JuneFullScreenDialog
import org.koin.compose.koinInject

data class PlusSection(
    val title: String,
    val description: String,
    val iconRes: Int,
    val shape: Shape,
    val containerColor: Color,
    val contentColor: Color
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val junePlusSections: List<PlusSection>
    @Composable
    get() = listOf(
        PlusSection(
            title = "Unlock all Features",
            description = "Keep your most personal journals tucked away in a private vault, and secure your app with pattern gesture or password locks.",
            iconRes = R.drawable.lock_24px_fill,
            shape = MaterialShapes.Ghostish.toShape(),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        PlusSection(
            title = "More customization options",
            description = "Make journaling personal with live variable font controls, handpicked typefaces, per-entry custom color palettes, and styled map accents.",
            iconRes = R.drawable.palette_24px_fill,
            shape = MaterialShapes.Sunny.toShape(),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        PlusSection(
            title = "Support Development",
            description = "Born out of love and handcrafted by a solo indie developer. Your one-time purchase directly funds continuous improvements and future updates.",
            iconRes = R.drawable.favorite_24px_fill,
            shape = MaterialShapes.Flower.toShape(),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JunePlusPaywallScreen(
    controller: PlusSheetController,
    modifier: Modifier = Modifier
) {
    val billingHandler = koinInject<BillingHandler>()
    val isPlus by billingHandler.isPlus.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = controller.isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ),
        modifier = modifier.fillMaxSize()
    ) {
        BackHandler(enabled = true) {
            controller.dismiss(isPlus)
        }

        JunePlusPaywallContent(
            onDismiss = { controller.dismiss(isPlus) },
            onUnlocked = { controller.onPlusUnlockedSuccess() }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun JunePlusPaywallContent(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit = onDismiss,
) {
    val billingHandler = koinInject<BillingHandler>()
    val isPlus by billingHandler.isPlus.collectAsStateWithLifecycle()
    val plusPrice by billingHandler.plusPrice.collectAsStateWithLifecycle()
    val isInternetAllowed = LocalInternetAllowed.current

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val activity = remember(context) { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()
    var isPurchasing by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val buttonText = when {
                        isPurchasing -> "Unlocking Plus…"
                        plusPrice != null -> "Unlock Plus • $plusPrice"
                        else -> "Unlock Plus"
                    }

                    if (BuildConfig.FLAVOR == "foss") {
                        Text(
                            text = "All features are unlocked in FOSS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = { uriHandler.openUri(Constants.BUY_ME_A_COFFEE_URL) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.buy_me_a_coffee),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Buy Me a Coffee",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (isPlus) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "June Plus is Active",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        errorMessage?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                            )
                        }

                        Text(
                            text = "Pay once, own forever",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = {
                                val currentActivity = activity ?: context.findActivity()
                                if (currentActivity != null) {
                                    coroutineScope.launch {
                                        isPurchasing = true
                                        errorMessage = null
                                        val result = billingHandler.purchasePlus(currentActivity)
                                        isPurchasing = false
                                        if (result.isSuccess && result.getOrNull() == true) {
                                            onUnlocked()
                                        } else if (result.isFailure) {
                                            val msg = result.exceptionOrNull()?.message
                                            if (msg != "Purchase cancelled") {
                                                errorMessage = msg ?: "Purchase failed"
                                            }
                                        }
                                    }
                                } else {
                                    errorMessage = "Cannot find hosting Activity"
                                }
                            },
                            enabled = isInternetAllowed && !isPurchasing && !isRestoring,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (isPurchasing) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = buttonText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    isRestoring = true
                                    errorMessage = null
                                    val result = billingHandler.restorePurchases()
                                    isRestoring = false
                                    if (!result.isSuccess || result.getOrNull() != true) {
                                        errorMessage = "No active Plus purchase found to restore."
                                    }
                                }
                            },
                            enabled = isInternetAllowed && !isPurchasing && !isRestoring
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "Restore Purchases",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                HeroTopSection()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 0.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (!isInternetAllowed) {
                        InternetRestrictedBanner(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }



                    junePlusSections.forEach { section ->
                        PlusSectionView(section = section)
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }

            TopAppBar(
                title = {},
                navigationIcon = {
                    JuneCloseButton(onClick = onDismiss)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                windowInsets = TopAppBarDefaults.windowInsets.add(
                    WindowInsets(left = 8.dp, right = 8.dp)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroTopSection() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp)
            .background(
                Brush.verticalGradient(
                    0.0f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    0.50f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f),
                    0.85f to MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.15f),
                    1.0f to MaterialTheme.colorScheme.surface
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Black,
                            0.50f to Color.Black.copy(alpha = 0.90f),
                            0.80f to Color.Black.copy(alpha = 0.35f),
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = (-10).dp, y = (-12).dp)
                    .rotate(-15f)
                    .clip(MaterialShapes.Cookie4Sided.toShape())
                    .background(primaryColor.copy(alpha = 0.24f))
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = 96.dp, y = 28.dp)
                    .rotate(-25f)
                    .clip(MaterialShapes.SemiCircle.toShape())
                    .background(tertiaryColor.copy(alpha = 0.26f))
            )
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .offset(x = 0.dp, y = 96.dp)
                    .rotate(-18f)
                    .clip(MaterialShapes.Ghostish.toShape())
                    .background(secondaryColor.copy(alpha = 0.20f))
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .offset(x = 92.dp, y = 90.dp)
                    .rotate(24f)
                    .clip(MaterialShapes.Sunny.toShape())
                    .background(primaryColor.copy(alpha = 0.22f))
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .offset(x = 46.dp, y = 168.dp)
                    .rotate(14f)
                    .clip(MaterialShapes.Arrow.toShape())
                    .background(tertiaryColor.copy(alpha = 0.22f))
            )

            Box(
                modifier = Modifier
                    .size(82.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = (-8).dp)
                    .rotate(34f)
                    .clip(MaterialShapes.Cookie6Sided.toShape())
                    .background(secondaryColor.copy(alpha = 0.22f))
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-106).dp, y = 36.dp)
                    .rotate(-35f)
                    .clip(MaterialShapes.Pill.toShape())
                    .background(primaryColor.copy(alpha = 0.20f))
            )
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-28).dp, y = 92.dp)
                    .rotate(14f)
                    .clip(MaterialShapes.Pentagon.toShape())
                    .background(tertiaryColor.copy(alpha = 0.22f))
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-100).dp, y = 148.dp)
                    .rotate(-10f)
                    .clip(MaterialShapes.Flower.toShape())
                    .background(secondaryColor.copy(alpha = 0.20f))
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-22).dp, y = 184.dp)
                    .rotate(22f)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(primaryColor.copy(alpha = 0.18f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 24.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PlusBadge(
                size = 48.dp,
                iconSize = 36.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "June Plus",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "'GRAD' 150",
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PlusSectionView(
    section: PlusSection,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = section.containerColor,
                shape = section.shape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(section.iconRes),
                        contentDescription = null,
                        tint = section.contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = section.description,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 54.dp)
        )
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}
