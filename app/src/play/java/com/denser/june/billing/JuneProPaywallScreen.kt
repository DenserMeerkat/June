package com.denser.june.billing

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denser.june.core.R
import com.denser.june.core.domain.billing.BillingHandler
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.presentation.components.JuneAppBarType
import com.denser.june.presentation.components.JuneTopAppBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuneProPaywallScreen(
    onDismiss: () -> Unit
) {
    val billingHandler: BillingHandler = koinInject()
    val privacyPreferences: PrivacyPreferences = koinInject()
    val isPro by billingHandler.isPro.collectAsStateWithLifecycle()
    val proPrice by billingHandler.proPrice.collectAsStateWithLifecycle()
    val isInternetAllowed by privacyPreferences.getIsInternetAllowedFlow().collectAsStateWithLifecycle(initialValue = true)
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            JuneTopAppBar(
                type = JuneAppBarType.Small,
                title = { Text("June Pro") },
                navigationIcon = {
                    FilledIconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close_24px),
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isPro) "June Pro is active" else "June Pro is inactive",
                    style = MaterialTheme.typography.titleMedium
                )

                if (!isInternetAllowed) {
                    Text(
                        text = "Internet access is restricted in June's privacy settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (!isPro) {
                    Button(
                        onClick = {
                            activity?.let {
                                scope.launch { billingHandler.purchasePro(it) }
                            }
                        },
                        enabled = isInternetAllowed,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (proPrice != null) "Unlock Pro — $proPrice" else "Unlock Pro")
                    }

                    TextButton(
                        onClick = {
                            scope.launch { billingHandler.restorePurchases() }
                        },
                        enabled = isInternetAllowed
                    ) {
                        Text("Restore Purchases")
                    }
                } else {
                    Button(
                        onClick = { billingHandler.resetProForTesting() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Pro (Testing)")
                    }
                }
            }
        }
    }
}
