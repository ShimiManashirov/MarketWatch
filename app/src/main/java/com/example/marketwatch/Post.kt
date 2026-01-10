package com.example.marketwatch

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfilePicture: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp? = null,
    val likes: List<String> = emptyList() // רשימה של UIDs של משתמשים שעשו לייק
)
