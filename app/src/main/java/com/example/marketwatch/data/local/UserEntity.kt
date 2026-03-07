package com.example.marketwatch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val name: String,
    val profilePictureUrl: String?,
    val currency: String,
    val timezone: String
)
