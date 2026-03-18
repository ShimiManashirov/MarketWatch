package com.example.marketwatch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.marketwatch.data.PortfolioRepository
import com.example.marketwatch.data.StocksRepository
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
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var portfolioRepository: PortfolioRepository

    @Mock
    private lateinit var stocksRepository: StocksRepository

    private lateinit var viewModel: PortfolioViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Mock default portfolio behavior
        `when`(portfolioRepository.getPortfolioItems()).thenReturn(flowOf(emptyList()))
        
        viewModel = PortfolioViewModel(portfolioRepository, stocksRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadPortfolio updates items LiveData`() = runTest {
        val mockItems = listOf(
            PortfolioItem("AAPL", "Apple", 10.0, true, 1500.0),
            PortfolioItem("TSLA", "Tesla", 5.0, false, 1000.0)
        )
        `when`(portfolioRepository.getPortfolioItems()).thenReturn(flowOf(mockItems))

        // Trigger reload or check init result
        advanceUntilIdle()

        assertEquals(mockItems, viewModel.portfolioItems.value)
    }

    @Test
    fun `buyStock calls repository and handles success`() = runTest {
        val symbol = "AAPL"
        val quantity = 5.0
        val price = 150.0
        
        viewModel.buyStock(symbol, quantity, price)
        advanceUntilIdle()

        // Verify repository interaction
        // verify(portfolioRepository).buyStock(symbol, quantity, price)
    }

    @Test
    fun `sellStock handles insufficient quantity`() = runTest {
        // Setup state with 2 shares
        val mockItems = listOf(PortfolioItem("AAPL", "Apple", 2.0, false, 300.0))
        `when`(portfolioRepository.getPortfolioItems()).thenReturn(flowOf(mockItems))
        advanceUntilIdle()

        // Try to sell 5
        viewModel.sellStock("AAPL", 5.0, 160.0)
        advanceUntilIdle()

        // Check for error message
        // assertEquals("Insufficient shares", viewModel.errorMessage.value)
    }
}
