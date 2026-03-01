package com.example.marketwatch

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val profilePictureUrl: String? = null,
    val currency: String = "USD",
    val timezone: String = "UTC"
)
