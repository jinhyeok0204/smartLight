package com.example.lightapp

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/submitFeedback")
    fun submitFeedback(@Body feedback: Feedback): Call<FeedbackResponse>
}