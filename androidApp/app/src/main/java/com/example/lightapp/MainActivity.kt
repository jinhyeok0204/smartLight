package com.example.lightapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import com.example.lightapp.databinding.ActivityMainBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

// 로그인을 위한 Activity
class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val server_uri = BuildConfig.SERVER_URI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

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

        binding.buttonGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    //login 하는 함수
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
                            text = "Invalid credentials"
                            visibility = TextView.VISIBLE
                        }
                    }
                }
            }
        })
    }
}