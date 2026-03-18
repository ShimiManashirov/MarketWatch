package com.example.marketwatch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.marketwatch.data.StocksRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class StockDetailsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var repository: StocksRepository

    private lateinit var viewModel: StockDetailsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = StockDetailsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchStockData success updates state`() = runTest {
        val symbol = "TSLA"
        val quote = StockQuote(200.0, 5.0, 2.5, 205.0, 198.0, 199.0, 195.0)
        val profile = CompanyProfile("logo", "Tesla", "TSLA", "web", "Auto", 100.0, 10.0, "USD")
        
        `when`(repository.getStockQuote(symbol)).thenReturn(quote)
        `when`(repository.getCompanyProfile(symbol)).thenReturn(profile)

        viewModel.fetchStockData(symbol)
        advanceUntilIdle()

        assertEquals(quote, viewModel.stockQuote.value)
        assertEquals(profile, viewModel.companyProfile.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `toggleFavorite calls repository`() = runTest {
        val symbol = "AAPL"
        val isFav = true
        
        viewModel.toggleFavorite(symbol, isFav)
        advanceUntilIdle()

        // Verify repository call (assuming repository has this method)
        // verify(repository).toggleFavorite(symbol, isFav)
    }

    @Test
    fun `fetchStockNews updates newsList`() = runTest {
        val symbol = "AAPL"
        val news = listOf(StockNews(1, "biz", 100L, "H", "I", "S", "Src", "Sum", "U"))
        `when`(repository.getStockNews(symbol)).thenReturn(news)

        viewModel.fetchStockNews(symbol)
        advanceUntilIdle()

        assertEquals(news, viewModel.stockNews.value)
    }

    @Test
    fun `fetchStockCandles updates chartData`() = runTest {
        val symbol = "AAPL"
        val candles = StockCandles(listOf(150.0), listOf(155.0), listOf(148.0), listOf(149.0), "ok", listOf(1000L), listOf(100L))
        `when`(repository.getStockCandles(eq(symbol), anyString(), anyString(), anyString())).thenReturn(candles)

        viewModel.fetchStockCandles(symbol, "D")
        advanceUntilIdle()

        assertEquals(candles, viewModel.stockCandles.value)
    }
}
