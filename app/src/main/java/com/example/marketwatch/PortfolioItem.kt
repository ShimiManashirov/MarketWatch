package com.example.marketwatch

import com.google.firebase.firestore.PropertyName

data class PortfolioItem(
    @get:PropertyName("symbol") @set:PropertyName("symbol") var symbol: String = "",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("quantity") @set:PropertyName("quantity") var quantity: Double = 0.0,
    @get:PropertyName("isFavorite") @set:PropertyName("isFavorite") var isFavorite: Boolean = false,
    @get:PropertyName("totalCost") @set:PropertyName("totalCost") var totalCost: Double = 0.0
)
