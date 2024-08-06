package com.example.lightapp

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lightapp.databinding.ActivityFeedbackBinding
import okhttp3.*
import java.io.IOException

class FeedbackActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val binding by lazy {ActivityFeedbackBinding.inflate(layoutInflater)}
    private val serverUri = BuildConfig.SERVER_URI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val userId = intent.getStringExtra("USER_ID").orEmpty()

        binding.buttonSubmitFeedback.setOnClickListener {
            val feedback = binding.editTextFeedback.text.toString()

            if (feedback.isNotEmpty()) {
                binding.textViewFeedbackError.visibility = TextView.GONE
                submitFeedback(userId, feedback)
            } else {
                binding.textViewFeedbackError.text = "Please enter your feedback"
                binding.textViewFeedbackError.visibility = TextView.VISIBLE
            }
        }
    }

    private fun submitFeedback(userId: String, feedback: String) {
        val url = "http://${serverUri}:5000/submitFeedback"
        val formBody = FormBody.Builder()
            .add("user_id", userId)
            .add("feedback", feedback)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.textViewFeedbackError.apply {
                        text = "Failed to submit feedback: ${e.message}"
                        visibility = TextView.VISIBLE
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@FeedbackActivity, "Feedback submitted successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        binding.textViewFeedbackError.apply {
                            text = "Failed to submit feedback: ${response.body()?.string()}"
                            visibility = TextView.VISIBLE
                        }
                    }
                }
            }
        })
    }
}
