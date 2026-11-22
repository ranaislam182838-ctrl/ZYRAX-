package com.zyrax.ai.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore

object GeminiClient {
    private const val PREFS = "zyrax_secure_gemini"
    private const val KEY = "encrypted_api_key"
    private const val ALIAS = "zyrax_gemini_keystore_key"
    private const val MODEL = "gemini-2.5-flash"

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val combined = cipher.iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        require(combined.size > 12) { "Encrypted key data is invalid." }
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(128, iv)
        )
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun hasApiKey(context: Context): Boolean =
        !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null).isNullOrBlank()

    fun saveApiKey(context: Context, apiKey: String) {
        require(apiKey.trim().isNotEmpty()) { "API Key খালি রাখা যাবে না।" }
        val encrypted = encrypt(apiKey.trim())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, encrypted).apply()
    }

    fun clearApiKey(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY).apply()
    }

    private fun readApiKey(context: Context): String {
        val encrypted = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?: throw IllegalStateException("Settings-এ Gemini API Key যোগ করুন।")
        return decrypt(encrypted)
    }

    suspend fun generateReply(
        context: Context,
        prompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        require(prompt.isNotBlank()) { "মেসেজ খালি।" }
        val apiKey = readApiKey(context)

        val contents = JSONArray()
        history.takeLast(12).forEach { (role, message) ->
            contents.put(
                JSONObject()
                    .put("role", if (role == "model") "model" else "user")
                    .put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", message))
                    )
            )
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        )

        val requestBody = JSONObject()
            .put("contents", contents)
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.7)
                    .put("maxOutputTokens", 1024)
            )

        val connection = (URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
        ).openConnection() as HttpURLConnection)

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20000
            connection.readTimeout = 45000
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8"
            )
            connection.setRequestProperty("x-goog-api-key", apiKey)

            connection.outputStream.use {
                it.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                val detail = runCatching {
                    JSONObject(response).getJSONObject("error")
                        .optString("message")
                }.getOrDefault("HTTP $status")
                throw IllegalStateException("Gemini API error ($status): $detail")
            }

            val candidates = JSONObject(response).optJSONArray("candidates")
                ?: throw IllegalStateException("Gemini কোনো উত্তর দেয়নি।")

            val parts = candidates.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?: throw IllegalStateException("Gemini উত্তর পাওয়া যায়নি।")

            val answer = buildString {
                for (index in 0 until parts.length()) {
                    append(parts.optJSONObject(index)?.optString("text").orEmpty())
                }
            }.trim()

            if (answer.isBlank()) {
                throw IllegalStateException("Gemini খালি উত্তর দিয়েছে।")
            }
            answer
        } finally {
            connection.disconnect()
        }
    }
}
