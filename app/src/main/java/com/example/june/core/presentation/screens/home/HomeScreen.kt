package com.example.june.core.presentation.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.Route
import com.example.june.core.presentation.screens.home.components.FloatingBottomBar
import com.example.june.core.presentation.screens.home.journals.JournalsPage
import com.example.june.viewmodels.HomeVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

enum class NavItem(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    JOURNALS("Journals", Icons.AutoMirrored.Outlined.Note, Icons.AutoMirrored.Filled.Note),
    CHATS("Chats", Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Filled.Chat),
    ARCHIVE("Archive", Icons.Outlined.Archive, Icons.Filled.Archive)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen() {
    val navigator = koinInject<AppNavigator>()

    val viewModel: HomeVM = koinViewModel()
    val journals by viewModel.journals.collectAsStateWithLifecycle()

    var selectedNavItem by remember { mutableStateOf(NavItem.JOURNALS) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "June",
                            style = MaterialTheme.typography.displaySmallEmphasized,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        FilledTonalIconButton(
                            onClick = { navigator.navigateTo(Route.Settings) },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                when (selectedNavItem) {
                    NavItem.JOURNALS -> JournalsPage(
                        journals = journals
                    )

                    NavItem.CHATS -> ChatsContent()
                    NavItem.ARCHIVE -> ArchiveContent()
                }
            }
        }
        FloatingBottomBar(
            selectedItem = selectedNavItem,
            onItemSelected = { selectedNavItem = it },
            onFabClick = { navigator.navigateTo(Route.Journal(null)) },
        )

    }
}

@Composable
fun ChatsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = NavItem.CHATS.icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No chats yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ArchiveContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = NavItem.ARCHIVE.icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Archive is empty",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}