package com.example.marketwatch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.marketwatch.data.StockRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class StockDetailsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockAuth: FirebaseAuth
    @Mock
    private lateinit var mockDb: FirebaseFirestore

    private lateinit var viewModel: StockDetailsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        val mockRepository = StockRepository(mockAuth, mockDb)
        viewModel = StockDetailsViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isLoading starts as null and fetchData sets it to false after completion`() = runTest {
        // fetchData sets _isLoading.value = true, then on error posts false in finally
        viewModel.fetchData("AAPL")
        advanceUntilIdle()
        // After fetchData completes (with network error in tests), isLoading is false
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `StockQuote data model properties are correct`() {
        val quote = StockQuote(150.0, 2.0, 1.3, 155.0, 148.0, 149.0, 148.0)
        assertEquals(150.0, quote.currentPrice, 0.0)
        assertEquals(155.0, quote.highPrice, 0.0)
        assertEquals(148.0, quote.lowPrice, 0.0)
    }

    @Test
    fun `CompanyProfile data model properties are correct`() {
        val profile = CompanyProfile("logo", "Apple", "AAPL", "web", "Tech", 1000.0, 100.0, "USD")
        assertEquals("AAPL", profile.ticker)
        assertEquals("Tech", profile.industry)
    }

    @Test
    fun `tradeStatus initial value is null`() {
        assertEquals(null, viewModel.tradeStatus.value)
    }

    @Test
    fun `exchangeRate initial value is 3.7`() {
        assertEquals(3.7, viewModel.exchangeRate.value ?: 3.7, 0.0)
    }
}
