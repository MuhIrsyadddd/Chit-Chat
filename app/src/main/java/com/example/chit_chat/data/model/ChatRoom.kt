package com.example.chit_chat.data.model

import com.google.firebase.Timestamp

data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val status: String = "active", // active, ended
    val createdAt: Timestamp? = null
)
