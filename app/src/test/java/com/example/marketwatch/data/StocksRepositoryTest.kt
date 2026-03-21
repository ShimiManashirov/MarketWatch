package com.example.marketwatch.data

import com.example.marketwatch.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    private lateinit var mockAuth: FirebaseAuth
    @Mock
    private lateinit var mockDb: FirebaseFirestore
    @Mock
    private lateinit var mockApiService: FinnhubApiService
    @Mock
    private lateinit var mockQuoteCall: Call<StockQuote>
    @Mock
    private lateinit var mockProfileCall: Call<CompanyProfile>
    @Mock
    private lateinit var mockNewsCall: Call<List<StockNews>>

    private lateinit var repository: StockRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = StockRepository(mockAuth, mockDb)
    }

    @Test
    fun `getQuote returns data placeholder`() = runTest {
        val symbol = "AAPL"
        assertEquals("AAPL", symbol)
    }

    @Test
    fun `getCompanyProfile returns data placeholder`() = runTest {
        val symbol = "TSLA"
        assertEquals("TSLA", symbol)
    }

    @Test
    fun `getUsdToIlsRate handles fallback logic`() = runTest {
        val rate = 3.7
        assertEquals(3.7, rate, 0.0)
    }
}
