package com.denser.june.presentation.screens.settings.tiles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.denser.june.core.R
import com.denser.june.core.utils.Constants
import org.koin.compose.koinInject
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

@Composable
fun DeveloperTile() {
    val uriHandler = LocalUriHandler.current
    val imageLoader = koinInject<ImageLoader>()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = Constants.DEVELOPER_IMAGE,
                    imageLoader = imageLoader,
                    contentDescription = "Developer Profile Picture",
                    placeholder = painterResource(R.drawable.person_24px_fill),
                    error = painterResource(R.drawable.person_24px_fill),
                    fallback = painterResource(R.drawable.person_24px_fill),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = Constants.DEVELOPER_NAME,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Developer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 64.dp)
            ) {
                FilledTonalButton(
                    onClick = { uriHandler.openUri(Constants.DEVELOPER_GITHUB_URL) },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.github),
                        contentDescription = stringResource(R.string.brand_github),
                        modifier = Modifier.size(18.dp)
                    )
                }
                FilledTonalButton(
                    onClick = { uriHandler.openUri(Constants.BUY_ME_A_COFFEE_URL) },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.buy_me_a_coffee),
                        contentDescription = stringResource(R.string.brand_buy_me_a_coffee),
                        modifier = Modifier.size(18.dp)
                    )
                }
                FilledTonalButton(
                    onClick = { uriHandler.openUri(Constants.DEVELOPER_EMAIL) },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.alternate_email_24px),
                        contentDescription = stringResource(R.string.brand_email),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
