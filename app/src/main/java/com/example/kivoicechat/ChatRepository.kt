package com.example.kivoicechat
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ChatRepository(private val apiKeyManager: ApiKeyManager) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenRouterApi::class.java)

    suspend fun sendMessage(userText: String, requestedModel: String = "openrouter/auto"): Pair<String, String>? {
        val apiKey = apiKeyManager.getApiKey() ?: return Pair("Fehler: Kein API-Key hinterlegt.", "system")
        return try {
            val response = api.getChatCompletion("Bearer $apiKey", request = ChatRequest(requestedModel, listOf(ApiMessage("user", userText))))
            
            if (response.isSuccessful) {
                val body = response.body()
                // Wir navigieren sicher durch die Antwort (mit ?.)
                val text = body?.choices?.firstOrNull()?.message?.content ?: "Kein Text in der Antwort gefunden."
                val actualModel = body?.model ?: requestedModel
                Pair(text, actualModel)
            } else {
                // Wenn OpenRouter z.B. Error 429 (Rate Limit) oder 402 (Guthaben leer) schickt
                Pair("API Fehler Code ${response.code()}: ${response.errorBody()?.string()}", "system")
            }
        } catch (e: Exception) { 
            Log.e("ChatRepository", "System-Fehler", e)
            // NEU: Gibt den exakten Java/Kotlin Fehlergrund im Chat aus!
            Pair("System-Fehler: ${e.localizedMessage ?: e.javaClass.simpleName}", "system") 
        }
    }

    suspend fun fetchAvailableModels(): List<ModelData>? {
        return try {
            val response = api.getModels()
            if (response.isSuccessful) response.body()?.data else null
        } catch (e: Exception) {
            Log.e("ChatRepository", "Fehler beim Laden der Modelle", e)
            null
        }
    }
}
