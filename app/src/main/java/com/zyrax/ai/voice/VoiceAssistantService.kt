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
import androidx.core.app.NotificationCompat
import com.zyrax.ai.MainActivity
import com.zyrax.ai.R
import java.util.Locale

class VoiceAssistantService : Service(), RecognitionListener {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopping = false

    companion object {
        const val CHANNEL_ID = "zyrax_voice_channel"
        const val NOTIFICATION_ID = 4101
        const val ACTION_STOP = "com.zyrax.ai.STOP_VOICE"
    }

    override fun onCreate() {
        super.onCreate()

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("bn", "BD")
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(this)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("ZYRAX AI is listening")
            .setContentText("বলুন: ZYRAX অথবা JARVIS")
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    private fun startListening() {
        if (stopping || recognizer == null) return

        handler.postDelayed({
            if (stopping) return@postDelayed

            try {
                val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                recognizer?.startListening(speechIntent)
            } catch (_: Exception) {
                scheduleRestart()
            }
        }, 350)
    }

    private fun scheduleRestart() {
        if (!stopping) {
            handler.postDelayed({ startListening() }, 900)
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        scheduleRestart()
    }

    override fun onResults(results: Bundle?) {
        val spoken = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

        if (spoken.isNotBlank()) processVoiceCommand(spoken)
        scheduleRestart()
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun processVoiceCommand(spoken: String) {
        val command = spoken.lowercase(Locale.ROOT)

        when {
            command.contains("jarvis") ||
            command.contains("জারভিস") ||
            command.contains("জার্ভিস") -> {
                getSharedPreferences("zyrax_voice", MODE_PRIVATE)
                    .edit().putString("active_agent", "JARVIS").apply()
                speak("জি, আমি JARVIS। বলুন, কী করতে হবে?")
            }

            command.contains("zyrax") ||
            command.contains("জাইরাক্স") ||
            command.contains("জিরাক্স") -> {
                getSharedPreferences("zyrax_voice", MODE_PRIVATE)
                    .edit().putString("active_agent", "ZYRAX").apply()
                speak("জি, আমি ZYRAX। বলুন, আমি শুনছি।")
            }

            else -> {
                val agent = getSharedPreferences("zyrax_voice", MODE_PRIVATE)
                    .getString("active_agent", "ZYRAX") ?: "ZYRAX"

                speak("$agent আপনার কথা শুনেছে। পূর্ণ AI উত্তর দেওয়ার জন্য AI সংযোগ প্রয়োজন।")
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zyrax_reply")
    }

    override fun onDestroy() {
        stopping = true
        handler.removeCallbacksAndMessages(null)

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
