package com.example.lightapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.lightapp.databinding.ActivityLightBinding
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

class LightActivity : AppCompatActivity() {

    private val binding by lazy {ActivityLightBinding.inflate(layoutInflater)}
    private lateinit var mqttClient: MqttAndroidClient

    private val lightCount = 3 // 조명의 개수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val serverURI = BuildConfig.MQTT_SERVER_URI

    }


    private fun connectToMqttBroker(){
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = true

        mqttClient.connect(options, null, object: IMqttActionListener{
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "Connected to MQTT broker")
                for(i in 1..lightCount){
                    mqttClient.subscribe("home/lightStatus$i", 1)
                    getLightStatus(i)
                }
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d("MQTT", "Failed to connect to MQTT broker")
            }
        })
    }

    // 조명의 상태를 가져옴
    private fun getLightStatus(lightNumber: Int){

    }

    private fun setupLights(){
        val lightContainer = binding.lightContainer
        for(i in 1..lightCount){
            val textView = TextView(this).apply{
                id = View.generateViewId()
                text = "Light $i Stauts: Unknown"
                textSize = 18f
            }

            val button = Button(this).apply{
                id = View.generateViewId()
                text = "Toggle Light $i"
                setOnClickListener{
                    toggleLight(i)
                }
            }
        }
    }

    // 조명 on / off
    private fun toggleLight(lightNumber: Int){
        val currentStatus = lightStatusViews[lightNumber - 1].text.toString().split(":")[1]
        val newStatus = if (currentStatus == "ON") "OFF" else "ON"
        mqttClient.publish("home/light$lightNumber" MqttMessage(newStatus.toByteArray()))
    }
}
