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
import org.json.JSONArray
import org.json.JSONObject

class AiPreferences(private val prefs: SharedPreferences) {
    fun read(): AiSettings {
        val chat = readProfiles("chat", "AI 对话")
        val image = readProfiles("image", "AI 生图")
        return AiSettings(chat, image, prefs.getString("ai_chat_selected", null), prefs.getString("ai_image_selected", null))
    }

    fun write(settings: AiSettings) {
        writeProfiles("chat", settings.chatProfiles, settings.selectedChatId)
        writeProfiles("image", settings.imageProfiles, settings.selectedImageId)
    }

    private fun readProfiles(prefix: String, label: String): List<AiProviderProfile> {
        val raw = prefs.getString("ai_${prefix}_profiles", null)
        if (raw.isNullOrBlank()) {
            val legacy = AiEndpointConfig(
                baseUrl = prefs.getString("ai_${prefix}_base_url", "https://wawapii.com/v1").orEmpty(),
                apiKey = readSecret("ai_${prefix}_api_key"),
                model = prefs.getString("ai_${prefix}_model", "").orEmpty()
            )
            return listOf(AiProviderProfile("$prefix-default", "$label 1", legacy))
        }
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val id = item.getString("id")
                    add(AiProviderProfile(id, item.optString("name", "$label ${index + 1}"), AiEndpointConfig(
                        item.optString("baseUrl", "https://wawapii.com/v1"), readSecret("ai_${prefix}_${id}_api_key"), item.optString("model")
                    )))
                }
            }.ifEmpty { listOf(AiProviderProfile("$prefix-default", "$label 1", AiEndpointConfig())) }
        }.getOrElse { listOf(AiProviderProfile("$prefix-default", "$label 1", AiEndpointConfig())) }
    }

    private fun writeProfiles(prefix: String, profiles: List<AiProviderProfile>, selectedId: String?) {
        val oldIds = runCatching {
            val array = JSONArray(prefs.getString("ai_${prefix}_profiles", "[]"))
            buildSet { for (index in 0 until array.length()) add(array.getJSONObject(index).getString("id")) }
        }.getOrDefault(emptySet())
        val metadata = JSONArray()
        profiles.forEachIndexed { index, profile ->
            metadata.put(JSONObject().put("id", profile.id).put("name", profile.name.ifBlank { "$prefix ${index + 1}" }).put("baseUrl", profile.config.baseUrl).put("model", profile.config.model))
            writeSecret("ai_${prefix}_${profile.id}_api_key", profile.config.apiKey)
        }
        prefs.edit().apply {
            putString("ai_${prefix}_profiles", metadata.toString())
            putString("ai_${prefix}_selected", selectedProvider(profiles, selectedId)?.id)
            (oldIds - profiles.map { it.id }.toSet()).forEach { id -> remove("ai_${prefix}_${id}_api_key"); remove("ai_${prefix}_${id}_api_key_iv") }
            remove("ai_${prefix}_api_key")
            remove("ai_${prefix}_api_key_iv")
        }.apply()
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
