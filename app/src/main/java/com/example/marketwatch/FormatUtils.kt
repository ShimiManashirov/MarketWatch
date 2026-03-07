package com.example.marketwatch

import java.text.SimpleDateFormat
import java.util.*

object FormatUtils {
    private val dateTimeFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatDateTime(date: Date?): String = date?.let { dateTimeFormatter.format(it) } ?: "Just now"
    
    fun formatDate(date: Date?): String = date?.let { dateFormatter.format(it) } ?: "Unknown date"

    fun formatApiDate(date: Date): String = apiDateFormatter.format(date)

    fun formatCurrency(amount: Double, currencyCode: String = "USD"): String {
        val symbol = when (currencyCode) {
            "USD" -> "$"
            "EUR" -> "€"
            "ILS" -> "₪"
            "GBP" -> "£"
            "JPY" -> "¥"
            else -> "$"
        }
        return "$symbol${String.format("%.2f", amount)}"
    }
    
    fun formatPercent(percent: Double): String {
        return "${String.format("%.2f", percent)}%"
    }
}
