package com.example.marketwatch

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AlphaVantageApiClient {
    private const val BASE_URL = "https://www.alphavantage.co/"
    const val API_KEY = "YOUR_ALPHA_VANTAGE_KEY" // אתה יכול להוציא מפתח חינמי ב-alphavantage.co

    val apiService: AlphaVantageApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AlphaVantageApiService::class.java)
    }
}
