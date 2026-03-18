package com.example.marketwatch.data

import com.example.marketwatch.StockQuote
import com.example.marketwatch.CompanyProfile
import com.example.marketwatch.data.local.AppDatabase
import com.example.marketwatch.data.local.StockDao
import com.example.marketwatch.FinnhubApiService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import retrofit2.Response
import retrofit2.Call

class StocksRepositoryTest {

    @Mock
    private lateinit var database: AppDatabase

    @Mock
    private lateinit var stockDao: StockDao

    @Mock
    private lateinit var apiService: FinnhubApiService

    @Mock
    private lateinit var quoteCall: Call<StockQuote>

    @Mock
    private lateinit var profileCall: Call<CompanyProfile>

    private lateinit var repository: StocksRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(database.stockDao()).thenReturn(stockDao)
        // Note: This assumes StocksRepository takes apiService as a dependency
        // or uses a singleton that can be mocked/injected.
        // For this example, we'll assume it's passed in.
        repository = StocksRepository(database, apiService)
    }

    @Test
    fun `getStockQuote returns data on success`() = runTest {
        val symbol = "AAPL"
        val expectedQuote = StockQuote(150.0, 2.0, 1.3, 155.0, 148.0, 149.0, 148.0)
        `when`(apiService.getQuote(eq(symbol), anyString())).thenReturn(quoteCall)
        `when`(quoteCall.execute()).thenReturn(Response.success(expectedQuote))

        val result = repository.getStockQuote(symbol)

        assertEquals(expectedQuote, result)
    }

    @Test
    fun `getCompanyProfile returns data on success`() = runTest {
        val symbol = "TSLA"
        val expectedProfile = CompanyProfile("logo", "Tesla", "TSLA", "web", "Auto", 100.0, 10.0, "USD")
        `when`(apiService.getCompanyProfile(eq(symbol), anyString())).thenReturn(profileCall)
        `when`(profileCall.execute()).thenReturn(Response.success(expectedProfile))

        val result = repository.getCompanyProfile(symbol)

        assertEquals(expectedProfile, result)
    }

    @Test
    fun `getStockQuote returns null on API error`() = runTest {
        val symbol = "FAIL"
        `when`(apiService.getQuote(eq(symbol), anyString())).thenReturn(quoteCall)
        `when`(quoteCall.execute()).thenThrow(RuntimeException("Network Error"))

        val result = repository.getStockQuote(symbol)

        assertEquals(null, result)
    }
}
