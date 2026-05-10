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

    suspend fun tryMatch(userId: String): String {
        Log.d("ChatRepo", "tryMatch for $userId")
        
        // 1. Ambil kandidat tertua yang bukan kita
        val fiveMinutesAgo = Timestamp(Timestamp.now().seconds - 300, 0)
        val snapshot = queueCollection
            .whereGreaterThan("joinedAt", fiveMinutesAgo)
            .orderBy("joinedAt", Query.Direction.ASCENDING)
            .limit(5)
            .get().await()
            
        val waitingUserDoc = snapshot.documents.firstOrNull { it.id != userId }

        return firestore.runTransaction { transaction ->
            if (waitingUserDoc != null) {
                val otherUserId = waitingUserDoc.id
                val peerInQueue = transaction.get(queueCollection.document(otherUserId))
                
                if (peerInQueue.exists()) {
                    // Berhasil pairing! Buat room
                    val newRoomRef = roomsCollection.document()
                    val room = ChatRoom(
                        id = newRoomRef.id, 
                        participants = listOf(userId, otherUserId),
                        createdAt = Timestamp.now(),
                        status = "active"
                    )
                    
                    transaction.set(newRoomRef, room)
                    // Hapus KEDUANYA dari queue agar tidak dipasangkan lagi
                    transaction.delete(queueCollection.document(otherUserId))
                    transaction.delete(queueCollection.document(userId))
                    
                    Log.d("ChatRepo", "Matched with $otherUserId, created room ${newRoomRef.id}")
                    newRoomRef.id
                } else {
                    // Orang tersebut sudah diambil/keluar, masuk queue sendiri
                    Log.d("ChatRepo", "Peer was taken, joining queue")
                    transaction.set(queueCollection.document(userId), mapOf("joinedAt" to FieldValue.serverTimestamp()))
                    ""
                }
            } else {
                // Tidak ada lawan, masuk queue
                Log.d("ChatRepo", "No peers, joining queue")
                transaction.set(queueCollection.document(userId), mapOf("joinedAt" to FieldValue.serverTimestamp()))
                ""
            }
        }.await()
    }

    fun listenForIncomingMatch(userId: String): Flow<String?> = callbackFlow {
        // Mendengarkan room baru di mana kita adalah salah satu partisipannya
        val now = Timestamp.now()
        val subscription = roomsCollection
            .whereArrayContains("participants", userId)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                // Cari room yang createdAt-nya sangat baru (setelah kita mulai mencari)
                val matchedRoom = snapshot?.documents?.filter { 
                    val created = it.getTimestamp("createdAt")
                    created != null && created.seconds >= now.seconds - 10 // toleransi 10 detik
                }?.maxByOrNull { it.getTimestamp("createdAt")?.seconds ?: 0L }
                
                trySend(matchedRoom?.id)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun leaveQueue(userId: String) {
        try {
            queueCollection.document(userId).delete().await()
        } catch (e: Exception) {
            Log.e("ChatRepo", "Error leaving queue", e)
        }
    }

    suspend fun leaveRoom(roomId: String) {
        try {
            roomsCollection.document(roomId).update("status", "ended").await()
        } catch (e: Exception) {
            Log.e("ChatRepo", "Error leaving room", e)
        }
    }
}
