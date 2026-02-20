package com.example.kivoicechat
import com.google.gson.annotations.SerializedName

data class ChatRequest(val model: String, val messages: List<ApiMessage>)
data class ApiMessage(val role: String, val content: String)
data class ChatResponse(val id: String, val choices: List<Choice>)
data class Choice(val message: ApiMessage, @SerializedName("finish_reason") val finishReason: String?)
