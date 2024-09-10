package com.example.lightapp

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.example.lightapp.databinding.ActivityLightBinding
import yuku.ambilwarna.AmbilWarnaDialog

// 조명 조절 Activity
class LightActivity : AppCompatActivity() {

    private val binding by lazy { ActivityLightBinding.inflate(layoutInflater) }

    private val lightCount = 3 // 조명의 개수
    private val lightStatusViews = mutableListOf<TextView>()
    private val lightImageViews = mutableListOf<ImageView>()
    private var selectedColors = mutableListOf(Color.WHITE, Color.WHITE, Color.WHITE)
    private var selectedBrightness = mutableListOf(100, 100, 100) // 추가된 밝기 저장
    private var mqttService: MqttService? = null
    private var isBound = false
    private var musicModeOn = false

    private val connection = object : ServiceConnection {
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
    private val statusReceiver = object : BroadcastReceiver() {
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

        binding.musicModeButton.setOnClickListener {
            toggleMusicMode()
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MqttService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    override fun onDestroy() {
        unregisterReceiver(statusReceiver)
        super.onDestroy()
    }

    private fun setupLights() {
        val lightContainer = binding.lightContainer

        for (i in 1..lightCount) {
            val lightLayout = createLightLayout(i)
            lightContainer.addView(lightLayout)
        }
    }

    private fun createLightLayout(lightNumber: Int): ConstraintLayout {
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
            text = "Light $lightNumber Status: OFF"
            textSize = 30f
            textAlignment = View.TEXT_ALIGNMENT_CENTER // 중앙 정렬
        }

        val imageView = ImageView(this).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.light_off)
            layoutParams = ConstraintLayout.LayoutParams(400, 400)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val colorSpinner = Spinner(this).apply {
            id = View.generateViewId()
            adapter = ArrayAdapter.createFromResource(
                this@LightActivity,
                R.array.color_array,
                android.R.layout.simple_spinner_item
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        val brightnessSpinner = Spinner(this).apply {
            id = View.generateViewId()
            adapter = ArrayAdapter.createFromResource(
                this@LightActivity,
                R.array.brightness_array,
                android.R.layout.simple_spinner_item
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        val comboBoxLayout = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER // 중앙 정렬
            addView(colorSpinner)
            addView(brightnessSpinner)
        }

        val editButton = Button(this).apply {
            id = View.generateViewId()
            text = "Edit Color $lightNumber"
            setOnClickListener { openColorPicker(lightNumber) }
        }

        // ON 버튼을 먼저 왼쪽에 배치
        val onButton = Button(this).apply {
            id = View.generateViewId()
            text = "ON"
            setOnClickListener { toggleLight(lightNumber, "ON") }
        }

        // OFF 버튼을 오른쪽에 배치
        val offButton = Button(this).apply {
            id = View.generateViewId()
            text = "OFF"
            setOnClickListener { toggleLight(lightNumber, "OFF") }
        }

        val onOffLayout = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER // 중앙 정렬
            addView(onButton)  // ON 버튼 먼저
            addView(offButton) // OFF 버튼 나중에
        }

        lightLayout.addView(textView)
        lightLayout.addView(imageView)
        lightLayout.addView(comboBoxLayout)
        lightLayout.addView(editButton)
        lightLayout.addView(onOffLayout)

        val set = ConstraintSet()
        set.clone(lightLayout)

        // 텍스트뷰 설정
        set.connect(textView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
        set.connect(textView.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 16)
        set.connect(textView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16)
        set.setHorizontalBias(textView.id, 0.5f)

        // 이미지뷰 설정
        set.connect(imageView.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
        set.connect(imageView.id, ConstraintSet.TOP, textView.id, ConstraintSet.BOTTOM, 16)
        set.connect(imageView.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16)
        set.setHorizontalBias(imageView.id, 0.5f)

        // ComboBox 레이아웃 설정 (가로 배치)
        set.connect(comboBoxLayout.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
        set.connect(comboBoxLayout.id, ConstraintSet.TOP, imageView.id, ConstraintSet.BOTTOM, 16)
        set.connect(comboBoxLayout.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16)
        set.setHorizontalBias(comboBoxLayout.id, 0.5f)

        // Edit 버튼 설정 (위쪽)
        set.connect(editButton.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
        set.connect(editButton.id, ConstraintSet.TOP, comboBoxLayout.id, ConstraintSet.BOTTOM, 16)
        set.connect(editButton.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16)
        set.setHorizontalBias(editButton.id, 0.5f)

        // ON/OFF 버튼 설정 (가로 배치)
        set.connect(onOffLayout.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
        set.connect(onOffLayout.id, ConstraintSet.TOP, editButton.id, ConstraintSet.BOTTOM, 16)
        set.connect(onOffLayout.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16)
        set.connect(onOffLayout.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 16)
        set.setHorizontalBias(onOffLayout.id, 0.5f)

        set.applyTo(lightLayout)

        return lightLayout
    }


    // Helper function for mapping color names to RGB values
    private fun getColorFromName(colorName: String): Int {
        return when (colorName) {
            "RED" -> Color.RED
            "ORANGE" -> Color.rgb(255, 165, 0)
            "YELLOW" -> Color.YELLOW
            "GREEN" -> Color.GREEN
            "BLUE" -> Color.BLUE
            "PURPLE" -> Color.rgb(128, 0, 128)
            else -> Color.WHITE
        }
    }

    private fun toggleLight(lightNumber: Int, status: String) {
        if (isBound) {
            val color = selectedColors[lightNumber - 1]
            val brightness = selectedBrightness[lightNumber - 1]
            val r = (Color.red(color) * (brightness / 100.0)).toInt()
            val g = (Color.green(color) * (brightness / 100.0)).toInt()
            val b = (Color.blue(color) * (brightness / 100.0)).toInt()

            mqttService?.publishLightToggle(lightNumber, "$status/$r/$g/$b")
        }
    }


    // 색상 선택을 위한 팝업 열기
    private fun openColorPicker(lightNumber: Int) {
        val dialog = AmbilWarnaDialog(this, selectedColors[lightNumber - 1], object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                selectedColors[lightNumber - 1] = color
            }

            override fun onCancel(dialog: AmbilWarnaDialog?) {
                // 취소 시 아무 동작 없음
            }
        })
        dialog.show()
    }

    private fun updateLightStatus(lightNumber: Int, status: String) {
        Log.d("LIGHT", "$lightNumber , $status")
        lightStatusViews[lightNumber - 1].text = "Light $lightNumber Status: $status"
        updateLightImage(lightNumber - 1, status)
    }

    private fun updateLightImage(index: Int, status: String) {
        Log.d("LIGHTIMG", "lgiht status: $status")
        val imageRes = if (status == "ON") R.drawable.light_on else R.drawable.light_off
        lightImageViews[index].setImageResource(imageRes)
    }

    // Music Mode 토글
    private fun toggleMusicMode() {
        musicModeOn = !musicModeOn
        val status = if (musicModeOn) true else false
        mqttService?.publishMusicMode(status)
    }
}
