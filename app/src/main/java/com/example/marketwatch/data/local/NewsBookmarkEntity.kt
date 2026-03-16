package com.example.marketwatch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a bookmarked news article.
 * This class is used to store news data locally for offline access and personalization.
 */
@Entity(tableName = "news_bookmarks")
data class NewsBookmarkEntity(
    @PrimaryKey val id: Long,
    val category: String,
    val datetime: Long,
    val headline: String,
    val image: String,
    val symbol: String,
    val source: String,
    val summary: String,
    val url: String,
    val bookmarkedAt: Long = System.currentTimeMillis()
)
