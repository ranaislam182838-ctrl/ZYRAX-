package com.zyrax.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Talks to the AI provider using the API key the user typed in Settings. */
object AiClient {

    suspend fun reply(settings: AppSettings, system: String, history: List<ChatMessage>): String =
        withContext(Dispatchers.IO) {
            val turns = normalize(history)
            when (settings.provider) {
                Provider.CLAUDE -> claude(settings, system, turns)
                Provider.OPENAI -> openai(settings, system, turns)
            }
        }

    /** Providers need turns that start with the user and alternate roles. */
    private fun normalize(history: List<ChatMessage>): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        for (m in history.takeLast(24)) {
            if (out.isEmpty() && m.role != "user") continue
            if (out.isNotEmpty() && out.last().first == m.role) {
                out[out.size - 1] = m.role to (out.last().second + "\n" + m.text)
            } else {
                out.add(m.role to m.text)
            }
        }
        return out
    }

    private fun claude(s: AppSettings, system: String, turns: List<Pair<String, String>>): String {
        val msgs = JSONArray()
        turns.forEach { (r, t) -> msgs.put(JSONObject().put("role", r).put("content", t)) }
        val body = JSONObject()
            .put("model", s.effectiveModel)
            .put("max_tokens", 1024)
            .put("system", system)
            .put("messages", msgs)
            .toString()
        val resp = JSONObject(
            post(
                "https://api.anthropic.com/v1/messages",
                mapOf(
                    "x-api-key" to s.apiKey,
                    "anthropic-version" to "2023-06-01",
                    "content-type" to "application/json"
                ),
                body
            )
        )
        val content = resp.getJSONArray("content")
        val sb = StringBuilder()
        for (i in 0 until content.length()) {
            val block = content.getJSONObject(i)
            if (block.optString("type") == "text") sb.append(block.optString("text"))
        }
        return sb.toString().trim()
    }

    private fun openai(s: AppSettings, system: String, turns: List<Pair<String, String>>): String {
        val msgs = JSONArray()
        msgs.put(JSONObject().put("role", "system").put("content", system))
        turns.forEach { (r, t) -> msgs.put(JSONObject().put("role", r).put("content", t)) }
        val body = JSONObject()
            .put("model", s.effectiveModel)
            .put("messages", msgs)
            .toString()
        val resp = JSONObject(
            post(
                "https://api.openai.com/v1/chat/completions",
                mapOf(
                    "Authorization" to "Bearer ${s.apiKey}",
                    "Content-Type" to "application/json"
                ),
                body
            )
        )
        return resp.getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").optString("content").trim()
    }

    private fun post(url: String, headers: Map<String, String>, body: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20000
            conn.readTimeout = 60000
            conn.doOutput = true
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IOException("HTTP $code ${text.take(300)}")
            return text
        } finally {
            conn.disconnect()
        }
    }
}
