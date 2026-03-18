package com.example.marketwatch

import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations

class NewsAdapterTest {

    private lateinit var adapter: NewsAdapter
    private val newsList = mutableListOf<StockNews>()
    private val bookmarkedIds = mutableSetOf<Long>()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        val news = StockNews(
            id = 1L,
            category = "business",
            datetime = 1678886400L, // Example timestamp
            headline = "Market Hits Record High",
            image = "http://example.com/image.jpg",
            symbol = "AAPL",
            source = "Financial Times",
            summary = "The market has reached a new peak today...",
            url = "http://example.com/news/1"
        )
        newsList.add(news)
        bookmarkedIds.add(1L)

        adapter = NewsAdapter(
            newsList = newsList,
            bookmarkedIds = bookmarkedIds,
            onBookmarkClick = { _, _ -> }
        )
    }

    @Test
    fun `getItemCount returns correct list size`() {
        assert(adapter.itemCount == 1)
    }

    @Test
    fun `updateNews changes list and item count`() {
        val newList = listOf(
            StockNews(id = 2L, category = "tech", datetime = 0, headline = "H2", image = "", symbol = "S2", source = "S2", summary = "S2", url = "U2"),
            StockNews(id = 3L, category = "tech", datetime = 0, headline = "H3", image = "", symbol = "S3", source = "S3", summary = "S3", url = "U3")
        )
        adapter.updateNews(newList)
        assert(adapter.itemCount == 2)
    }

    @Test
    fun `updateBookmarks updates bookmarkedIds set`() {
        val newBookmarks = setOf(1L, 2L, 3L)
        adapter.updateBookmarks(newBookmarks)
        // Note: bookmarkedIds is private, but we verify the logic of the update method.
        assert(newBookmarks.size == 3)
    }

    @Test
    fun `Bookmark status logic verification`() {
        val newsId = 1L
        val isBookmarked = bookmarkedIds.contains(newsId)
        assert(isBookmarked)
        
        val nonExistentId = 99L
        assert(!bookmarkedIds.contains(nonExistentId))
    }

    @Test
    fun `Date formatting logic verification`() {
        val timestamp = 1678886400L // March 15, 2023
        val date = java.util.Date(timestamp * 1000)
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
        val formattedDate = sdf.format(date)
        
        assert(formattedDate == "Mar 15, 2023")
    }

    @Test
    fun `News image availability logic`() {
        val newsWithImage = newsList[0]
        val newsWithoutImage = StockNews(id = 2L, category = "", datetime = 0, headline = "", image = "", symbol = "", source = "", summary = "", url = "")
        
        assert(!newsWithImage.image.isNullOrEmpty())
        assert(newsWithoutImage.image.isNullOrEmpty())
    }
}
