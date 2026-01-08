package com.example.marketwatch

import com.google.gson.annotations.SerializedName

data class StockLookupResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("result") val result: List<StockSymbol>
)

data class StockSymbol(
    @SerializedName("description") val description: String,
    @SerializedName("displaySymbol") val displaySymbol: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("type") val type: String
)

data class StockQuote(
    @SerializedName("c") val currentPrice: Double,
    @SerializedName("d") val change: Double,
    @SerializedName("dp") val percentChange: Double,
    @SerializedName("h") val highPrice: Double,
    @SerializedName("l") val lowPrice: Double,
    @SerializedName("o") val openPrice: Double,
    @SerializedName("pc") val previousClose: Double
)
