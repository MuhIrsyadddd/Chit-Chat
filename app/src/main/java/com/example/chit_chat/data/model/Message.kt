package com.example.chit_chat.data.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null
)
