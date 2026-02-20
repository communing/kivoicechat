package com.example.kivoicechat

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat

class VoiceService : Service() {

    private val CHANNEL_ID = "VoiceServiceChannel"
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Erstelle die permanente Benachrichtigung
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KI Voice Chat")
            .setContentText("Ich höre dir im Hintergrund zu...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Standard Android-Icon
            .build()

        // 2. Starte den Dienst offiziell im Vordergrund
        startForeground(1, notification)

        // 3. Beginne mit dem Zuhören
        startListening()

        return START_STICKY
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            
            override fun onError(error: Int) {
                // Wenn nichts gesagt wurde oder ein Fehler auftritt, fangen wir einfach wieder von vorne an
                startListening()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    Log.d("VoiceService", "Erkannt: $recognizedText")
                    
                    // Wir senden den erkannten Text an die MainActivity
                    val broadcastIntent = Intent("com.example.kivoicechat.SPEECH_RECOGNIZED")
                    broadcastIntent.putExtra("text", recognizedText)
                    sendBroadcast(broadcastIntent)
                }
                // Nach einem Satz direkt wieder zuhören
                startListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE") // Auf Deutsch gestellt
        }
        speechRecognizer?.startListening(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID, "Hintergrund-Zuhören",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(serviceChannel)
    }
}
