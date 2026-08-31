package com.example.birthdaycountdown.ai

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore

class AiPreferences(private val prefs: SharedPreferences) {
    fun read(): AiSettings = AiSettings(readConfig("chat"), readConfig("image"))

    fun write(settings: AiSettings) {
        writeConfig("chat", settings.chat)
        writeConfig("image", settings.image)
    }

    private fun readConfig(prefix: String) = AiEndpointConfig(
        baseUrl = prefs.getString("ai_${prefix}_base_url", "https://wawapii.com/v1").orEmpty(),
        apiKey = readSecret("ai_${prefix}_api_key"),
        model = prefs.getString("ai_${prefix}_model", "").orEmpty()
    )

    private fun writeConfig(prefix: String, config: AiEndpointConfig) {
        prefs.edit()
            .putString("ai_${prefix}_base_url", config.baseUrl)
            .putString("ai_${prefix}_model", config.model)
            .apply()
        writeSecret("ai_${prefix}_api_key", config.apiKey)
    }

    private fun readSecret(key: String): String {
        val encrypted = prefs.getString(key, null) ?: return ""
        val iv = prefs.getString("${key}_iv", null) ?: return encrypted
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.DEFAULT)))
            String(cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT)), Charsets.UTF_8)
        }.getOrElse { encrypted }
    }

    private fun writeSecret(key: String, value: String) {
        if (value.isBlank()) {
            prefs.edit().remove(key).remove("${key}_iv").apply()
            return
        }
        runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            prefs.edit()
                .putString(key, Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP))
                .putString("${key}_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .apply()
        }.onFailure {
            prefs.edit().putString(key, value).remove("${key}_iv").apply()
        }
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    private companion object { const val KEY_ALIAS = "time_planning_ai_key" }
}
