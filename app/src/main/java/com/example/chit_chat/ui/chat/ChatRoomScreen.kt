package com.example.chit_chat.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chit_chat.data.model.Message
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    roomId: String,
    onLeaveRoom: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(roomId) {
        viewModel.setRoom(roomId)
    }

    ChatRoomLayout(
        messages = messages,
        error = error,
        currentUserId = viewModel.currentUserId,
        onSendMessage = { viewModel.sendMessage(it) },
        onLeaveRoom = {
            viewModel.leaveRoom(onLeaveRoom)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomLayout(
    messages: List<Message>,
    error: String?,
    currentUserId: String,
    onSendMessage: (String) -> Unit,
    onLeaveRoom: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChitChat") },
                actions = {
                    TextButton(onClick = onLeaveRoom) {
                        Text("Keluar", color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            ChatRoomContent(
                messages = messages,
                currentUserId = currentUserId,
                onSendMessage = onSendMessage
            )
        }
    }
}

@Composable
fun ChatRoomContent(
    messages: List<Message>,
    currentUserId: String,
    onSendMessage: (String) -> Unit
) {
    var textState by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = false
        ) {
            items(messages, key = { it.id.ifEmpty { it.timestamp?.seconds.toString() + it.content.hashCode() } }) { message ->
                MessageItem(
                    message = message,
                    isCurrentUser = message.senderId == currentUserId
                )
            }
        }

        ChatInput(
            text = textState,
            onTextChange = { textState = it },
            onSend = {
                onSendMessage(textState)
                textState = ""
            }
        )
    }
}

@Composable
fun MessageItem(message: Message, isCurrentUser: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 0.dp,
                bottomEnd = if (isCurrentUser) 0.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isCurrentUser) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isCurrentUser) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentUser) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ketik pesan...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                ),
                maxLines = 4
            )
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim",
                    tint = if (text.isNotBlank()) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Chat Room - Standard View")
@Composable
fun ChatRoomPreviewStandard() {
    MaterialTheme {
        ChatRoomLayout(
            messages = listOf(
                Message(id = "1", senderId = "user1", senderName = "Budi", content = "Halo!", timestamp = Timestamp.now()),
                Message(id = "2", senderId = "me", senderName = "Saya", content = "Hai Budi, apa kabar?", timestamp = Timestamp.now()),
                Message(id = "3", senderId = "user1", senderName = "Budi", content = "Baik! Kamu?", timestamp = Timestamp.now())
            ),
            error = null,
            currentUserId = "me",
            onSendMessage = {},
            onLeaveRoom = {}
        )
    }
}

@Preview(showBackground = true, name = "Chat Room - Error View")
@Composable
fun ChatRoomPreviewError() {
    MaterialTheme {
        ChatRoomLayout(
            messages = emptyList(),
            error = "Gagal memuat pesan. Periksa koneksi internet atau Firebase Rules.",
            currentUserId = "me",
            onSendMessage = {},
            onLeaveRoom = {}
        )
    }
}
