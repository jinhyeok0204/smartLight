package com.example.lightapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// object -> 싱글톤 객체로 선언. 애플리케이션 전체에서 동일한 인스턴스를 사용하게 됨.
object RetrofitClient {
    private var retrofit: Retrofit?= null

    fun getClient(baseUrl: String): Retrofit{
        // synchronized : 다른 스레드가 이 블록 내의 코드를 동시에 실행하지 못하도록 보장

        return retrofit ?: synchronized(this){
            retrofit ?: Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .also{retrofit = it} // 생성된 Retrofit 인스턴스를 retrofit 변수에 저장
        }
    }
}