package com.example.kivoicechat
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurityVault {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val alias = "OpenRouterApiKeyAlias"
    init { if (!keyStore.containsAlias(alias)) generateKey() }

    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }
    fun encrypt(data: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = keyStore.getKey(alias, null) as SecretKey
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.iv + cipher.doFinal(data.toByteArray(Charsets.UTF_8))
    }
    fun decrypt(data: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = keyStore.getKey(alias, null) as SecretKey
        val iv = data.copyOfRange(0, 12)
        val encryptedData = data.copyOfRange(12, data.size)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encryptedData), Charsets.UTF_8)
    }
}
