package com.zyrax.ai.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.zyrax.ai.MainActivity
import com.zyrax.ai.ai.GeminiClient
import com.zyrax.ai.agents.AgentStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceAssistantService : Service(), RecognitionListener {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var stopping = false
    private var busy = false
    private var ttsReady = false

    companion object {
        const val CHANNEL_ID = "zyrax_voice_channel"
        const val NOTIFICATION_ID = 4101
        const val ACTION_STOP = "com.zyrax.ai.STOP_VOICE"
    }

    override fun onCreate() {
        super.onCreate()

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("bn", "BD"))
                ttsReady = result != TextToSpeech.ERROR &&
                    result != TextToSpeech.LANG_MISSING_DATA

                tts?.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}

                        override fun onDone(utteranceId: String?) {
                            handler.post {
                                busy = false
                                if (!stopping) {
                                    scheduleRestart()
                                }
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            handler.post {
                                busy = false
                                scheduleRestart()
                            }
                        }
                    }
                )
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(this)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        stopping = false
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        getSharedPreferences("zyrax_voice", MODE_PRIVATE)
            .edit().putBoolean("running", true).apply()

        startListening()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ZYRAX Live Voice",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "ZYRAX voice assistant is active"
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, VoiceAssistantService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("ZYRAX AI Voice")
            .setContentText("ভয়েস অ্যাসিস্ট্যান্ট চালু আছে")
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Voice OFF",
                stopIntent
            )
            .build()
    }

    private fun startListening() {
        if (stopping || busy || recognizer == null) return

        handler.postDelayed({
            if (stopping || busy) return@postDelayed

            try {
                val speechIntent =
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE,
                            "bn-BD"
                        )
                        putExtra(
                            RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                            true
                        )
                        putExtra(
                            RecognizerIntent.EXTRA_MAX_RESULTS,
                            3
                        )
                    }

                recognizer?.startListening(speechIntent)
            } catch (_: Exception) {
                scheduleRestart()
            }
        }, 500)
    }

    private fun scheduleRestart() {
        if (!stopping && !busy) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({
                startListening()
            }, 1000)
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        if (!busy) scheduleRestart()
    }

    override fun onResults(results: Bundle?) {
        val spoken = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

        if (spoken.isBlank()) {
            scheduleRestart()
        } else {
            processVoiceCommand(spoken)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun processVoiceCommand(spoken: String) {
        if (busy || stopping) return

        val command = spoken.lowercase(Locale.ROOT)

        val isJarvis = listOf(
            "jarvis", "জারভিস", "জার্ভিস"
        ).any { command.contains(it) }

        val isZyrax = listOf(
            "zyrax", "জাইরাক্স", "জিরাক্স", "জাইরেক্স"
        ).any { command.contains(it) }

        if (isJarvis) {
            AgentStore.setActive(this, "jarvis")
        } else if (isZyrax) {
            AgentStore.setActive(this, "zyrax")
        }

        val activeAgent = AgentStore.getActive(this)
        val question = command
            .replace(
                Regex("(?i)\\b(jarvis|zyrax)\\b"),
                ""
            )
            .replace("জারভিস", "")
            .replace("জার্ভিস", "")
            .replace("জাইরাক্স", "")
            .replace("জিরাক্স", "")
            .replace("জাইরেক্স", "")
            .replace("তুমি আসো", "")
            .replace("তুমি এসো", "")
            .replace("come here", "")
            .replace("are you there", "")
            .trim()
            .trim(',', '.', '!', '?', '।')

        if (question.isBlank()) {
            busy = true
            speak("জি, আমি ${activeAgent.name}। বলুন, কী করতে হবে?")
            return
        }

        busy = true

        serviceScope.launch {
            try {
                val answer = GeminiClient.generateReply(
                    this@VoiceAssistantService,
                    question,
                    agent = activeAgent
                )

                speak(answer)
            } catch (e: Exception) {
                val message = e.message
                    ?: "Gemini সংযোগে সমস্যা হয়েছে।"

                speak(
                    if (message.contains("API Key", ignoreCase = true)) {
                        "Gemini API Key সেটিংসে যোগ করুন।"
                    } else {
                        "দুঃখিত, উত্তর আনতে সমস্যা হয়েছে। $message"
                    }
                )
            }
        }
    }

    private fun speak(text: String) {
        if (stopping) return

        if (tts == null || !ttsReady) {
            busy = false
            scheduleRestart()
            return
        }

        val result = tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "zyrax_reply_${System.currentTimeMillis()}"
        )

        if (result == TextToSpeech.ERROR) {
            busy = false
            scheduleRestart()
        }
    }

    override fun onDestroy() {
        stopping = true
        busy = false
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()

        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {}

        tts?.stop()
        tts?.shutdown()

        getSharedPreferences("zyrax_voice", MODE_PRIVATE)
            .edit().putBoolean("running", false).apply()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
