package com.example.marketwatch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val symbol: String,
    val description: String,
    val quantity: Double,
    val isFavorite: Boolean,
    val currentPrice: Double = 0.0,
    val percentChange: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)
