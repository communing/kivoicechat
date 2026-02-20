package com.example.kivoicechat
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ChatRepository(private val apiKeyManager: ApiKeyManager) {
    
    // NEU: Wir erhöhen die Timeouts von 10 auf 90 Sekunden!
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
        val apiKey = apiKeyManager.getApiKey() ?: return Pair("Fehler: Kein API-Key.", "system")
        return try {
            val response = api.getChatCompletion("Bearer $apiKey", request = ChatRequest(requestedModel, listOf(ApiMessage("user", userText))))
            val body = response.body()
            val text = body?.choices?.firstOrNull()?.message?.content
            val actualModel = body?.model ?: requestedModel
            if (response.isSuccessful && text != null) Pair(text, actualModel) else Pair("Fehler: Code ${response.code()}", "system")
        } catch (e: Exception) { 
            Log.e("ChatRepository", "Netzwerkfehler", e)
            Pair("Netzwerk- oder Timeout-Fehler. Das Modell hat zu lange gebraucht oder das Internet ist weg.", "system") 
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
