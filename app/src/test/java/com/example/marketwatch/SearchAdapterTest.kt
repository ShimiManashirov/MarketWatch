package com.example.marketwatch

import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations

class SearchAdapterTest {

    private lateinit var adapter: SearchAdapter
    private val stocks = mutableListOf<StockSymbol>()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        val stock = StockSymbol(
            description = "Apple Inc.",
            displaySymbol = "AAPL",
            symbol = "AAPL",
            type = "Common Stock"
        )
        stocks.add(stock)

        adapter = SearchAdapter(stocks)
    }

    @Test
    fun `getItemCount returns correct list size`() {
        assert(adapter.itemCount == 1)
    }

    @Test
    fun `updateData changes list and item count`() {
        val newList = listOf(
            StockSymbol("Microsoft", "MSFT", "MSFT", "Common"),
            StockSymbol("Google", "GOOGL", "GOOGL", "Common")
        )
        adapter.updateData(newList)
        assert(adapter.itemCount == 2)
    }

    @Test
    fun `Stock data binding verification`() {
        val stock = stocks[0]
        assert(stock.displaySymbol == "AAPL")
        assert(stock.description == "Apple Inc.")
    }

    @Test
    fun `Stock symbol property test`() {
        val stock = StockSymbol("Description", "Display", "Symbol", "Type")
        assert(stock.symbol == "Symbol")
    }

    @Test
    fun `Empty list handling test`() {
        val emptyAdapter = SearchAdapter(emptyList())
        assert(emptyAdapter.itemCount == 0)
    }

    @Test
    fun `List with multiple items test`() {
        val list = listOf(
            StockSymbol("D1", "S1", "S1", "T1"),
            StockSymbol("D2", "S2", "S2", "T2"),
            StockSymbol("D3", "S3", "S3", "T3")
        )
        val multiAdapter = SearchAdapter(list)
        assert(multiAdapter.itemCount == 3)
    }
}
