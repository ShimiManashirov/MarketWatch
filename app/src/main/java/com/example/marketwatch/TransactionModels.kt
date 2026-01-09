package com.example.marketwatch

import com.google.firebase.Timestamp

data class Transaction(
    val id: String = "",
    val type: String = "", // "BUY", "SELL", "DEPOSIT", "WITHDRAW"
    val symbol: String? = null,
    val amount: Double = 0.0,
    val quantity: Double? = null,
    val timestamp: Timestamp? = null
)
