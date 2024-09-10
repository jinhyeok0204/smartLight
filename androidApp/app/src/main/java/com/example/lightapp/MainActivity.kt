package com.example.lightapp

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.lightapp.databinding.ActivityMainBinding
import okhttp3.*
import java.io.IOException

// 로그인을 위한 Activity
class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val server_uri = BuildConfig.SERVER_URI

    // 마이크 권한 요청 코드
    private val REQUEST_CODE_MIC_PERMISSION = 1001

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 권한을 확인하고 필요한 경우 요청
        checkAndRequestPermissions()

        // 음성 인식 서비스 시작
        startVoiceRecognitionService()

        // 로그인 버튼 클릭 시 동작
        binding.buttonLogin.setOnClickListener {
            val userId = binding.editTextUserId.text.toString()
            val password = binding.editTextPassword.text.toString()

            if (userId.isNotEmpty() && password.isNotEmpty()) {
                binding.textViewLoginError.visibility = TextView.GONE
                login(userId, password) // login
            } else {
                binding.textViewLoginError.text = "Please enter User ID and Password"
                binding.textViewLoginError.visibility = TextView.VISIBLE
            }
        }

        // 회원가입 버튼 클릭 시 동작
        binding.buttonGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // 권한을 확인하고 필요한 경우 요청하는 함수
    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionsToRequest = mutableListOf<String>()

            // RECORD_AUDIO 권한 확인
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.RECORD_AUDIO)
            }

            // Android 13 이상에서 FOREGROUND_SERVICE_MICROPHONE 권한 확인
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_MICROPHONE) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(android.Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
            }

            // 필요한 권한 요청
            if (permissionsToRequest.isNotEmpty()) {
                requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_CODE_MIC_PERMISSION)
            }
        }
    }

    // 권한 요청 결과를 처리하는 함수
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_MIC_PERMISSION) {
            // 권한이 허용되었는지 확인
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognitionService()
            } else {
                Toast.makeText(this, "마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 로그인 요청 함수
    private fun login(userId: String, password: String) {
        val url = "http://${server_uri}:5000/login"
        val formBody = FormBody.Builder()
            .add("username", userId)
            .add("password", password)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    findViewById<TextView>(R.id.textViewLoginError).apply {
                        text = "Login failed: ${e.message}"
                        visibility = TextView.VISIBLE
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        val intent = Intent(this@MainActivity, LightActivity::class.java)
                        intent.putExtra("USER_ID", userId)
                        startActivity(intent)
                        finish()
                    } else {
                        findViewById<TextView>(R.id.textViewLoginError).apply {
                            text = "잘못된 사용자 ID 또는 비밀번호입니다."
                            visibility = TextView.VISIBLE
                        }
                    }
                }
            }
        })
    }

    // 음성 인식 서비스 시작 함수
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startVoiceRecognitionService() {
        val intent = Intent(this, VoiceRecognitionService::class.java)
        startForegroundService(intent) // Foreground 서비스로 시작
    }
}