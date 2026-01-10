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

data class CompanyProfile(
    @SerializedName("logo") val logo: String,
    @SerializedName("name") val name: String,
    @SerializedName("ticker") val ticker: String,
    @SerializedName("weburl") val weburl: String,
    @SerializedName("finnhubIndustry") val industry: String?,
    @SerializedName("marketCapitalization") val marketCap: Double?,
    @SerializedName("shareOutstanding") val sharesOutstanding: Double?,
    @SerializedName("currency") val currency: String?
)

data class StockNews(
    @SerializedName("id") val id: Long,
    @SerializedName("category") val category: String,
    @SerializedName("datetime") val datetime: Long,
    @SerializedName("headline") val headline: String,
    @SerializedName("image") val image: String,
    @SerializedName("related") val symbol: String,
    @SerializedName("source") val source: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("url") val url: String
)

data class StockCandles(
    @SerializedName("c") val closePrices: List<Double>?,
    @SerializedName("h") val highPrices: List<Double>?,
    @SerializedName("l") val lowPrices: List<Double>?,
    @SerializedName("o") val openPrices: List<Double>?,
    @SerializedName("s") val status: String,
    @SerializedName("t") val timestamps: List<Long>?,
    @SerializedName("v") val volumes: List<Long>?
)
