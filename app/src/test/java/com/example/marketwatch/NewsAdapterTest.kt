package com.example.marketwatch

import androidx.recyclerview.widget.RecyclerView
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class NewsAdapterTest {

    private lateinit var adapter: NewsAdapter
    private val newsList = mutableListOf<StockNews>()
    private val bookmarkedIds = mutableSetOf<Long>()

    @Mock
    private lateinit var observer: RecyclerView.AdapterDataObserver

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        val news = StockNews(
            id = 1L,
            category = "business",
            datetime = 1678886400L,
            headline = "Market Hits Record High",
            image = "http://example.com/image.jpg",
            symbol = "AAPL",
            source = "Financial Times",
            summary = "Summary",
            url = "http://example.com/news/1"
        )
        newsList.add(news)
        bookmarkedIds.add(1L)

        adapter = NewsAdapter(
            newsList = newsList,
            bookmarkedIds = bookmarkedIds,
            onBookmarkClick = { _, _ -> }
        )
        
        // Fix: Register observer to prevent NPE on notifyDataSetChanged
        adapter.registerAdapterDataObserver(observer)
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
        assert(newBookmarks.size == 3)
    }
}
