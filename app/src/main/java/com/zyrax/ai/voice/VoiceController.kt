package com.zyrax.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.zyrax.ai.data.AppLanguage
import com.zyrax.ai.data.Persona
import java.util.Locale

/** Microphone (speech-to-text) and voice replies (text-to-speech). */
class VoiceController(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null

    init {
        tts = TextToSpeech(appContext) { status -> ttsReady = status == TextToSpeech.SUCCESS }
    }

    /** Returns false if the phone has no voice for this language. */
    fun speak(text: String, lang: AppLanguage, persona: Persona): Boolean {
        val engine = tts ?: return false
        if (!ttsReady) return false
        val r = engine.setLanguage(Locale.forLanguageTag(lang.tag))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return false
        engine.setPitch(if (persona == Persona.ZYRAX) 1.2f else 0.8f)
        engine.setSpeechRate(1.0f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zyrax_reply")
        return true
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun startListening(
        lang: AppLanguage,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onFail: (Int) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onFail(-1)
            return
        }
        stopListening()
        val r = SpeechRecognizer.createSpeechRecognizer(appContext)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                onFail(error)
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text.isNullOrBlank()) onFail(SpeechRecognizer.ERROR_NO_MATCH) else onFinal(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.let(onPartial)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.tag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        r.startListening(intent)
    }

    fun stopListening() {
        recognizer?.let {
            runCatching { it.cancel() }
            runCatching { it.destroy() }
        }
        recognizer = null
    }

    fun shutdown() {
        stopListening()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
