package com.example.june.core.presentation.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.june.R
import com.example.june.core.domain.data_classes.Message
import com.example.june.core.domain.utils.formatTime
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.presentation.components.JuneTopAppBar
import com.example.june.core.presentation.screens.settings.components.EditChatSheet
import com.example.june.viewmodels.ChatVM
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val navigator = koinInject<AppNavigator>()
    val chatViewModel: ChatVM = koinViewModel()
    val state by chatViewModel.chatDetailState.collectAsState()

    val chat = state.chat
    val messages = state.messages
    val isLoading = state.isLoading

    var showEditSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            JuneTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 0.dp)
                    ) {
                        val initials = chat?.chatName?.firstOrNull()?.uppercase() ?: "?"
                        val avatarUrl = chat?.chatAvatarUrl

                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (!avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Chat avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(text = chat?.chatName ?: "Chat")
                    }
                },
                navigationIcon = {
                    FilledIconButton(
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px,),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (chat != null) {
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(
                                painter = painterResource(R.drawable.edit_24px),
                                contentDescription = "Edit Chat Details"
                            )
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        if (messages.isEmpty()) {
            EmptyChat(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                reverseLayout = true
            ) {
                items(messages.reversed(), key = { it.id }) { message ->
                    MessageBubble(message = message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showEditSheet && chat != null) {
        EditChatSheet(
            chat = chat,
            onDismiss = { showEditSheet = false },
            onSave = { newName, newUri ->
                chatViewModel.updateChat(
                    chatId = chat.id,
                    newName = newName,
                    newAvatarUrl = newUri?.toString()
                )
            }
        )
    }
}

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isIncoming) Alignment.Start else Alignment.End
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isIncoming) 4.dp else 16.dp,
                bottomEnd = if (message.isIncoming) 16.dp else 4.dp
            ),
            color = if (message.isIncoming) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (message.isIncoming) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.messageContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isIncoming) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isIncoming) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyChat(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.forum_24px),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}