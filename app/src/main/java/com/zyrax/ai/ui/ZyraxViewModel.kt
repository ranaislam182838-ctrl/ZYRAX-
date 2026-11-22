package com.zyrax.ai.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zyrax.ai.data.AiClient
import com.zyrax.ai.data.AppSettings
import com.zyrax.ai.data.ChatMessage
import com.zyrax.ai.data.Demo
import com.zyrax.ai.data.MemoryItem
import com.zyrax.ai.data.Persona
import com.zyrax.ai.data.Store
import com.zyrax.ai.data.systemPrompt
import com.zyrax.ai.voice.VoiceController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Screen { HOME, CHAT, MEMORY, SETTINGS }

class ZyraxViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    private val voice = VoiceController(app)

    var settings by mutableStateOf(store.loadSettings())
        private set
    var screen by mutableStateOf(Screen.HOME)
        private set
    var persona by mutableStateOf(Persona.ZYRAX)
        private set
    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set
    var memory by mutableStateOf(store.loadMemory())
        private set
    var busy by mutableStateOf(false)
        private set
    var listening by mutableStateOf(false)
        private set
    var partial by mutableStateOf("")
        private set
    var errorText by mutableStateOf<String?>(null)
        private set

    private val strings: Strings get() = Strings.of(settings.language)

    // ---- navigation ----
    fun openChat(p: Persona) {
        persona = p
        messages = store.loadChat(p)
        errorText = null
        screen = Screen.CHAT
    }

    fun openMemory() { screen = Screen.MEMORY }

    fun openSettings() { screen = Screen.SETTINGS }

    fun goBack() {
        voice.stopSpeaking()
        voice.stopListening()
        listening = false
        partial = ""
        screen = Screen.HOME
    }

    fun showError(msg: String) { errorText = msg }

    // ---- chat ----
    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || busy) return
        errorText = null
        voice.stopSpeaking()
        val p = persona
        val st = settings
        val mem = memory
        val userMsg = ChatMessage(System.nanoTime(), "user", text)
        messages = messages + userMsg
        store.saveChat(p, messages)
        val history = messages
        busy = true
        viewModelScope.launch {
            try {
                val reply = if (st.demo) {
                    delay(700)
                    Demo.reply(p, st.language, text)
                } else {
                    AiClient.reply(st, p.systemPrompt(st.language, mem), history)
                }
                val aMsg = ChatMessage(System.nanoTime(), "assistant", reply.ifBlank { "…" })
                val updated = (if (persona == p) messages else store.loadChat(p)) + aMsg
                store.saveChat(p, updated)
                if (persona == p) messages = updated
                if (st.speakReplies && screen == Screen.CHAT && persona == p) {
                    if (!voice.speak(aMsg.text, st.language, p)) errorText = strings.ttsMissing
                }
            } catch (e: Exception) {
                errorText = "${strings.error}: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                busy = false
            }
        }
    }

    fun clearChat() {
        voice.stopSpeaking()
        messages = emptyList()
        store.saveChat(persona, messages)
    }

    // ---- voice ----
    fun startListening() {
        voice.stopSpeaking()
        listening = true
        partial = ""
        errorText = null
        voice.startListening(
            settings.language,
            onPartial = { partial = it },
            onFinal = {
                listening = false
                partial = ""
                send(it)
            },
            onFail = { code ->
                listening = false
                partial = ""
                errorText = if (code == -1) strings.voiceUnavailable else strings.voiceError
            }
        )
    }

    fun stopListening() {
        voice.stopListening()
        listening = false
        partial = ""
    }

    // ---- memory ----
    fun addMemory(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        memory = memory + MemoryItem(System.nanoTime(), t)
        store.saveMemory(memory)
    }

    fun deleteMemory(id: Long) {
        memory = memory.filterNot { it.id == id }
        store.saveMemory(memory)
    }

    // ---- settings ----
    fun saveSettings(s: AppSettings) {
        settings = s
        store.saveSettings(s)
    }

    override fun onCleared() {
        voice.shutdown()
        super.onCleared()
    }
}
