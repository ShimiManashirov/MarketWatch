package com.example.marketwatch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.marketwatch.data.CommentRepository
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
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class CommentViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var repository: CommentRepository

    private lateinit var viewModel: CommentViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = CommentViewModel(repository)
    }

    @Test
    fun `setPostId triggers repository to fetch comments`() = runTest {
        // Arrange
        val postId = "post123"
        val comments = listOf(Comment(id = "c1", postId = postId, content = "Nice!"))
        `when`(repository.getCommentsForPost(postId)).thenReturn(flowOf(comments))
        
        val observer = Observer<List<Comment>> {}
        viewModel.comments.observeForever(observer)

        // Act
        viewModel.setPostId(postId)
        advanceUntilIdle()

        // Assert
        verify(repository).getCommentsForPost(postId)
        assert(viewModel.comments.value == comments)
        
        viewModel.comments.removeObserver(observer)
    }

    @Test
    fun `addComment calls repository with correct data`() = runTest {
        // Arrange
        val postId = "post123"
        val content = "Great post!"
        viewModel.setPostId(postId)

        // Act
        viewModel.addComment(content)
        advanceUntilIdle()

        // Assert
        verify(repository).addComment(postId, content)
    }
}
