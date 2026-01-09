package com.example.marketwatch

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface AlphaVantageApiService {
    @GET("query")
    fun getDailySeries(
        @Query("function") function: String = "TIME_SERIES_DAILY",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): Call<AlphaVantageResponse>
}

data class AlphaVantageResponse(
    @com.google.gson.annotations.SerializedName("Time Series (Daily)")
    val timeSeries: Map<String, DailyData>?
)

data class DailyData(
    @com.google.gson.annotations.SerializedName("4. close")
    val close: String
)
