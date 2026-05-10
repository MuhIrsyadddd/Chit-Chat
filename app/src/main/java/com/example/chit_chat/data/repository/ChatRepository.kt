package com.example.chit_chat.data.repository

import android.util.Log
import com.example.chit_chat.data.model.ChatRoom
import com.example.chit_chat.data.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val messagesCollection = firestore.collection("messages")
    private val roomsCollection = firestore.collection("rooms")
    private val queueCollection = firestore.collection("matchmaking_queue")

    fun getMessages(roomId: String): Flow<List<Message>> = callbackFlow {
        val subscription = messagesCollection
            .whereEqualTo("roomId", roomId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(messages)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun sendMessage(message: Message) {
        messagesCollection.add(message).await()
    }

    suspend fun findMatch(userId: String): String {
        Log.d("ChatRepo", "Starting findMatch for $userId")
        
        // Pengecekan kandidat user yang sedang menunggu (maksimal 5 menit yang lalu)
        val fiveMinutesAgo = Timestamp(Timestamp.now().seconds - 300, 0)
        val snapshot = queueCollection
            .whereGreaterThan("joinedAt", fiveMinutesAgo)
            .orderBy("joinedAt", Query.Direction.ASCENDING)
            .limit(10)
            .get().await()
            
        val waitingUserDoc = snapshot.documents.firstOrNull { it.id != userId }

        return firestore.runTransaction { transaction ->
            if (waitingUserDoc != null) {
                val otherUserId = waitingUserDoc.id
                val peerDoc = transaction.get(queueCollection.document(otherUserId))
                
                if (peerDoc.exists()) {
                    val newRoomRef = roomsCollection.document()
                    val room = ChatRoom(
                        id = newRoomRef.id, 
                        participants = listOf(userId, otherUserId),
                        createdAt = Timestamp.now()
                    )
                    
                    transaction.set(newRoomRef, room)
                    transaction.delete(queueCollection.document(otherUserId))
                    transaction.delete(queueCollection.document(userId))
                    
                    newRoomRef.id
                } else {
                    transaction.set(queueCollection.document(userId), mapOf("joinedAt" to FieldValue.serverTimestamp()))
                    ""
                }
            } else {
                transaction.set(queueCollection.document(userId), mapOf("joinedAt" to FieldValue.serverTimestamp()))
                ""
            }
        }.await()
    }

    suspend fun leaveQueue(userId: String) {
        try {
            queueCollection.document(userId).delete().await()
        } catch (e: Exception) {
            Log.e("ChatRepo", "Error leaving queue", e)
        }
    }

    fun listenForMatch(userId: String): Flow<String?> = callbackFlow {
        // Hanya dengarkan room yang baru dibuat (misal dalam 1 menit terakhir)
        val oneMinuteAgo = Timestamp(Timestamp.now().seconds - 60, 0)
        
        val subscription = roomsCollection
            .whereArrayContains("participants", userId)
            .whereGreaterThan("createdAt", oneMinuteAgo)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                val roomId = snapshot?.documents?.maxByOrNull { 
                    it.getTimestamp("createdAt")?.seconds ?: 0L 
                }?.id
                
                trySend(roomId)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun leaveRoom(roomId: String) {
        try {
            roomsCollection.document(roomId).update("status", "ended").await()
        } catch (e: Exception) {
            Log.e("ChatRepo", "Error leaving room", e)
        }
    }
}
