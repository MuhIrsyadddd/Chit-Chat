package com.example.chit_chat.ui.matchmaking

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MatchmakingScreen(
    onMatched: (String) -> Unit,
    viewModel: MatchmakingViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is MatchmakingState.Matched) {
            onMatched((state as MatchmakingState.Matched).roomId)
        }
    }

    MatchmakingContent(
        state = state,
        onStartMatchmaking = { viewModel.startMatchmaking() },
        onStopMatchmaking = { viewModel.stopMatchmaking() }
    )
}

@Composable
fun MatchmakingContent(
    state: MatchmakingState,
    onStartMatchmaking: () -> Unit,
    onStopMatchmaking: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Cari Teman Chat",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        when (state) {
            is MatchmakingState.Idle, is MatchmakingState.Error, is MatchmakingState.Timeout -> {
                if (state is MatchmakingState.Error) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
                if (state is MatchmakingState.Timeout) {
                    Text(text = "Waktu habis (5 menit). Tidak ada teman ditemukan.", color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onStartMatchmaking) {
                    Text("Mulai Matchmaking")
                }
            }
            is MatchmakingState.Searching -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Mencari pasangan chat (max 5 menit)...")
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButton(onClick = onStopMatchmaking) {
                    Text("Batal")
                }
            }
            is MatchmakingState.Matched -> {
                Text("Berhasil Menemukan Teman! Menghubungkan...")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MatchmakingPreviewSearching() {
    MaterialTheme {
        MatchmakingContent(
            state = MatchmakingState.Searching, 
            onStartMatchmaking = {}, 
            onStopMatchmaking = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MatchmakingPreviewIdle() {
    MaterialTheme {
        MatchmakingContent(
            state = MatchmakingState.Idle, 
            onStartMatchmaking = {}, 
            onStopMatchmaking = {}
        )
    }
}
