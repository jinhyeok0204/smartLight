package com.example.lightapp

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
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

    private val lightCount = 3 // 조명의 개수

    private val lightStatusViews = mutableListOf<TextView>()
    private val lightImageViews = mutableListOf<ImageView>()

    private var mqttService: MqttService? = null
    private var isBound = false

    private val connection = object : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MqttService.LocalBinder
            mqttService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mqttService = null
            isBound = false
        }
    }

    // 상태 업데이트를 수신하는 브로드캐스트 리시버
    private val statusReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val lightNumber = intent?.getIntExtra("LIGHT_NUMBER", -1) ?: return
            val status = intent.getStringExtra("STATUS") ?: return
            updateLightStatus(lightNumber, status)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        registerReceiver(statusReceiver, IntentFilter("LIGHT_STATUS_UPDATE"), RECEIVER_NOT_EXPORTED)

        setupLights()
    }

    override fun onStart(){
        super.onStart()
        // MQTT 서비스에 바인드
        Intent(this, MqttService::class.java).also {intent ->
            bindService(intent, connection ,Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop(){
        super.onStop()
        if(isBound){
            unbindService(connection)
            isBound = false
        }
    }
    override fun onDestroy(){
        unregisterReceiver(statusReceiver)
        super.onDestroy()
    }


    // 조명의 개수에 따라 동적으로 초기화
    private fun setupLights() {
        val lightContainer = binding.lightContainer

        for (i in 1..lightCount){
            val lightLayout = createLightLayout(i)
            lightContainer.addView(lightLayout)
        }
    }
    private fun createLightLayout(lightNumber: Int):ConstraintLayout{
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
            text = "Light $lightNumber Status: Unknown"
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
            text = "Toggle Light $lightNumber"
            setOnClickListener { toggleLight(lightNumber) }
        }

        lightLayout.addView(textView)
        lightLayout.addView(imageView)
        lightLayout.addView(button)
        lightStatusViews.add(textView)
        lightImageViews.add(imageView)

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

        return lightLayout
    }

    private fun toggleLight(lightNumber: Int){
        if(isBound){
            val currentStatus = lightStatusViews[lightNumber - 1].text.toString().split(":")[1].trim()
            val newStatus = if (currentStatus == "ON") "OFF" else "ON"
            mqttService?.publishLightToggle(lightNumber, newStatus)
        }
    }

    private fun updateLightStatus(lightNumber: Int, status:String){
        runOnUiThread{
            lightStatusViews[lightNumber - 1].text = "Light $lightNumber Status: $status"
            updateLightImage(lightNumber - 1, status)
        }
    }

    private fun updateLightImage(index: Int, status: String){
        val imageRes = if (status == "ON") R.drawable.light_on else R.drawable.light_off
        lightImageViews[index].setImageResource(imageRes)
    }

}