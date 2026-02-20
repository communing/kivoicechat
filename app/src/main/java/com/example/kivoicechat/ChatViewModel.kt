package com.example.kivoicechat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository, private val chatDao: ChatDao) : ViewModel() {
    val messages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    // NEU: Speichert unsere dynamische Modell-Liste
    private val _modelList = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val modelList: StateFlow<List<Pair<String, String>>> = _modelList

    init {
        // Lädt die Modelle automatisch beim Start der App
        loadModels()
    }

    fun loadModels() {
        viewModelScope.launch {
            val rawModels = repository.fetchAvailableModels() ?: return@launch
            
            val sortedList = mutableListOf<Pair<String, String>>()
            
            // 1. Platz: Immer das Auto-Modell
            sortedList.add("openrouter/auto" to "🌟 Auto (OpenRouter entscheidet)")

            // Wir trennen die Modelle in "Kostenlos" und "Kostenpflichtig"
            val freeModels = mutableListOf<Pair<String, String>>()
            val paidModels = mutableListOf<Pair<String, String>>()

            for (model in rawModels) {
                if (model.id == "openrouter/auto") continue // Schon ganz oben hinzugefügt
                
                // Prüfen ob das Modell kostenlos ist (Prompt & Completion kosten 0)
                val isFree = model.pricing?.prompt == "0" && model.pricing?.completion == "0"
                val displayName = if (isFree) "🆓 ${model.name}" else "💰 ${model.name}"
                
                if (isFree || model.id.contains("free", ignoreCase = true)) {
                    freeModels.add(model.id to displayName)
                } else {
                    paidModels.add(model.id to displayName)
                }
            }

            // 2. Platz: Alle kostenlosen Modelle alphabetisch
            sortedList.addAll(freeModels.sortedBy { it.second })
            // 3. Platz: Alle kostenpflichtigen Modelle alphabetisch
            sortedList.addAll(paidModels.sortedBy { it.second })

            _modelList.value = sortedList
        }
    }

    fun sendMessage(userText: String, modelId: String) {
        viewModelScope.launch {
            chatDao.insertMessage(ChatMessage(role = "user", content = userText))
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
