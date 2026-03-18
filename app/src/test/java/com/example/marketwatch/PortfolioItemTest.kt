package com.example.marketwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class PortfolioItemTest {

    @Test
    fun `PortfolioItem correctly holds and updates values`() {
        val item = PortfolioItem(
            symbol = "TSLA",
            description = "Tesla, Inc.",
            quantity = 5.0,
            isFavorite = true,
            totalCost = 1000.0
        )
        
        assertEquals("TSLA", item.symbol)
        assertEquals("Tesla, Inc.", item.description)
        assertEquals(5.0, item.quantity, 0.0)
        assertEquals(true, item.isFavorite)
        assertEquals(1000.0, item.totalCost, 0.0)

        // Test setters
        item.quantity = 10.0
        item.isFavorite = false
        assertEquals(10.0, item.quantity, 0.0)
        assertEquals(false, item.isFavorite)
    }

    @Test
    fun `PortfolioItem handles default values`() {
        val item = PortfolioItem()
        assertEquals("", item.symbol)
        assertEquals("", item.description)
        assertEquals(0.0, item.quantity, 0.0)
        assertEquals(false, item.isFavorite)
        assertEquals(0.0, item.totalCost, 0.0)
    }
}
