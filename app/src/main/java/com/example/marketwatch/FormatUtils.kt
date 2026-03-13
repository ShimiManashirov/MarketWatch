package com.example.marketwatch

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * FormatUtils is a centralized utility class providing standardized formatting logic 
 * for the Market Watch application. 
 * 
 * This class ensures that currency, dates, and percentages are displayed consistently 
 * across all UI components, including the Dashboard, Portfolio, and Community Feed.
 */
object FormatUtils {

    /**
     * Formats a raw Double value into a user-friendly currency string (USD).
     * 
     * Example: 1234.567 -> "$1,234.57"
     *
     * @param amount The numerical value to be formatted.
     * @return A localized currency string prefixed with the '$' symbol.
     */
    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        return format.format(amount)
    }

    /**
     * Formats a raw Double value into a user-friendly Israeli Shekel string (ILS).
     * 
     * Example: 1234.567 -> "₪1,234.57"
     *
     * @param amount The numerical value in ILS.
     * @return A localized currency string prefixed with the '₪' symbol.
     */
    fun formatCurrencyIls(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("he", "IL"))
        return format.format(amount)
    }

    /**
     * Formats a percentage change value with a sign and two decimal places.
     * 
     * Example: 1.25 -> "+1.25%", -0.5 -> "-0.50%"
     *
     * @param percent The percentage value.
     * @return A formatted string suitable for display in stock price change labels.
     */
    fun formatPercentage(percent: Double): String {
        return String.format(Locale.getDefault(), "%+.2f%%", percent)
    }

    /**
     * Converts a Java Date object into a readable date and time string.
     * Uses the "MMM dd, HH:mm" pattern (e.g., Oct 24, 14:30).
     *
     * @param date The date to be formatted. If null, returns "Just now".
     * @return A formatted timestamp string.
     */
    fun formatDateTime(date: Date?): String {
        if (date == null) return "Just now"
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(date)
    }

    /**
     * Formats a numerical value with a specific number of decimal places.
     * 
     * @param value The value to format.
     * @param decimals The number of decimal places to include.
     * @return A formatted string representation of the number.
     */
    fun formatDecimal(value: Double, decimals: Int = 2): String {
        return String.format(Locale.getDefault(), "%.${decimals}f", value)
    }
}
