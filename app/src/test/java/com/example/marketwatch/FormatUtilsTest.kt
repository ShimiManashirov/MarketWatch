package com.example.marketwatch

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class FormatUtilsTest {

    @Test
    fun `formatCurrency formats USD correctly`() {
        val result = FormatUtils.formatCurrency(1234.567)
        // Note: NumberFormat depends on locale, but we expect $1,234.57 for US locale
        assert(result.contains("$"))
        assert(result.contains("1,234.57"))
    }

    @Test
    fun `formatCurrencyIls formats ILS correctly`() {
        val result = FormatUtils.formatCurrencyIls(1234.567)
        assert(result.contains("₪"))
        assert(result.contains("1,234.57"))
    }

    @Test
    fun `formatPercentage formats positive values with plus sign`() {
        val result = FormatUtils.formatPercentage(1.25)
        assertEquals("+1.25%", result)
    }

    @Test
    fun `formatPercentage formats negative values with minus sign`() {
        val result = FormatUtils.formatPercentage(-0.5)
        assertEquals("-0.50%", result)
    }

    @Test
    fun `formatPercentage formats zero correctly`() {
        val result = FormatUtils.formatPercentage(0.0)
        assertEquals("+0.00%", result)
    }

    @Test
    fun `formatDateTime returns Just now for null date`() {
        val result = FormatUtils.formatDateTime(null)
        assertEquals("Just now", result)
    }

    @Test
    fun `formatDateTime formats valid date correctly`() {
        val calendar = Calendar.getInstance()
        calendar.set(2026, Calendar.OCTOBER, 24, 14, 30)
        val date = calendar.time
        val result = FormatUtils.formatDateTime(date)
        // Pattern: "MMM dd, HH:mm"
        assert(result.contains("Oct 24"))
        assert(result.contains("14:30"))
    }

    @Test
    fun `formatDecimal formats with default two decimal places`() {
        val result = FormatUtils.formatDecimal(12.3456)
        assertEquals("12.35", result)
    }

    @Test
    fun `formatDecimal formats with specified decimal places`() {
        val result = FormatUtils.formatDecimal(12.3, 3)
        assertEquals("12.300", result)
    }
}
