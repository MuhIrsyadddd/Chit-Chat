package com.example.chit_chat.ui.matchmaking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chit_chat.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MatchmakingState {
    object Idle : MatchmakingState()
    object Searching : MatchmakingState()
    data class Matched(val roomId: String) : MatchmakingState()
    data class Error(val message: String) : MatchmakingState()
    object Timeout : MatchmakingState()
}

class MatchmakingViewModel(
    private val repository: ChatRepository = ChatRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow<MatchmakingState>(MatchmakingState.Idle)
    val state: StateFlow<MatchmakingState> = _state.asStateFlow()

    private val userId: String get() = auth.currentUser?.uid ?: ""
    private var matchmakingJob: Job? = null
    private var listenerJob: Job? = null

    fun startMatchmaking() {
        if (userId.isEmpty()) {
            _state.value = MatchmakingState.Error("User tidak terautentikasi")
            return
        }
        
        stopMatchmaking()
        _state.value = MatchmakingState.Searching
        Log.d("Matchmaking", "Starting for user: $userId")
        
        matchmakingJob = viewModelScope.launch {
            try {
                // Jalankan listener secara paralel sebelum mencoba findMatch
                startListeningForMatch()

                val roomId = repository.findMatch(userId)
                if (roomId.isNotEmpty()) {
                    Log.d("Matchmaking", "Instantly matched with room: $roomId")
                    _state.value = MatchmakingState.Matched(roomId)
                } else {
                    Log.d("Matchmaking", "Entered queue, waiting for peer...")
                    // Timer 5 menit
                    launch {
                        delay(300000)
                        if (_state.value is MatchmakingState.Searching) {
                            Log.d("Matchmaking", "Timeout reached")
                            stopMatchmaking()
                            _state.value = MatchmakingState.Timeout
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Matchmaking", "Error during matchmaking", e)
                _state.value = MatchmakingState.Error(e.localizedMessage ?: "Gagal mencari teman")
            }
        }
    }

    private fun startListeningForMatch() {
        listenerJob?.cancel()
        listenerJob = viewModelScope.launch {
            repository.listenForMatch(userId).collect { matchedRoomId ->
                if (matchedRoomId != null && _state.value !is MatchmakingState.Matched) {
                    Log.d("Matchmaking", "Found match via listener: $matchedRoomId")
                    _state.value = MatchmakingState.Matched(matchedRoomId)
                    // Hentikan proses pencarian aktif jika sudah dapat via listener
                    matchmakingJob?.cancel()
                }
            }
        }
    }

    fun stopMatchmaking() {
        matchmakingJob?.cancel()
        listenerJob?.cancel()
        viewModelScope.launch {
            repository.leaveQueue(userId)
            if (_state.value is MatchmakingState.Searching) {
                _state.value = MatchmakingState.Idle
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMatchmaking()
    }
}
