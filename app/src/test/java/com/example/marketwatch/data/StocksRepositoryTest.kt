package com.example.marketwatch.data

import com.example.marketwatch.*
import com.example.marketwatch.data.local.AppDatabase
import com.example.marketwatch.data.local.StockDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import retrofit2.Call
import retrofit2.Response

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

    @Mock
    private lateinit var newsCall: Call<List<StockNews>>

    private lateinit var repository: StocksRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(database.stockDao()).thenReturn(stockDao)
        // Note: StocksRepository takes FinnhubApiService as a dependency.
        repository = StocksRepository(database, apiService)
    }

    @Test
    fun `getStockQuote success returns quote`() = runTest {
        val symbol = "AAPL"
        val expected = StockQuote(150.0, 2.0, 1.3, 155.0, 148.0, 149.0, 148.0)
        `when`(apiService.getQuote(eq(symbol), anyString())).thenReturn(quoteCall)
        `when`(quoteCall.execute()).thenReturn(Response.success(expected))

        val result = repository.getStockQuote(symbol)

        assertEquals(expected, result)
    }

    @Test
    fun `getCompanyProfile success returns profile`() = runTest {
        val symbol = "TSLA"
        val expected = CompanyProfile("logo", "Tesla", "TSLA", "web", "Auto", 100.0, 10.0, "USD")
        `when`(apiService.getCompanyProfile(eq(symbol), anyString())).thenReturn(profileCall)
        `when`(profileCall.execute()).thenReturn(Response.success(expected))

        val result = repository.getCompanyProfile(symbol)

        assertEquals(expected, result)
    }

    @Test
    fun `getStockNews success returns list`() = runTest {
        val symbol = "AAPL"
        val expected = listOf(StockNews(1, "biz", 100L, "H", "I", "S", "Src", "Sum", "U"))
        `when`(apiService.getStockNews(eq(symbol), anyString(), anyString(), anyString())).thenReturn(newsCall)
        `when`(newsCall.execute()).thenReturn(Response.success(expected))

        val result = repository.getStockNews(symbol)

        assertEquals(expected, result)
    }

    @Test
    fun `getStockQuote network failure returns null`() = runTest {
        val symbol = "FAIL"
        `when`(apiService.getQuote(eq(symbol), anyString())).thenReturn(quoteCall)
        `when`(quoteCall.execute()).thenThrow(RuntimeException("Network failure"))

        val result = repository.getStockQuote(symbol)

        assertEquals(null, result)
    }

    @Test
    fun `getStockQuote API error returns null`() = runTest {
        val symbol = "ERROR"
        `when`(apiService.getQuote(eq(symbol), anyString())).thenReturn(quoteCall)
        `when`(quoteCall.execute()).thenReturn(Response.error(404, mock()))

        val result = repository.getStockQuote(symbol)

        assertEquals(null, result)
    }
}
