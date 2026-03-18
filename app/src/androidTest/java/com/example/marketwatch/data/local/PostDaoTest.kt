package com.example.marketwatch.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class PostDaoTest {

    private lateinit var postDao: PostDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        postDao = db.postDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writePostAndReadInList() = runBlocking {
        val post = PostEntity(
            id = "1",
            userId = "u1",
            userName = "Test User",
            userProfilePicture = "url",
            content = "Hello Room",
            imageUrl = null,
            timestamp = 123456789L,
            likesCount = 5
        )
        postDao.insertPosts(listOf(post))
        val allPosts = postDao.getAllPosts().first()
        assertEquals(allPosts[0].content, post.content)
        assertEquals(allPosts[0].id, post.id)
    }

    @Test
    @Throws(Exception::class)
    fun deleteAllPosts() = runBlocking {
        val post1 = PostEntity("1", "u1", "N1", "P1", "C1", null, 1L, 0)
        val post2 = PostEntity("2", "u2", "N2", "P2", "C2", null, 2L, 0)
        postDao.insertPosts(listOf(post1, post2))
        
        postDao.deleteAll()
        
        val allPosts = postDao.getAllPosts().first()
        assertEquals(0, allPosts.size)
    }

    @Test
    @Throws(Exception::class)
    fun updatePostAndVerify() = runBlocking {
        val post = PostEntity("1", "u1", "N1", "P1", "Original", null, 1L, 0)
        postDao.insertPosts(listOf(post))
        
        val updatedPost = PostEntity("1", "u1", "N1", "P1", "Updated", null, 1L, 10)
        postDao.insertPosts(listOf(updatedPost)) // Room @Insert(onConflict = REPLACE)
        
        val allPosts = postDao.getAllPosts().first()
        assertEquals("Updated", allPosts[0].content)
        assertEquals(10, allPosts[0].likesCount)
    }
}
