package com.example.kivoicechat
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String,
    val content: String,
    val modelName: String? = null, // NEU: Speichert das verwendete Modell
    val timestamp: Long = System.currentTimeMillis()
)
