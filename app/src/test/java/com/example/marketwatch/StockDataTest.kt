package com.example.marketwatch

import org.junit.Assert.*
import org.junit.Test

class StockDataTest {

    @Test
    fun `StockQuote initialization and values verification`() {
        val quote = StockQuote(
            currentPrice = 175.25,
            change = 3.50,
            percentChange = 2.04,
            highPrice = 178.00,
            lowPrice = 172.50,
            openPrice = 173.00,
            previousClose = 171.75
        )

        assertEquals(175.25, quote.currentPrice, 0.0)
        assertEquals(3.50, quote.change, 0.0)
        assertEquals(2.04, quote.percentChange, 0.0)
        assertEquals(178.00, quote.highPrice, 0.0)
        assertEquals(172.50, quote.lowPrice, 0.0)
        assertEquals(173.00, quote.openPrice, 0.0)
        assertEquals(171.75, quote.previousClose, 0.0)
    }

    @Test
    fun `StockSymbol initialization and values verification`() {
        val symbol = StockSymbol(
            description = "NVIDIA Corporation",
            displaySymbol = "NVDA",
            symbol = "NVDA",
            type = "Common Stock"
        )

        assertEquals("NVIDIA Corporation", symbol.description)
        assertEquals("NVDA", symbol.displaySymbol)
        assertEquals("NVDA", symbol.symbol)
        assertEquals("Common Stock", symbol.type)
    }

    @Test
    fun `StockLookupResponse initialization with multiple symbols`() {
        val symbol1 = StockSymbol("Apple Inc.", "AAPL", "AAPL", "Common")
        val symbol2 = StockSymbol("Microsoft", "MSFT", "MSFT", "Common")
        val response = StockLookupResponse(count = 2, result = listOf(symbol1, symbol2))

        assertEquals(2, response.count)
        assertEquals(2, response.result.size)
        assertEquals("AAPL", response.result[0].symbol)
        assertEquals("MSFT", response.result[1].symbol)
    }

    @Test
    fun `StockQuote handles negative changes correctly`() {
        val quote = StockQuote(100.0, -5.0, -4.76, 106.0, 99.0, 105.0, 105.0)
        assertTrue(quote.change < 0)
        assertTrue(quote.percentChange < 0)
        assertEquals(-5.0, quote.change, 0.0)
    }

    @Test
    fun `StockSymbol handles empty description gracefully`() {
        val symbol = StockSymbol("", "TEST", "TEST", "Equity")
        assertEquals("", symbol.description)
        assertEquals("TEST", symbol.displaySymbol)
    }

    @Test
    fun `CompanyProfile exhaustive field verification`() {
        val profile = CompanyProfile(
            logo = "logo_url",
            name = "Google",
            ticker = "GOOGL",
            weburl = "google.com",
            industry = "Technology",
            marketCap = 1800000.0,
            sharesOutstanding = 13000.0,
            currency = "USD"
        )

        assertEquals("logo_url", profile.logo)
        assertEquals("Google", profile.name)
        assertEquals("GOOGL", profile.ticker)
        assertEquals("google.com", profile.weburl)
        assertEquals("Technology", profile.industry)
        assertEquals(1800000.0, profile.marketCap!!, 0.0)
        assertEquals(13000.0, profile.sharesOutstanding!!, 0.0)
        assertEquals("USD", profile.currency)
    }
}
