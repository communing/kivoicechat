package com.example.kivoicechat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository, private val chatDao: ChatDao) : ViewModel() {
    val messages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    fun sendMessage(userText: String) {
        viewModelScope.launch {
            chatDao.insertMessage(ChatMessage(role = "user", content = userText))
            
            val result = repository.sendMessage(userText) // Nutzt jetzt openrouter/auto
            
            if (result != null) {
                val (text, modelUsed) = result
                // Speichert die KI-Antwort zusammen mit dem Namen des verwendeten Modells
                chatDao.insertMessage(ChatMessage(role = "assistant", content = text, modelName = modelUsed))
            } else {
                chatDao.insertMessage(ChatMessage(role = "system", content = "Fehler beim Abrufen.", modelName = "system"))
            }
        }
    }
}
