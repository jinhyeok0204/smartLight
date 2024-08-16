package com.example.lightapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.example.lightapp.databinding.ActivityLightBinding
import com.hivemq.client.mqtt.MqttClient

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import java.nio.charset.StandardCharsets

// 조명 조절 Activity
class LightActivity : AppCompatActivity() {

    private val binding by lazy { ActivityLightBinding.inflate(layoutInflater) }
    private lateinit var mqttClient: Mqtt3AsyncClient
    private val serverURI = BuildConfig.MQTT_SERVER_URI

    private val lightCount = 3 // 조명의 개수
    private val lightStatusViews = mutableListOf<TextView>()
    private val toggleButtons = mutableListOf<Button>()
    private val lightImageViews = mutableListOf<ImageView>()

    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        userId = intent.getStringExtra("USER_ID").orEmpty()
        val clientID = "AndroidClient_$userId"

        mqttClient = MqttClient.builder()
            .useMqttVersion3()
            .serverHost(serverURI)
            .serverPort(1883)
            .identifier(clientID)
            .buildAsync()

        connectToMqttBroker()
        setupLights()

        binding.feedbackButton.setOnClickListener {
            val intent = Intent(this, FeedbackActivity::class.java)
            startActivity(intent)
        }
    }

    private fun connectToMqttBroker() {
        mqttClient.connectWith()
            .cleanSession(true)
            .keepAlive(60)
            .send()
            .whenComplete { ack, throwable ->
                if (throwable != null) {
                    Log.e("MQTT", "Failed to connect to MQTT broker", throwable)
                    retryConnection()
                } else {
                    Log.d("MQTT", "Connected to MQTT broker")
                    for (i in 1..lightCount) {
                        mqttClient.subscribeWith()
                            .topicFilter("home/light$i/status")
                            .qos(MqttQos.AT_LEAST_ONCE)
                            .callback { publish ->
                                val message = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                                runOnUiThread {
                                    lightStatusViews[i - 1].text = "Light $i Status: $message"
                                    updateLightImage(i - 1, message)
                                }
                            }
                            .send()
                        getLightStatus(i)
                    }
                }
            }
    }

    private fun retryConnection() {
        Handler(Looper.getMainLooper()).postDelayed({
            connectToMqttBroker()
        }, 5000)
    }

    // 조명의 상태를 가져옴
    private fun getLightStatus(lightNumber: Int) {
        if (mqttClient.state.isConnected) {
            mqttClient.publishWith()
                .topic("home/light$lightNumber/status")
                .payload(ByteArray(0))
                .send()
        } else {
            Toast.makeText(this, "Not connected to MQTT broker", Toast.LENGTH_SHORT).show()
        }
    }

    // 조명의 개수에 따라 동적으로 초기화
    private fun setupLights() {
        val lightContainer = binding.lightContainer


        for (i in 1..lightCount) {
            val lightLayout = ConstraintLayout(this).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 100, 0, 16)
                }
            }

            val textView = TextView(this).apply {
                id = View.generateViewId()
                text = "Light $i Status: Unknown"
                textSize = 30f
            }

            val imageView = ImageView(this).apply {
                id = View.generateViewId()
                setImageResource(R.drawable.light_off)
                layoutParams = ConstraintLayout.LayoutParams(400, 400)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }

            val button = Button(this).apply {
                id = View.generateViewId()
                text = "Toggle Light $i"
                setOnClickListener {
                    toggleLight(i)
                }
            }

            lightLayout.addView(textView)
            lightLayout.addView(imageView)
            lightLayout.addView(button)
            lightContainer.addView(lightLayout)
            lightStatusViews.add(textView)
            lightImageViews.add(imageView)
            toggleButtons.add(button)

            val set = ConstraintSet()
            set.clone(lightLayout)

            set.connect(textView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
            set.connect(textView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 16)
            set.connect(textView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16)
            set.setHorizontalBias(textView.id, 0.5f)

            set.connect(imageView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
            set.connect(imageView.id, ConstraintSet.TOP, textView.id, ConstraintSet.BOTTOM, 16)
            set.connect(imageView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16)
            set.setHorizontalBias(imageView.id, 0.5f)

            set.connect(button.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
            set.connect(button.id, ConstraintSet.TOP, imageView.id, ConstraintSet.BOTTOM, 16)
            set.connect(button.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16)
            set.connect(button.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 16)
            set.setHorizontalBias(button.id, 0.5f)

            set.applyTo(lightLayout)
        }
    }

    // 조명 on / off
    private fun toggleLight(lightNumber: Int) {
        if (mqttClient.state.isConnected) {
            // 현재 상태 가져오기
            val currentStatus = lightStatusViews[lightNumber - 1].text.toString().split(":")[1].trim()
            val newStatus = if (currentStatus == "ON") "OFF" else "ON"

            // 상태 변경 메시지 전송
            mqttClient.publishWith()
                .topic("home/light$lightNumber/toggle")
                .payload(newStatus.toByteArray(StandardCharsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()

            // UI 업데이트: 토글 버튼을 누를 때 즉시 상태를 업데이트
            runOnUiThread {
                lightStatusViews[lightNumber - 1].text = "Light $lightNumber Status: $newStatus"
                updateLightImage(lightNumber - 1, newStatus)
            }
        } else {
            Toast.makeText(this, "Not connected to MQTT broker", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLightImage(index: Int, status: String) {
        val imageRes = if (status == "ON") R.drawable.light_on else R.drawable.light_off
        lightImageViews[index].setImageResource(imageRes)
    }
}