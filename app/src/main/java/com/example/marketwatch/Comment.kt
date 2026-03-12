package com.example.marketwatch

import com.google.firebase.Timestamp

/**
 * Data model representing a comment on a community post.
 *
 * @property id Unique identifier for the comment.
 * @property postId The ID of the post this comment belongs to.
 * @property userId The ID of the user who wrote the comment.
 * @property userName The name of the user who wrote the comment.
 * @property userProfilePicture URL to the user's profile picture.
 * @property content The text content of the comment.
 * @property timestamp The time the comment was created.
 */
data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfilePicture: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null
)
