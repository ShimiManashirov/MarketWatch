package com.example.marketwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyModelsTest {

    @Test
    fun `FrankfurterResponse correctly holds values`() {
        val rates = mapOf("ILS" to 3.7, "EUR" to 0.9)
        val response = FrankfurterResponse(
            amount = 1.0,
            base = "USD",
            date = "2024-03-15",
            rates = rates
        )
        
        assertEquals(1.0, response.amount, 0.0)
        assertEquals("USD", response.base)
        assertEquals("2024-03-15", response.date)
        assertEquals(rates, response.rates)
        assertEquals(3.7, response.rates["ILS"]!!, 0.0)
    }
}
