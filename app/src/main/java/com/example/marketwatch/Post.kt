package com.example.marketwatch

import com.google.firebase.Timestamp

/**
 * Data model representing a community post.
 */
data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfilePicture: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp? = null,
    val likes: List<String> = emptyList(),
    val commentsCount: Int = 0
)
