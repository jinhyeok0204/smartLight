package com.example.lightapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import com.example.lightapp.databinding.ActivityLightBinding

import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage


// 조명 조절 Activity
class LightActivity : AppCompatActivity() {

    private val binding by lazy {ActivityLightBinding.inflate(layoutInflater)}
    private lateinit var mqttClient: MqttAndroidClient

    private val lightCount = 3 // 조명의 개수
    private val lightStatusViews = mutableListOf<TextView>()
    private val toggleButtons = mutableListOf<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val serverURI = BuildConfig.MQTT_SERVER_URI
        val clientID = "AndroidClient"

        mqttClient = MqttAndroidClient(this.applicationContext, serverURI, clientID)

        mqttClient.setCallback(object: MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Toast.makeText(this@LightActivity, "connection Lost", Toast.LENGTH_SHORT).show()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                for (i in 1..lightCount) {
                    if (topic == "home/light$i/status") {
                        lightStatusViews[i-1].text = "Light $i Status: ${message.toString()}"
                    }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
               Log.d("onCreate", "Delivery Complete")
            }
        })

        connectToMqttBroker()

        setupLights()

        binding.feedbackButton.setOnClickListener{
            val intent = Intent(this, FeedbackActivity::class.java)
            startActivity(intent)
        }
    }


    private fun connectToMqttBroker(){
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = true

        mqttClient.connect(options, null, object: IMqttActionListener{
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "Connected to MQTT broker")
                for(i in 1..lightCount){
                    mqttClient.subscribe("home/light$i/status", 1)
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
        mqttClient.publish("/home/light$lightNumber/status", MqttMessage("".toByteArray()))
    }

    // 조명의 개수에 따라 동적으로 초기화
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

            lightContainer.addView(textView)
            lightContainer.addView(button)
            lightStatusViews.add(textView)
            toggleButtons.add(button)
        }
    }

    // 조명 on / off
    private fun toggleLight(lightNumber: Int){
        val currentStatus = lightStatusViews[lightNumber - 1].text.toString().split(":")[1]
        val newStatus = if (currentStatus == "ON") "OFF" else "ON"
        mqttClient.publish("home/light$lightNumber/toggle", MqttMessage(newStatus.toByteArray()))
    }
}
