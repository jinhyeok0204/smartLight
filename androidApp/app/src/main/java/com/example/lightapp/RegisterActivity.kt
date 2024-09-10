package com.example.lightapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lightapp.databinding.ActivityRegisterBinding
import okhttp3.*
import org.w3c.dom.Text
import java.io.IOException
import java.util.regex.Pattern

// 회원가입 Activity
class RegisterActivity : AppCompatActivity() {

    private val binding by lazy {ActivityRegisterBinding.inflate(layoutInflater)}
    private val client = OkHttpClient()
    private val passwordPattern = Pattern.compile("^(?=.*[A-Z])(?=.*[!@#\$%^&*]).{1,20}$") // 대문자 포함, 특수문자 포함, 20글자 이내
    private val server_uri = BuildConfig.SERVER_URI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // layout 요소
        val editTextNewUserId = binding.editTextNewUserId
        val editTextNewPassword = binding.editTextConfirmPassword
        val editTextConfirmPassword = binding.editTextConfirmPassword
        val buttonRegisterNewUser =binding.buttonRegisterNewUser
        val textViewRegisterError =binding.textViewRegisterError

        // 회원가입 버튼 클릭 시 event 처리
        buttonRegisterNewUser.setOnClickListener {
            val newUserId = editTextNewUserId.text.toString()
            val newPassword = editTextNewPassword.text.toString()
            val confirmPassword = editTextConfirmPassword.text.toString()

            if (newUserId.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (newUserId.length <= 10) {
                    if (passwordPattern.matcher(newPassword).matches()) {
                        if (newPassword == confirmPassword) {
                            textViewRegisterError.visibility = TextView.GONE
                            register(newUserId, newPassword) // 회원 가입
                        } else { // Password와 ConfirmPassword 불일치
                            textViewRegisterError.text = "Passwords do not match"
                            textViewRegisterError.visibility = TextView.VISIBLE
                        }
                    } else { // Password 패턴 만족 x
                        textViewRegisterError.text = "Password must be 20 characters or less, include at least one uppercase letter and one special character"
                        textViewRegisterError.visibility = TextView.VISIBLE
                    }
                } else {  // UserID 길이 초과
                    textViewRegisterError.text = "User ID must be 10 characters or less"
                    textViewRegisterError.visibility = TextView.VISIBLE
                }
            } else { // 비어있는 칸이 있을 때
                textViewRegisterError.text = "Please enter User ID and Password"
                textViewRegisterError.visibility = TextView.VISIBLE
            }
        }
    }

    // 회원 가입 함수
    private fun register(userId: String, password: String) {
        val url = "http://$server_uri:5000/register"
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
                    binding.textViewRegisterError.apply{
                        text = "Registration failed: ${e.message}"
                        visibility = TextView.VISIBLE
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        binding.textViewRegisterError.apply {
                            text = "Registration failed: ${response.body()?.string()}"
                            visibility = TextView.VISIBLE
                        }
                    }
                }
            }
        })
    }
}
