package com.example.june.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.june.core.domain.ChatRepo
import com.example.june.core.domain.data_classes.Chat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatListState(
    val chats: List<Chat> = emptyList(),
    val isLoading: Boolean = false
)

class HomeChatVM(
    private val chatRepo: ChatRepo
) : ViewModel() {

    private val _chatListState = MutableStateFlow(ChatListState())
    val chatListState = _chatListState.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            _chatListState.update { it.copy(isLoading = true) }
            chatRepo.getAllChats().collect { chats ->
                _chatListState.update { it.copy(chats = chats, isLoading = false) }
            }
        }
    }

    fun createChat(name: String, avatarUrl: String?) {
        viewModelScope.launch {
            val newChat = Chat(
                chatName = name,
                chatAvatarUrl = avatarUrl,
                lastMessagePreview = "No messages yet",
                lastMessageTimestamp = System.currentTimeMillis()
            )
            chatRepo.insertChat(newChat)
        }
    }
}