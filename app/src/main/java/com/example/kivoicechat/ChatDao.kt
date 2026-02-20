package com.example.kivoicechat
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert suspend fun insertMessage(message: ChatMessage)
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC") fun getAllMessages(): Flow<List<ChatMessage>>
}
