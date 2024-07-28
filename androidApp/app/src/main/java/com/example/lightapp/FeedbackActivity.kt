package com.example.lightapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lightapp.databinding.ActivityFeedbackBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedbackActivity : AppCompatActivity() {

    private val binding by lazy { ActivityFeedbackBinding.inflate(layoutInflater)}
    private lateinit var apiService: ApiService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_feedback)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun submitFeedback(){
        val feedback = binding.feedbackEditText.text.toString() // 피드백 데이터 형태? 위치, 밝기 / 색상

        if (feedback.isNotEmpty()) {
            val feedbackObj = Feedback("user", feedback)
            val call = apiService.submitFeedback(feedbackObj)

            call.enqueue(object : Callback<FeedbackResponse?> {
                override fun onResponse(call: Call<FeedbackResponse?>, response: Response<FeedbackResponse?>) {
                    val responseBody = response.body()
                    if (response.isSuccessful && responseBody != null) {
                        Toast.makeText(this@FeedbackActivity, "Feedback submitted successfully", Toast.LENGTH_SHORT).show()
                        Log.d("FeedbackActivity", "Feedback submitted successfully: ${responseBody.status}")
                    } else {
                        Toast.makeText(this@FeedbackActivity, "Failed to submit feedback", Toast.LENGTH_SHORT).show()
                        Log.e("FeedbackActivity", "Failed to submit feedback: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<FeedbackResponse?>, t: Throwable) {
                    Toast.makeText(this@FeedbackActivity, "Error submitting feedback", Toast.LENGTH_SHORT).show()
                    Log.e("FeedbackActivity", "Error submitting feedback", t)
                }
            })
        } else {
            Toast.makeText(this, "Please enter your feedback", Toast.LENGTH_SHORT).show()
        }
    }
}