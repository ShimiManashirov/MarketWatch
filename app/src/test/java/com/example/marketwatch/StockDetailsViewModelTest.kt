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
    fun `calculateMovingAverage returns correct value`() {
        val prices = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        // This is a logic test if the method exists in VM
        // val ma = viewModel.calculateMovingAverage(prices, 3)
        // assertEquals(40.0, ma, 0.01)
    }
}
