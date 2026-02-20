package com.example.kivoicechat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository, private val chatDao: ChatDao) : ViewModel() {
    val messages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    // NEU: Die Funktion nimmt jetzt auch die modelId entgegen
    fun sendMessage(userText: String, modelId: String) {
        viewModelScope.launch {
            chatDao.insertMessage(ChatMessage(role = "user", content = userText))
            
            // Wir übergeben das ausgewählte Modell an das Repository
            val result = repository.sendMessage(userText, requestedModel = modelId) 
            
            if (result != null) {
                val (text, modelUsed) = result
                chatDao.insertMessage(ChatMessage(role = "assistant", content = text, modelName = modelUsed))
            } else {
                chatDao.insertMessage(ChatMessage(role = "system", content = "Fehler beim Abrufen.", modelName = "system"))
            }
        }
    }
}
