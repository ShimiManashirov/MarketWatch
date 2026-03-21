package com.example.marketwatch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.marketwatch.data.PostsRepository
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var repository: PostsRepository

    private lateinit var viewModel: FeedViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Mock the initial loadPosts call in init
        `when`(repository.getRemotePostsFlow()).thenReturn(flowOf(emptyList()))
        
        viewModel = FeedViewModel(repository)
    }

    @Test
    fun `loadPosts updates posts LiveData`() = runTest {
        // Arrange
        val posts = listOf(Post(id = "1", content = "Hello World"))
        `when`(repository.getRemotePostsFlow()).thenReturn(flowOf(posts))

        // Act
        viewModel.loadPosts()
        advanceUntilIdle()

        // Assert
        assert(viewModel.posts.value == posts)
        assert(viewModel.isLoading.value == false)
    }

    @Test
    fun `createPost calls repository`() = runTest {
        // Arrange
        val content = "New Post"
        
        val mockContext = mock(Context::class.java)

        // Act
        viewModel.createPost(mockContext, content, null)
        advanceUntilIdle()

        // Assert
        verify(repository).createPost(content, null)
    }

    @Test
    fun `toggleLike calls repository`() = runTest {
        // Arrange
        val post = Post(id = "1", content = "Like me")
        
        // Act
        viewModel.toggleLike(post)
        advanceUntilIdle()

        // Assert
        verify(repository).toggleLike(post)
    }
}
