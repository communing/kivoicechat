package com.example.kivoicechat
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatRepository(private val apiKeyManager: ApiKeyManager) {
    private val client = OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }).build()
    private val api = Retrofit.Builder().baseUrl("https://openrouter.ai/").client(client).addConverterFactory(GsonConverterFactory.create()).build().create(OpenRouterApi::class.java)

    suspend fun sendMessage(userText: String, model: String = "google/gemini-2.5-flash"): String? {
        val apiKey = apiKeyManager.getApiKey() ?: return "Fehler: Kein API-Key."
        return try {
            val response = api.getChatCompletion("Bearer $apiKey", request = ChatRequest(model, listOf(ApiMessage("user", userText))))
            if (response.isSuccessful) response.body()?.choices?.firstOrNull()?.message?.content else "Fehler: Code ${response.code()}"
        } catch (e: Exception) { "Netzwerkfehler." }
    }
}
