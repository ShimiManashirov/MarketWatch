package com.example.marketwatch.data

import com.example.marketwatch.Post
import com.example.marketwatch.data.local.AppDatabase
import com.example.marketwatch.data.local.PostDao
import com.example.marketwatch.data.local.PostEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class PostsRepositoryTest {

    @Mock
    private lateinit var db: FirebaseFirestore
    @Mock
    private lateinit var localDb: AppDatabase
    @Mock
    private lateinit var postDao: PostDao
    @Mock
    private lateinit var auth: FirebaseAuth
    @Mock
    private lateinit var storage: FirebaseStorage

    private lateinit var repository: PostsRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(localDb.postDao()).thenReturn(postDao)
        repository = PostsRepository(db, localDb, auth, storage)
    }

    @Test
    fun `getLocalPosts returns mapped posts from local database`() = runTest {
        // Arrange
        val postEntities = listOf(
            PostEntity("1", "u1", "User 1", "", "Content 1", null, 1000L, 5),
            PostEntity("2", "u2", "User 2", "", "Content 2", "img", 2000L, 10)
        )
        `when`(postDao.getAllPosts()).thenReturn(flowOf(postEntities))

        // Act
        val result = repository.getLocalPosts().first()

        // Assert
        assertEquals(2, result.size)
        assertEquals("1", result[0].id)
        assertEquals("u1", result[0].userId)
        assertEquals("Content 1", result[0].content)
        assertEquals("2", result[1].id)
        assertEquals("Content 2", result[1].content)
    }
}
