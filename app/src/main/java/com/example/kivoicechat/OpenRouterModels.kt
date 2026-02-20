package com.example.kivoicechat
import com.google.gson.annotations.SerializedName

data class ChatRequest(val model: String, val messages: List<ApiMessage>)
data class ApiMessage(val role: String, val content: String?)

// NEU: Alles was von OpenRouter kommt, ist jetzt "nullable" (?), 
// damit die App nicht abstürzt, falls ein Feld mal fehlt!
data class ChatResponse(
    val id: String?, 
    val model: String?, 
    val choices: List<Choice>?
)
data class Choice(
    val message: ApiMessage?, 
    @SerializedName("finish_reason") val finishReason: String?
)

data class ModelsResponse(val data: List<ModelData>?)
data class ModelData(val id: String, val name: String, val pricing: Pricing?)
data class Pricing(val prompt: String?, val completion: String?)
