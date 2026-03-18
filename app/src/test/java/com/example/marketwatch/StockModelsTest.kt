package com.example.marketwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class StockModelsTest {

    @Test
    fun `StockSymbol correctly holds values`() {
        val symbol = StockSymbol("Apple Inc.", "AAPL", "AAPL", "Common Stock")
        assertEquals("Apple Inc.", symbol.description)
        assertEquals("AAPL", symbol.displaySymbol)
        assertEquals("AAPL", symbol.symbol)
        assertEquals("Common Stock", symbol.type)
    }

    @Test
    fun `StockQuote correctly holds values`() {
        val quote = StockQuote(
            currentPrice = 150.0,
            change = 2.5,
            percentChange = 1.67,
            highPrice = 155.0,
            lowPrice = 148.0,
            openPrice = 149.0,
            previousClose = 147.5
        )
        assertEquals(150.0, quote.currentPrice, 0.0)
        assertEquals(2.5, quote.change, 0.0)
        assertEquals(1.67, quote.percentChange, 0.0)
    }

    @Test
    fun `CompanyProfile handles nullable fields`() {
        val profile = CompanyProfile(
            logo = "url",
            name = "Test Co",
            ticker = "TEST",
            weburl = "web",
            industry = null,
            marketCap = null,
            sharesOutstanding = null,
            currency = "USD"
        )
        assertEquals(null, profile.industry)
        assertEquals("USD", profile.currency)
    }

    @Test
    fun `StockLookupResponse correctly holds list`() {
        val symbols = listOf(StockSymbol("A", "A", "A", "T"))
        val response = StockLookupResponse(1, symbols)
        assertEquals(1, response.count)
        assertEquals(symbols, response.result)
    }
}
