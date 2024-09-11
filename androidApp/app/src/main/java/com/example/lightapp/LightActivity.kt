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
import android.widget.CheckBox
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
    private var music_status = false // Music Mode 상태
    private var action_status = false // Behavior Mode 상태

    // 뷰 ID를 속성으로 정의
    private val lightViewIds = mutableListOf<Triple<Int, Int, Int>>() // (colorCheckBoxId, colorSpinnerId, brightnessSpinnerId)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupLights()

        // Music Mode 버튼 클릭 리스너
        binding.musicModeButton.setOnClickListener {
            if (action_status) toggleActionMode()
            toggleMusicMode()
        }

        // Behavior Mode 버튼 클릭 리스너
        binding.actionModeButton.setOnClickListener {
            if (music_status) toggleMusicMode()
            toggleActionMode()
        }
    }

    private fun setupLights() {
        val lightContainer = binding.lightContainer

        for (i in 1..lightCount) {
            val lightLayout = createLightLayout(i)
            lightContainer.addView(lightLayout)

            val (colorCheckBoxId, colorSpinnerId, brightnessSpinnerId) = lightViewIds[i - 1]
            val colorCheckBox = lightLayout.findViewById<CheckBox>(colorCheckBoxId)
            val colorSpinner = lightLayout.findViewById<Spinner>(colorSpinnerId)
            val brightnessSpinner = lightLayout.findViewById<Spinner>(brightnessSpinnerId)
            binding.musicModeButton.setBackgroundColor(Color.GRAY)
            binding.actionModeButton.setBackgroundColor(Color.GRAY)
        }
    }

    private fun createLightLayout(lightNumber: Int): ConstraintLayout {
        val colorCheckBoxId = View.generateViewId()
        val colorSpinnerId = View.generateViewId()
        val brightnessSpinnerId = View.generateViewId()
        val editButtonId = View.generateViewId() // Edit Color 버튼 ID 생성

        lightViewIds.add(Triple(colorCheckBoxId, colorSpinnerId, brightnessSpinnerId))

        val lightLayout = ConstraintLayout(this).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 100, 0, 16)
            }
        }

        // 뷰 생성
        val textView = TextView(this).apply {
            id = View.generateViewId()
            text = "Light $lightNumber Status: OFF"
            textSize = 30f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        val imageView = ImageView(this).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.light_off)
            layoutParams = ConstraintLayout.LayoutParams(400, 400)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val colorCheckBox = CheckBox(this).apply {
            id = colorCheckBoxId
            text = "Use Edit Color"
            isChecked = false
        }

        val colorSpinner = Spinner(this).apply {
            id = colorSpinnerId
            adapter = ArrayAdapter.createFromResource(
                this@LightActivity,
                R.array.color_array,
                android.R.layout.simple_spinner_item
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        val brightnessSpinner = Spinner(this).apply {
            id = brightnessSpinnerId
            adapter = ArrayAdapter.createFromResource(
                this@LightActivity,
                R.array.brightness_array,
                android.R.layout.simple_spinner_item
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        val editButton = Button(this).apply {
            id = editButtonId
            text = "  Edit Color $lightNumber  "
            setBackgroundColor(selectedColors[lightNumber - 1]) // Edit Color 버튼 배경색 설정
            setOnClickListener { openColorPicker(lightNumber, this) }
        }

        // 색상 콤보박스와 Edit Color 버튼을 함께 배치
        val comboBoxLayout = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(colorSpinner) // 왼쪽에 색상 콤보박스
            addView(editButton)   // 오른쪽에 Edit Color 버튼
        }

        val onButton = Button(this).apply {
            id = View.generateViewId()
            text = "ON"
            setOnClickListener { toggleLight(lightNumber, "ON") }
        }

        val offButton = Button(this).apply {
            id = View.generateViewId()
            text = "OFF"
            setOnClickListener { toggleLight(lightNumber, "OFF") }
        }

        val onOffLayout = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(onButton)
            addView(offButton)
        }

        lightLayout.addView(textView)
        lightLayout.addView(imageView)
        lightLayout.addView(brightnessSpinner) // 밝기 스피너를 상단에 배치
        lightLayout.addView(comboBoxLayout)    // 색상 콤보박스와 Edit Color 버튼
        lightLayout.addView(colorCheckBox)
        lightLayout.addView(onOffLayout)

        // Constraint 설정
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

        set.connect(brightnessSpinner.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
        set.connect(brightnessSpinner.id, ConstraintSet.TOP, imageView.id, ConstraintSet.BOTTOM, 16)
        set.connect(brightnessSpinner.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16)
        set.setHorizontalBias(brightnessSpinner.id, 0.5f)

        set.connect(comboBoxLayout.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
        set.connect(comboBoxLayout.id, ConstraintSet.TOP, brightnessSpinner.id, ConstraintSet.BOTTOM, 16)
        set.connect(comboBoxLayout.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16)
        set.setHorizontalBias(comboBoxLayout.id, 0.5f)

        set.connect(colorCheckBox.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
        set.connect(colorCheckBox.id, ConstraintSet.TOP, comboBoxLayout.id, ConstraintSet.BOTTOM, 16)
        set.connect(colorCheckBox.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16)
        set.setHorizontalBias(colorCheckBox.id, 0.5f)

        set.connect(onOffLayout.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
        set.connect(onOffLayout.id, ConstraintSet.TOP, colorCheckBox.id, ConstraintSet.BOTTOM, 16)
        set.connect(onOffLayout.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16)
        set.setHorizontalBias(onOffLayout.id, 0.5f)

        set.applyTo(lightLayout)

        lightStatusViews.add(textView)
        lightImageViews.add(imageView)

        return lightLayout
    }
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
            val lightLayout = binding.lightContainer.getChildAt(lightNumber - 1) as ConstraintLayout

            // ID를 이용하여 뷰를 찾습니다.
            val (colorCheckBoxId, colorSpinnerId, brightnessSpinnerId) = lightViewIds[lightNumber - 1]
            val colorCheckBox = lightLayout.findViewById<CheckBox>(colorCheckBoxId)
            val colorSpinner = lightLayout.findViewById<Spinner>(colorSpinnerId)
            val brightnessSpinner = lightLayout.findViewById<Spinner>(brightnessSpinnerId)

            // 색상 결정
            val color: Int
            if (colorCheckBox.isChecked) {
                // Use color from color picker
                color = selectedColors[lightNumber - 1]
            } else {
                // Use color from spinner
                val colorName = colorSpinner.selectedItem.toString()
                color = getColorFromName(colorName)
            }

            // 밝기 결정
            val brightnessString = brightnessSpinner.selectedItem.toString()
            val brightness = brightnessString.replace("%", "").toIntOrNull() ?: 100
            val r = (Color.red(color) * (brightness / 100.0)).toInt()
            val g = (Color.green(color) * (brightness / 100.0)).toInt()
            val b = (Color.blue(color) * (brightness / 100.0)).toInt()

            mqttService?.publishLightToggle(lightNumber, "$status/$r/$g/$b")

            val statusText = "Light $lightNumber Status: $status"
            lightStatusViews[lightNumber - 1].text = statusText
            val lightImage = if (status == "ON") R.drawable.light_on else R.drawable.light_off
            lightImageViews[lightNumber - 1].setImageResource(lightImage)
        }
    }
    // Music Mode 상태 토글
    private fun toggleMusicMode() {
        music_status = !music_status
        val color = if (music_status) Color.parseColor("#6200EE") else Color.GRAY
        mqttService?.publishMusicMode(music_status)
        binding.musicModeButton.setBackgroundColor(color)
    }

    // Behavior Mode 상태 토글
    private fun toggleActionMode() {
        action_status = !action_status
        val color = if (action_status) Color.parseColor("#6200EE") else Color.GRAY
        mqttService?.publishActionMode(action_status)
        binding.actionModeButton.setBackgroundColor(color)
    }

    // Color Picker 다이얼로그
    private fun openColorPicker(lightNumber: Int, editButton: Button) {
        val dialog = AmbilWarnaDialog(this, selectedColors[lightNumber - 1], object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                selectedColors[lightNumber - 1] = color
                editButton.setBackgroundColor(color) // 선택한 색상으로 Edit Color 버튼 배경 변경
            }

            override fun onCancel(dialog: AmbilWarnaDialog?) {
                // 취소 시 아무 동작 없음
            }
        })
        dialog.show()
    }


    override fun onStart() {
        super.onStart()
        bindService(Intent(this, MqttService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}