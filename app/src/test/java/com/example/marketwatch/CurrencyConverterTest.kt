package com.example.marketwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyConverterTest {

    @Test
    fun testUsdToIlsConversion() {
        val usdAmount = 100.0
        val exchangeRate = 3.7
        val expectedIls = 370.0
        
        val actualIls = usdAmount * exchangeRate
        
        assertEquals("Conversion from USD to ILS should be correct", expectedIls, actualIls, 0.001)
    }

    @Test
    fun testIlsToUsdConversion() {
        val ilsAmount = 370.0
        val exchangeRate = 3.7
        val expectedUsd = 100.0
        
        val actualUsd = ilsAmount / exchangeRate
        
        assertEquals("Conversion from ILS to USD should be correct", expectedUsd, actualUsd, 0.001)
    }
}
