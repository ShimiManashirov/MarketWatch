package com.example.marketwatch.data

import com.example.marketwatch.PortfolioItem
import com.example.marketwatch.data.local.AppDatabase
import com.example.marketwatch.data.local.StockDao
import com.example.marketwatch.data.local.StockEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any as kAny
import org.mockito.kotlin.verify as kVerify

class PortfolioRepositoryTest {

    @Mock
    private lateinit var mockLocalDb: AppDatabase
    @Mock
    private lateinit var mockStockDao: StockDao
    @Mock
    private lateinit var mockAuth: FirebaseAuth
    @Mock
    private lateinit var mockDb: FirebaseFirestore

    private lateinit var portfolioRepository: PortfolioRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(mockLocalDb.stockDao()).thenReturn(mockStockDao)
        portfolioRepository = PortfolioRepository(mockLocalDb, mockAuth, mockDb)
    }

    @Test
    fun `syncLocalDatabase correctly maps and inserts items`() = runTest {
        val items = listOf(
            PortfolioItem("AAPL", "Apple", 10.0, true, 1500.0),
            PortfolioItem("TSLA", "Tesla", 5.0, false, 1000.0)
        )
        
        portfolioRepository.syncLocalDatabase(items)
        
        kVerify(mockStockDao).deleteAll()
        kVerify(mockStockDao).insertStocks(kAny())
    }

    @Test
    fun `syncLocalDatabase with empty list handles correctly`() = runTest {
        portfolioRepository.syncLocalDatabase(emptyList())
        kVerify(mockStockDao).deleteAll()
        kVerify(mockStockDao).insertStocks(emptyList())
    }

    @Test
    fun `PortfolioItem properties and mapping verification`() {
        val item = PortfolioItem("MSFT", "Microsoft", 20.0, true, 4000.0)
        val entity = StockEntity(item.symbol, item.description, item.quantity, item.isFavorite, item.totalCost)
        
        assertEquals("MSFT", entity.symbol)
        assertEquals(20.0, entity.quantity, 0.0)
        assertEquals(true, entity.isFavorite)
    }

    @Test
    fun `PortfolioItem handles default values correctly`() {
        val item = PortfolioItem()
        assertEquals("", item.symbol)
        assertEquals(0.0, item.quantity, 0.0)
        assertEquals(false, item.isFavorite)
    }

    @Test
    fun `StockEntity mapping handles edge cases`() {
        val entity = StockEntity("GOOGL", "Google", 0.0, false, 0.0)
        assertEquals("GOOGL", entity.symbol)
        assertEquals(0.0, entity.quantity, 0.0)
    }
}
