package com.zyrax.ai.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Saves settings, chat history and memory on the phone (private app storage). */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("zyrax_store", Context.MODE_PRIVATE)

    fun loadSettings(): AppSettings = AppSettings(
        provider = runCatching { Provider.valueOf(prefs.getString("provider", "") ?: "") }
            .getOrDefault(Provider.CLAUDE),
        apiKey = prefs.getString("apiKey", "") ?: "",
        model = prefs.getString("model", "") ?: "",
        language = runCatching { AppLanguage.valueOf(prefs.getString("language", "") ?: "") }
            .getOrDefault(AppLanguage.EN),
        speakReplies = prefs.getBoolean("speak", true)
    )

    fun saveSettings(s: AppSettings) {
        prefs.edit()
            .putString("provider", s.provider.name)
            .putString("apiKey", s.apiKey)
            .putString("model", s.model)
            .putString("language", s.language.name)
            .putBoolean("speak", s.speakReplies)
            .apply()
    }

    fun loadChat(p: Persona): List<ChatMessage> {
        val raw = prefs.getString("chat_${p.name}", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                ChatMessage(o.getLong("id"), o.getString("role"), o.getString("text"))
            }
        }.getOrDefault(emptyList())
    }

    fun saveChat(p: Persona, list: List<ChatMessage>) {
        val arr = JSONArray()
        list.takeLast(200).forEach {
            arr.put(JSONObject().put("id", it.id).put("role", it.role).put("text", it.text))
        }
        prefs.edit().putString("chat_${p.name}", arr.toString()).apply()
    }

    fun loadMemory(): List<MemoryItem> {
        val raw = prefs.getString("memory", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                MemoryItem(o.getLong("id"), o.getString("text"))
            }
        }.getOrDefault(emptyList())
    }

    fun saveMemory(list: List<MemoryItem>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("text", it.text)) }
        prefs.edit().putString("memory", arr.toString()).apply()
    }
}
