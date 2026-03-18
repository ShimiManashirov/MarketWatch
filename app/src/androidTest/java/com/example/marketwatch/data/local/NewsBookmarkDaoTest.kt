package com.example.marketwatch.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class NewsBookmarkDaoTest {

    private lateinit var bookmarkDao: NewsBookmarkDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        bookmarkDao = db.newsBookmarkDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetBookmark() = runBlocking {
        val bookmark = NewsBookmarkEntity(
            id = 1L,
            category = "business",
            datetime = 123456L,
            headline = "Market Update",
            image = "url",
            symbol = "AAPL",
            source = "Reuters",
            summary = "Market is up",
            url = "http://example.com"
        )
        bookmarkDao.insertBookmark(bookmark)
        
        val bookmarks = bookmarkDao.getAllBookmarks().first()
        assertEquals(1, bookmarks.size)
        assertEquals("Market Update", bookmarks[0].headline)
    }

    @Test
    fun deleteBookmark() = runBlocking {
        val bookmark = NewsBookmarkEntity(1L, "cat", 1L, "H", "I", "S", "Src", "Sum", "U")
        bookmarkDao.insertBookmark(bookmark)
        
        bookmarkDao.deleteBookmark(1L)
        
        val bookmarks = bookmarkDao.getAllBookmarks().first()
        assertTrue(bookmarks.isEmpty())
    }

    @Test
    fun isBookmarkedReturnsTrue() = runBlocking {
        val bookmark = NewsBookmarkEntity(1L, "cat", 1L, "H", "I", "S", "Src", "Sum", "U")
        bookmarkDao.insertBookmark(bookmark)
        
        val isBookmarked = bookmarkDao.isBookmarked(1L).first()
        assertTrue(isBookmarked)
    }

    @Test
    fun isBookmarkedReturnsFalseForMissingId() = runBlocking {
        val isBookmarked = bookmarkDao.isBookmarked(999L).first()
        assertFalse(isBookmarked)
    }
}
