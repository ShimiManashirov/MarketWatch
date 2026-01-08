package com.example.marketwatch

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubApiService {
    @GET("search")
    fun searchStock(
        @Query("q") query: String,
        @Query("token") apiKey: String
    ): Call<StockLookupResponse>

    @GET("quote")
    fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): Call<StockQuote>
}
