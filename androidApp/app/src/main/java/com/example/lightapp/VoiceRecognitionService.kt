package com.example.lightapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat

class VoiceRecognitionService : Service() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private var mqttService: MqttService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MqttService.LocalBinder
            mqttService = binder?.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mqttService = null
            isBound = false
        }
    }

    private val lightCount = 3

    override fun onCreate(){
        super.onCreate()

        // MQTT 서비스에 바인딩
        bindService(Intent(this, MqttService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)

        // Foreground Service -> 백그라운드에서 동작
        startForegroundService()

        // 음성인식기능 초기화 및 리스너 설정
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply{
            setRecognitionListener(object:RecognitionListener{
                override fun onReadyForSpeech(params: Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    startListening() // 음성 인식이 끝나면 다시 시작
                }

                override fun onError(error: Int) {
                    startListening() // 오류 발생하면 다시 시작
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                    matches?.let{
                        val recognizedText = it[0]
                        handleCommand(recognizedText)
                    }
                    startListening() // 결과를 처리한 후 다시 시작
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        startListening()
    }

    private fun startListening(){
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR") // 한국어 인식
        }
        speechRecognizer.startListening(intent)
    }

    private fun handleCommand(command: String){
        if(command.contains("조명아")){
            // 전체 조명 제어
            if(command.contains("전체")){
                when{
                    command.contains("켜줘") -> toggleAllLights("ON")
                    command.contains("꺼줘") -> toggleAllLights("OFF")
                }
            } else {
                // 개별 조명 제어
                val lightNumber = when {
                    command.contains("1번") -> 1
                    command.contains("2번") -> 2
                    command.contains("3번") -> 3
                    else -> null
                }

                lightNumber?.let{
                    when{
                        command.contains("켜줘") -> mqttService?.publishLightToggle(it, "ON")
                        command.contains("꺼줘") -> mqttService?.publishLightToggle(it, "OFF")
                        else -> Log.d("VoiceRecognition", "Unknown command for light $it : $command")
                    }
                } ?: Log.d("VoiceRecognition", "No valid light number found in command: $command")
            }
        }
    }

    // 전체 조명 제어 함수
    private fun toggleAllLights(status: String){
        for(i in 1..lightCount){
            mqttService?.publishLightToggle(i, status)
        }
    }

    private fun startForegroundService() {
        val channelId = "VoiceRecognitionServiceChannel"
        val channelName = "Voice Recognition Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Service for voice recognition"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Voice Recognition Service")
            .setContentText("Listening for voice commands...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(isBound){
            unbindService(serviceConnection)
        }
        speechRecognizer.destroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}