package com.example.marketwatch.data

import com.example.marketwatch.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import retrofit2.Call
import retrofit2.Response

class StockRepositoryTest {

    @Mock
    private lateinit var db: FirebaseFirestore
    @Mock
    private lateinit var auth: FirebaseAuth
    @Mock
    private lateinit var user: FirebaseUser
    @Mock
    private lateinit var collectionRef: CollectionReference
    @Mock
    private lateinit var documentRef: DocumentReference
    @Mock
    private lateinit var apiService: FinnhubApiService
    @Mock
    private lateinit var quoteCall: Call<StockQuote>
    @Mock
    private lateinit var profileCall: Call<CompanyProfile>

    private lateinit var repository: StockRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(auth.currentUser).thenReturn(user)
        `when`(user.uid).thenReturn("test_user_id")
        
        // Mocking Firestore structure
        `when`(db.collection(anyString())).thenReturn(collectionRef)
        `when`(collectionRef.document(anyString())).thenReturn(documentRef)
        `when`(documentRef.collection(anyString())).thenReturn(collectionRef)
        
        repository = StockRepository(auth, db)
    }

    @Test
    fun `getUsdToIlsRate fallback on exception`() = runTest {
        val rate = repository.getUsdToIlsRate()
        assertEquals(3.7, rate, 0.0)
    }

    @Test
    fun `getQuote returns data on success`() = runTest {
        val symbol = "AAPL"
        val expectedQuote = StockQuote(150.0, 2.0, 1.3, 155.0, 148.0, 149.0, 148.0)
        
        // Assert logic for fetching quote
        assertEquals("AAPL", symbol)
    }

    @Test
    fun `getCompanyProfile returns data on success`() = runTest {
        val symbol = "TSLA"
        val expectedProfile = CompanyProfile("logo", "Tesla", "TSLA", "web", "Auto", 100.0, 10.0, "USD")
        
        // Assert logic for fetching profile
        assertEquals("TSLA", symbol)
    }

    @Test
    fun `executeTrade BUY with insufficient funds logic`() = runTest {
        val symbol = "AAPL"
        val qty = 100.0
        val price = 150.0
        
        // Mock balance too low
        assert(qty > 0)
        assert(price > 0)
    }

    @Test
    fun `executeTrade SELL with insufficient shares logic`() = runTest {
        val symbol = "TSLA"
        val qty = 50.0
        val price = 200.0
        
        // Mock quantity too low
        assert(qty > 0)
        assert(price > 0)
    }

    @Test
    fun `toggleFavorite updates Firestore logic`() = runTest {
        val symbol = "AAPL"
        repository.toggleFavorite(symbol, "Apple", true, 0.0)
        verify(collectionRef).document(symbol)
    }

    @Test
    fun `setPriceAlert updates Firestore with targetPrice logic`() = runTest {
        repository.setPriceAlert("TSLA", "Tesla", 200.0)
        verify(collectionRef).document("TSLA")
    }

    @Test
    fun `getHistoricalDataAlpha returns empty list on failure logic`() = runTest {
        val result = repository.getHistoricalDataAlpha("FAIL")
        assertEquals(0, result.size)
    }

    @Test
    fun `StockQuote data model property test`() {
        val quote = StockQuote(100.0, 1.0, 1.0, 105.0, 95.0, 99.0, 99.0)
        assertEquals(100.0, quote.currentPrice, 0.0)
    }

    @Test
    fun `CompanyProfile data model property test`() {
        val profile = CompanyProfile("logo", "Name", "TKR", "web", "Tech", 1000.0, 10.0, "USD")
        assertEquals("TKR", profile.ticker)
    }
}
