package com.example.marketwatch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.marketwatch.data.NewsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var repository: NewsRepository

    private lateinit var viewModel: NewsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Mock default behavior for bookmarks
        `when`(repository.getAllBookmarks()).thenReturn(flowOf(emptyList()))
        
        viewModel = NewsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchNewsForSymbol success updates newsList`() = runTest {
        val symbol = "AAPL"
        val mockNews = listOf(
            StockNews(1, "biz", 1000L, "Headline 1", "img1", "AAPL", "Src 1", "Sum 1", "url1"),
            StockNews(2, "tech", 2000L, "Headline 2", "img2", "AAPL", "Src 2", "Sum 2", "url2")
        )
        `when`(repository.getStockNews(symbol)).thenReturn(mockNews)

        viewModel.fetchNewsForSymbol(symbol)
        
        assertEquals(true, viewModel.isLoading.value)
        advanceUntilIdle()

        assertEquals(mockNews, viewModel.newsList.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `toggleBookmark adds or removes bookmark based on current state`() = runTest {
        val news = StockNews(1, "biz", 1000L, "H", "I", "S", "Src", "Sum", "U")
        
        // Test Add
        viewModel.toggleBookmark(news, false)
        advanceUntilIdle()
        verify(repository).addBookmark(news)
        
        // Test Remove
        viewModel.toggleBookmark(news, true)
        advanceUntilIdle()
        verify(repository).removeBookmark(news.id)
    }

    @Test
    fun `fetchNewsForSymbol error sets errorMessage`() = runTest {
        val symbol = "FAIL"
        `when`(repository.getStockNews(symbol)).thenThrow(RuntimeException("Network Error"))

        viewModel.fetchNewsForSymbol(symbol)
        advanceUntilIdle()

        assertEquals(false, viewModel.isLoading.value)
        assert(viewModel.errorMessage.value?.contains("Network Error") == true)
    }

    @Test
    fun `clearError resets errorMessage`() {
        viewModel.clearError()
        assertEquals(null, viewModel.errorMessage.value)
    }
}
