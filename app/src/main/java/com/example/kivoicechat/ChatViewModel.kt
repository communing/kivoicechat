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
            val aiResponseText = repository.sendMessage(userText)
            chatDao.insertMessage(ChatMessage(role = if (aiResponseText != null) "assistant" else "system", content = aiResponseText ?: "Fehler."))
        }
    }
}
