package com.example.lightapp

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Looper
import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import java.nio.charset.StandardCharsets
import android.os.IBinder
import android.widget.Toast

class MqttService : Service() {

    // Binder을 위한 LocalBinder 클래스
    inner class LocalBinder : Binder(){
        fun getService() : MqttService = this@MqttService
    }


    // LocalBinder의 인스턴스 생성
    private val binder = LocalBinder()

    private lateinit var mqttClient: Mqtt3AsyncClient
    private val serverURI = BuildConfig.MQTT_SERVER_URI

    private val lightCount = 3 // 조명의 개수

    override fun onCreate(){
        super.onCreate()
        initializeMqttClient()
        connectToMqttBroker()
    }

    private fun initializeMqttClient(){
        val clientID = "AndroidClient_${System.currentTimeMillis()}"
        mqttClient = MqttClient.builder()
            .useMqttVersion3()
            .serverHost(serverURI)
            .serverPort(1883)
            .identifier(clientID)
            .buildAsync()
    }

    private fun connectToMqttBroker(){
        mqttClient.connectWith()
            .cleanSession(true)
            .keepAlive(60)
            .send()
            .whenComplete{ack, throwable ->
                if(throwable != null){
                    Log.e("MQTT", "Failed to connect to MQTT broker", throwable)
                    retryConnection()
                }
                else{
                    Log.d("MQTT", "Connected to MQTT broker")
                    subscribeToLightStatus()
                    //getLightStatus()
                }
            }
    }

    private fun retryConnection(){
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            connectToMqttBroker()
        }, 5000)
    }

    private fun subscribeToLightStatus(){
        for (i in 1..lightCount){
            mqttClient.subscribeWith()
                .topicFilter("app/light$i/status")
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback{publish ->
                    Log.e("subscribe", "${publish}")
                    handleIncomingMessage(i, publish.payloadAsBytes)
                }
                .send()
        }
    }

    private fun handleIncomingMessage(lightNumber: Int, payload: ByteArray){
        val message = String(payload, StandardCharsets.UTF_8)
        Log.d("MQTT_RECEIVE", "Received from MQTT: Light $lightNumber, Status $message")
        val intent = Intent("LIGHT_STATUS_UPDATE").apply {
            putExtra("LIGHT_NUMBER", lightNumber)
            putExtra("STATUS", message)
        }
        sendBroadcast(intent)

    }

    fun getLightStatus(){
        for(i in 1.. lightCount){
            if(mqttClient.state.isConnected){
                mqttClient.publishWith().topic("home/light$i/status").payload(ByteArray(0)).send()
            }
            else{
                Toast.makeText(this, "Not Connected to MQTT broker", Toast.LENGTH_SHORT).show()
            }
        }

    }

    fun publishLightToggle(lightNumber: Int, status: String){
        if(mqttClient.state.isConnected){
            mqttClient.publishWith()
                .topic("home/light$lightNumber/toggle")
                .payload(status.toByteArray(StandardCharsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()
            Log.d("MqttService", "Light $lightNumber toggle to $status")
        } else {
            Toast.makeText(this, "Not connected to MQTT broker", Toast.LENGTH_SHORT).show()
        }
    }

    fun publishMusicMode(turnOn: Boolean){
        val message = if (turnOn) "ON" else "OFF"
        if(mqttClient.state.isConnected){
            mqttClient.publishWith()
                .topic("home/musicmode")
                .payload(message.toByteArray(StandardCharsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()
            Log.d("MqttService", "Music mode set to $message")
        } else {
            Toast.makeText(this, "Not connected to MQTT broker", Toast.LENGTH_SHORT).show()
        }
    }

    fun publishActionMode(turnOn: Boolean){
        val message = if (turnOn) "ON" else "OFF"
        if(mqttClient.state.isConnected){
            mqttClient.publishWith()
                .topic("home/actionmode")
                .payload(message.toByteArray(StandardCharsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()
            Log.d("MqttService", "Action mode set to $message")
        } else {
            Toast.makeText(this, "Not connected to MQTT broker", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBind(intent:Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        mqttClient.disconnect()
        super.onDestroy()
    }
}