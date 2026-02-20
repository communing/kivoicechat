package com.example.kivoicechat
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatRepository(private val apiKeyManager: ApiKeyManager) {
    private val client = OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }).build()
    private val api = Retrofit.Builder().baseUrl("https://openrouter.ai/").client(client).addConverterFactory(GsonConverterFactory.create()).build().create(OpenRouterApi::class.java)

    // Gibt nun ein Pair (Text, Modellname) zurück. Standard ist jetzt "openrouter/auto"!
    suspend fun sendMessage(userText: String, requestedModel: String = "openrouter/auto"): Pair<String, String>? {
        val apiKey = apiKeyManager.getApiKey() ?: return Pair("Fehler: Kein API-Key.", "system")
        return try {
            val response = api.getChatCompletion("Bearer $apiKey", request = ChatRequest(requestedModel, listOf(ApiMessage("user", userText))))
            val body = response.body()
            
            val text = body?.choices?.firstOrNull()?.message?.content
            val actualModel = body?.model ?: requestedModel // Das tatsächliche Modell
            
            if (response.isSuccessful && text != null) Pair(text, actualModel) else Pair("Fehler: Code ${response.code()}", "system")
        } catch (e: Exception) { Pair("Netzwerkfehler.", "system") }
    }
}
