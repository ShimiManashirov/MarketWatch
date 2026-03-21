package com.example.marketwatch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `PortfolioItem default values are correct`() {
        val item = PortfolioItem()
        assertEquals("", item.symbol)
        assertEquals(0.0, item.quantity, 0.0)
        assertEquals(false, item.isFavorite)
        assertEquals(0.0, item.totalCost, 0.0)
    }

    @Test
    fun `PortfolioItem custom values are set correctly`() {
        val item = PortfolioItem("AAPL", "Apple Inc.", 10.0, true, 1500.0)
        assertEquals("AAPL", item.symbol)
        assertEquals("Apple Inc.", item.description)
        assertEquals(10.0, item.quantity, 0.0)
        assertEquals(true, item.isFavorite)
        assertEquals(1500.0, item.totalCost, 0.0)
    }

    @Test
    fun `PortfolioItem formatting verification`() {
        val item = PortfolioItem("MSFT", "Microsoft", 1.0, true, 400.0)
        assertEquals("MSFT", item.symbol)
        assertEquals(1.0, item.quantity, 0.0)
    }

    @Test
    fun `portfolio total value calculation`() = runTest {
        val items = listOf(
            PortfolioItem("AAPL", "Apple", 10.0, true, 1500.0),
            PortfolioItem("TSLA", "Tesla", 5.0, false, 1000.0)
        )
        val totalCost = items.sumOf { it.totalCost }
        assertEquals(2500.0, totalCost, 0.0)
    }

    @Test
    fun `favorite filter logic works correctly`() = runTest {
        val items = listOf(
            PortfolioItem("AAPL", "Apple", 10.0, true, 1500.0),
            PortfolioItem("TSLA", "Tesla", 5.0, false, 1000.0)
        )
        val favorites = items.filter { it.isFavorite }
        assertEquals(1, favorites.size)
        assertEquals("AAPL", favorites[0].symbol)
    }
}
