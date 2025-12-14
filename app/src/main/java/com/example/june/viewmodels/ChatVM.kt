package com.example.june.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.june.core.domain.ChatRepo
import com.example.june.core.domain.data_classes.Chat
import com.example.june.core.domain.data_classes.Message
import com.example.june.core.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatDetailState(
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false
)

class ChatVM(
    savedStateHandle: SavedStateHandle,
    private val chatRepo: ChatRepo
) : ViewModel() {

    private val routeArgs = savedStateHandle.toRoute<Route.Chat>()
    private val chatId = routeArgs.chatId

    private val _chatDetailState = MutableStateFlow(ChatDetailState())
    val chatDetailState = _chatDetailState.asStateFlow()

    init {
        loadChatDetail(chatId)
    }

    fun updateChat(chatId: Long, newName: String, newAvatarUrl: String?) {
        viewModelScope.launch {
            val currentChat = chatRepo.getChatById(chatId)

            if (currentChat != null) {
                val updatedChat = currentChat.copy(
                    chatName = newName,
                    chatAvatarUrl = newAvatarUrl,
                    updatedAt = System.currentTimeMillis()
                )
                chatRepo.updateChat(updatedChat)
            }
        }
    }

    private fun loadChatDetail(chatId: Long?) {
        if (chatId == null) return
        viewModelScope.launch {
            _chatDetailState.update { it.copy(isLoading = true) }

            chatRepo.getMessagesForChat(chatId).collect { messages ->
                val chat = chatRepo.getChatById(chatId)
                _chatDetailState.update { it.copy(
                    chat = chat,
                    messages = messages,
                    isLoading = false
                ) }
            }
        }
    }
}