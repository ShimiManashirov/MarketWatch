package com.example.marketwatch.data

import com.example.marketwatch.StockNews
import com.example.marketwatch.data.local.AppDatabase
import com.example.marketwatch.data.local.NewsBookmarkDao
import com.example.marketwatch.data.local.NewsBookmarkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class NewsRepositoryTest {

    @Mock
    private lateinit var database: AppDatabase

    @Mock
    private lateinit var bookmarkDao: NewsBookmarkDao

    private lateinit var repository: NewsRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(database.newsBookmarkDao()).thenReturn(bookmarkDao)
        repository = NewsRepository(database)
    }

    @Test
    fun `getAllBookmarks maps entities to domain models`() = runTest {
        val entities = listOf(
            NewsBookmarkEntity(1, "biz", 100L, "H1", "I1", "S1", "Src1", "Sum1", "U1"),
            NewsBookmarkEntity(2, "tech", 200L, "H2", "I2", "S2", "Src2", "Sum2", "U2")
        )
        `when`(bookmarkDao.getAllBookmarks()).thenReturn(flowOf(entities))

        val result = repository.getAllBookmarks().first()

        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals("H1", result[0].headline)
        assertEquals(2L, result[1].id)
        assertEquals("H2", result[1].headline)
    }

    @Test
    fun `addBookmark converts news to entity and inserts`() = runTest {
        val news = StockNews(1, "biz", 100L, "H1", "I1", "S1", "Src1", "Sum1", "U1")
        
        repository.addBookmark(news)

        verify(bookmarkDao).insertBookmark(any())
    }

    @Test
    fun `removeBookmark calls dao delete`() = runTest {
        val newsId = 123L
        
        repository.removeBookmark(newsId)

        verify(bookmarkDao).deleteBookmark(newsId)
    }

    @Test
    fun `isBookmarked returns flow from dao`() = runTest {
        val newsId = 1L
        `when`(bookmarkDao.isBookmarked(newsId)).thenReturn(flowOf(true))

        val result = repository.isBookmarked(newsId).first()

        assertEquals(true, result)
        verify(bookmarkDao).isBookmarked(newsId)
    }
}
