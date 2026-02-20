package com.example.kivoicechat
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

class ApiKeyManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val securityVault = SecurityVault()

    fun saveApiKey(apiKey: String) {
        val encryptedBytes = securityVault.encrypt(apiKey)
        prefs.edit().putString("openrouter_api_key", Base64.encodeToString(encryptedBytes, Base64.DEFAULT)).apply()
    }
    fun getApiKey(): String? {
        val base64Key = prefs.getString("openrouter_api_key", null) ?: return null
        return try { securityVault.decrypt(Base64.decode(base64Key, Base64.DEFAULT)) } catch (e: Exception) { null }
    }
    fun hasApiKey(): Boolean = prefs.contains("openrouter_api_key")
}
