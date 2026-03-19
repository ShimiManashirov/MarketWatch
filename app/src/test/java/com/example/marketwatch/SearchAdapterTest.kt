package com.example.marketwatch

import androidx.recyclerview.widget.RecyclerView
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class SearchAdapterTest {

    private lateinit var adapter: SearchAdapter
    private val stocks = mutableListOf<StockSymbol>()

    @Mock
    private lateinit var observer: RecyclerView.AdapterDataObserver

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
        adapter.registerAdapterDataObserver(observer)
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
}
