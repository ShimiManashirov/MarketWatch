package com.example.marketwatch

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

/**
 * Unit tests for the [FormatUtils] object.
 * 
 * These tests ensure that currency, date, and decimal formatting logic
 * remains accurate and handles edge cases correctly across different locales.
 */
class FormatUtilsTest {

    /**
     * Verifies that [FormatUtils.formatCurrency] correctly formats 
     * a positive double into a USD currency string.
     */
    @Test
    fun formatCurrency_isCorrect() {
        val amount = 1234.56
        val expected = "$1,234.56"
        assertEquals(expected, FormatUtils.formatCurrency(amount))
    }

    /**
     * Verifies that [FormatUtils.formatCurrency] handles zero correctly.
     */
    @Test
    fun formatCurrency_zero_isCorrect() {
        val amount = 0.0
        val expected = "$0.00"
        assertEquals(expected, FormatUtils.formatCurrency(amount))
    }

    /**
     * Verifies that [FormatUtils.formatCurrencyIls] correctly formats 
     * a positive double into an ILS currency string.
     */
    @Test
    fun formatCurrencyIls_isCorrect() {
        val amount = 1234.56
        // Note: Expected string might vary slightly based on environment locale settings,
        // but typically should include the '₪' symbol.
        val result = FormatUtils.formatCurrencyIls(amount)
        assert(result.contains("₪"))
    }

    /**
     * Verifies that [FormatUtils.formatPercentage] correctly formats 
     * a positive growth percentage.
     */
    @Test
    fun formatPercentage_positive_isCorrect() {
        val percent = 1.25
        val expected = "+1.25%"
        assertEquals(expected, FormatUtils.formatPercentage(percent))
    }

    /**
     * Verifies that [FormatUtils.formatPercentage] correctly formats 
     * a negative drop percentage.
     */
    @Test
    fun formatPercentage_negative_isCorrect() {
        val percent = -0.5
        val expected = "-0.50%"
        assertEquals(expected, FormatUtils.formatPercentage(percent))
    }

    /**
     * Verifies that [FormatUtils.formatDateTime] returns "Just now" when the date is null.
     */
    @Test
    fun formatDateTime_null_returnsJustNow() {
        val result = FormatUtils.formatDateTime(null)
        assertEquals("Just now", result)
    }

    /**
     * Verifies that [FormatUtils.formatDecimal] rounds correctly to two places by default.
     */
    @Test
    fun formatDecimal_default_isCorrect() {
        val value = 10.5678
        val expected = "10.57"
        assertEquals(expected, FormatUtils.formatDecimal(value))
    }

    /**
     * Verifies that [FormatUtils.formatDecimal] supports custom precision.
     */
    @Test
    fun formatDecimal_customPrecision_isCorrect() {
        val value = 10.5
        val expected = "10.500"
        assertEquals(expected, FormatUtils.formatDecimal(value, 3))
    }
}
