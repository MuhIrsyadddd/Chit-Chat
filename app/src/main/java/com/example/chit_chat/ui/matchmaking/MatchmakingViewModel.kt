package com.example.chit_chat.ui.matchmaking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chit_chat.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
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

    fun startMatchmaking() {
        if (userId.isEmpty()) {
            _state.value = MatchmakingState.Error("Login diperlukan")
            return
        }
        
        // Hapus pemanggilan stopMatchmaking() di sini untuk mencegah reset state instan
        if (_state.value is MatchmakingState.Searching) return 

        _state.value = MatchmakingState.Searching
        Log.d("Matchmaking", "Start searching: $userId")
        
        matchmakingJob = viewModelScope.launch {
            try {
                // 1. Jalankan Listener
                launch {
                    repository.listenForIncomingMatch(userId).collect { roomId ->
                        if (roomId != null && _state.value is MatchmakingState.Searching) {
                            Log.d("Matchmaking", "Invited to room: $roomId")
                            _state.value = MatchmakingState.Matched(roomId)
                        }
                    }
                }

                // 2. Timer Timeout 5 Menit
                launch {
                    delay(300000)
                    if (_state.value is MatchmakingState.Searching) {
                        Log.d("Matchmaking", "Searching timeout")
                        _state.value = MatchmakingState.Timeout
                        stopMatchmakingInternal()
                    }
                }

                // 3. Loop Pencarian Aktif (Polling)
                while (_state.value is MatchmakingState.Searching) {
                    val matchedRoomId = repository.tryMatch(userId)
                    if (matchedRoomId.isNotEmpty()) {
                        Log.d("Matchmaking", "Found peer actively: $matchedRoomId")
                        _state.value = MatchmakingState.Matched(matchedRoomId)
                        break
                    }
                    delay(5000) 
                }
            } catch (e: CancellationException) {
                Log.d("Matchmaking", "Job cancelled intentionally")
            } catch (e: Exception) {
                Log.e("Matchmaking", "Search error", e)
                // Tangani error index khusus
                if (e.message?.contains("index") == true) {
                    _state.value = MatchmakingState.Error("Firestore memerlukan Index. Cek logcat untuk link pembuatannya.")
                } else {
                    _state.value = MatchmakingState.Error("Koneksi bermasalah: ${e.localizedMessage}")
                }
            }
        }
    }

    private suspend fun stopMatchmakingInternal() {
        matchmakingJob?.cancel()
        matchmakingJob = null
        repository.leaveQueue(userId)
    }

    fun stopMatchmaking() {
        matchmakingJob?.cancel()
        matchmakingJob = null
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
