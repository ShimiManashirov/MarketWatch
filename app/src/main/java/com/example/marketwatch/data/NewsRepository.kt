package com.example.marketwatch.data

import com.example.marketwatch.FinnhubApiClient
import com.example.marketwatch.StockNews
import com.example.marketwatch.data.local.AppDatabase
import com.example.marketwatch.data.local.NewsBookmarkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository responsible for handling news data.
 * It manages both remote data from the Finnhub API and local bookmarks in the Room database.
 */
class NewsRepository(private val database: AppDatabase) {

    private val api = FinnhubApiClient.apiService
    private val bookmarkDao = database.newsBookmarkDao()

    /**
     * Fetches the latest market news for a specific stock symbol.
     * @param symbol The stock symbol (e.g., "AAPL").
     * @return A list of [StockNews] objects.
     */
    suspend fun getStockNews(symbol: String): List<StockNews> = withContext(Dispatchers.IO) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val to = sdf.format(Date())
            val from = sdf.format(Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L))
            
            val response = api.getStockNews(symbol, from, to, FinnhubApiClient.API_KEY).execute()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Retrieves all bookmarked news articles as a [Flow].
     */
    fun getAllBookmarks(): Flow<List<StockNews>> {
        return bookmarkDao.getAllBookmarks().map { entities ->
            entities.map { it.toStockNews() }
        }
    }

    /**
     * Checks if an article is bookmarked.
     */
    fun isBookmarked(newsId: Long): Flow<Boolean> = bookmarkDao.isBookmarked(newsId)

    /**
     * Saves an article to the local bookmarks.
     */
    suspend fun addBookmark(news: StockNews) = withContext(Dispatchers.IO) {
        bookmarkDao.insertBookmark(news.toEntity())
    }

    /**
     * Removes an article from the local bookmarks.
     */
    suspend fun removeBookmark(newsId: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(newsId)
    }

    // Mapper functions to convert between Data Models and Entities

    private fun StockNews.toEntity() = NewsBookmarkEntity(
        id = id,
        category = category,
        datetime = datetime,
        headline = headline,
        image = image,
        symbol = symbol,
        source = source,
        summary = summary,
        url = url
    )

    private fun NewsBookmarkEntity.toStockNews() = StockNews(
        id = id,
        category = category,
        datetime = datetime,
        headline = headline,
        image = image,
        symbol = symbol,
        source = source,
        summary = summary,
        url = url
    )
}
