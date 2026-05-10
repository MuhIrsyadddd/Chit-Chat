package com.example.chit_chat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chit_chat.data.model.Message
import com.example.chit_chat.data.repository.ChatRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var _roomId = MutableStateFlow<String?>(null)
    val roomId: StateFlow<String?> = _roomId.asStateFlow()

    val currentUserId: String
        get() = auth.currentUser?.uid ?: "anonymous"

    val currentUserName: String
        get() = auth.currentUser?.displayName ?: "User ${currentUserId.takeLast(4)}"

    fun setRoom(id: String) {
        _roomId.value = id
        listenForMessages(id)
    }

    private fun listenForMessages(id: String) {
        viewModelScope.launch {
            try {
                repository.getMessages(id).collect {
                    _messages.value = it
                    _error.value = null
                }
            } catch (e: Exception) {
                _error.value = "Gagal memuat pesan: ${e.localizedMessage}"
            }
        }
    }

    fun sendMessage(content: String) {
        val currentRoomId = _roomId.value ?: return
        if (content.isBlank()) return

        val newMessage = Message(
            roomId = currentRoomId,
            senderId = currentUserId,
            senderName = currentUserName,
            content = content,
            timestamp = Timestamp.now()
        )

        viewModelScope.launch {
            try {
                repository.sendMessage(newMessage)
            } catch (e: Exception) {
                _error.value = "Gagal mengirim: ${e.localizedMessage}"
            }
        }
    }

    fun leaveRoom(onComplete: () -> Unit) {
        val id = _roomId.value ?: return
        viewModelScope.launch {
            repository.leaveRoom(id)
            _roomId.value = null
            onComplete()
        }
    }
}
