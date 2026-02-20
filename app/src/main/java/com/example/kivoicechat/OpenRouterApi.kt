package com.example.kivoicechat
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApi {
    @POST("api/v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authHeader: String,
        @Header("HTTP-Referer") referer: String = "https://kivoicechat.test", 
        @Header("X-Title") title: String = "Native KI App",
        @Body request: ChatRequest
    ): Response<ChatResponse>

    // NEU: Holt die Liste aller verfügbaren Modelle von OpenRouter
    @GET("api/v1/models")
    suspend fun getModels(): Response<ModelsResponse>
}
