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
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
            StockNews(1, "biz", 1000L, "Headline 1", "img1", "AAPL", "Src 1", "Sum 1", "url1")
        )
        `when`(repository.getStockNews(symbol)).thenReturn(mockNews)

        viewModel.fetchNewsForSymbol(symbol)
        advanceUntilIdle()

        assertEquals(mockNews, viewModel.newsList.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `toggleBookmark logic verification`() = runTest {
        val news = StockNews(1, "biz", 1000L, "H", "I", "S", "Src", "Sum", "U")
        
        viewModel.toggleBookmark(news, false)
        advanceUntilIdle()
        verify(repository).addBookmark(news)
        
        viewModel.toggleBookmark(news, true)
        advanceUntilIdle()
        verify(repository).removeBookmark(news.id)
    }

    @Test
    fun `clearError resets errorMessage`() {
        viewModel.clearError()
        assertEquals(null, viewModel.errorMessage.value)
    }
}
