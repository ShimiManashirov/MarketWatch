package com.example.marketwatch

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object FinnhubApiClient {
    private const val BASE_URL = "https://finnhub.io/api/v1/"
    val API_KEY = "d5ev9khr01qusp5lrna0d5ev9khr01qusp5lrnag"

    val apiService: FinnhubApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinnhubApiService::class.java)
    }
}
