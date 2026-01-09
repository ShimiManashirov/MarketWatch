package com.example.marketwatch

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface FrankfurterApiService {
    @GET("latest")
    fun getLatestRates(
        @Query("from") base: String = "USD"
    ): Call<FrankfurterResponse>
}
