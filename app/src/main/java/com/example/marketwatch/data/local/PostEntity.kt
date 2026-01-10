package com.example.marketwatch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userProfilePicture: String,
    val content: String,
    val imageUrl: String?,
    val timestamp: Long, // Store as Long for SQLite
    val likesCount: Int
)
