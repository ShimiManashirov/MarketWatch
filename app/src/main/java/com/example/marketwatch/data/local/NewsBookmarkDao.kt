package com.example.marketwatch.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for news bookmarks.
 * Provides methods to interact with the news_bookmarks table in the Room database.
 */
@Dao
interface NewsBookmarkDao {

    /**
     * Retrieves all news bookmarks ordered by the time they were bookmarked.
     */
    @Query("SELECT * FROM news_bookmarks ORDER BY bookmarkedAt DESC")
    fun getAllBookmarks(): Flow<List<NewsBookmarkEntity>>

    /**
     * Checks if a specific news article is bookmarked.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM news_bookmarks WHERE id = :newsId)")
    fun isBookmarked(newsId: Long): Flow<Boolean>

    /**
     * Inserts a new bookmark.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: NewsBookmarkEntity)

    /**
     * Removes a bookmark by its ID.
     */
    @Query("DELETE FROM news_bookmarks WHERE id = :newsId")
    suspend fun deleteBookmark(newsId: Long)

    /**
     * Clears all bookmarks.
     */
    @Query("DELETE FROM news_bookmarks")
    suspend fun deleteAll()
}
