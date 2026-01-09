package com.example.marketwatch

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object FrankfurterApiClient {
    private const val BASE_URL = "https://api.frankfurter.app/"

    val apiService: FrankfurterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FrankfurterApiService::class.java)
    }
}
