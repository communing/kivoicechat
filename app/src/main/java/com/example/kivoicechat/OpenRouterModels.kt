package com.example.kivoicechat
import com.google.gson.annotations.SerializedName

// --- Für das Chatten ---
data class ChatRequest(val model: String, val messages: List<ApiMessage>)
data class ApiMessage(val role: String, val content: String)
data class ChatResponse(val id: String, val model: String, val choices: List<Choice>)
data class Choice(val message: ApiMessage, @SerializedName("finish_reason") val finishReason: String?)

// --- NEU: Für das Abrufen der Modelle ---
data class ModelsResponse(val data: List<ModelData>)
data class ModelData(
    val id: String, 
    val name: String, 
    val pricing: Pricing?
)
data class Pricing(
    val prompt: String?, 
    val completion: String?
)
