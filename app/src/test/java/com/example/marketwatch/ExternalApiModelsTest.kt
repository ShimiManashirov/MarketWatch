package com.example.marketwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class ExternalApiModelsTest {

    @Test
    fun `FrankfurterResponse properties verification`() {
        val rates = mapOf("ILS" to 3.7, "EUR" to 0.92)
        val response = FrankfurterResponse(
            amount = 1.0,
            base = "USD",
            date = "2024-03-20",
            rates = rates
        )
        
        assertEquals(1.0, response.amount, 0.0)
        assertEquals("USD", response.base)
        assertEquals("2024-03-20", response.date)
        assertEquals(2, response.rates.size)
        assertEquals(3.7, response.rates["ILS"]!!, 0.0)
    }

    @Test
    fun `StockLookupResponse verification with multiple results`() {
        val symbols = listOf(
            StockSymbol("Apple", "AAPL", "AAPL", "Common"),
            StockSymbol("Microsoft", "MSFT", "MSFT", "Common"),
            StockSymbol("Amazon", "AMZN", "AMZN", "Common")
        )
        val response = StockLookupResponse(count = 3, result = symbols)
        
        assertEquals(3, response.count)
        assertEquals("AAPL", response.result[0].symbol)
        assertEquals("MSFT", response.result[1].symbol)
        assertEquals("AMZN", response.result[2].symbol)
    }

    @Test
    fun `StockCandles properties verification`() {
        val candles = StockCandles(
            closePrices = listOf(150.0, 155.0),
            highPrices = listOf(156.0, 160.0),
            lowPrices = listOf(149.0, 154.0),
            openPrices = listOf(150.0, 156.0),
            status = "ok",
            timestamps = listOf(1000L, 2000L),
            volumes = listOf(100L, 200L)
        )
        
        assertEquals("ok", candles.status)
        assertEquals(2, candles.closePrices!!.size)
        assertEquals(150.0, candles.closePrices!![0], 0.0)
        assertEquals(155.0, candles.closePrices!![1], 0.0)
    }

    @Test
    fun `StockNews detailed property verification`() {
        val news = StockNews(
            id = 12345L,
            category = "top news",
            datetime = 1710936000L,
            headline = "Market Update Headline",
            image = "https://example.com/image.png",
            symbol = "AAPL",
            source = "Reuters",
            summary = "A brief summary of the market update news article...",
            url = "https://reuters.com/news/12345"
        )
        
        assertEquals(12345L, news.id)
        assertEquals("top news", news.category)
        assertEquals("Market Update Headline", news.headline)
        assertEquals("Reuters", news.source)
        assertEquals("AAPL", news.symbol)
    }
}
