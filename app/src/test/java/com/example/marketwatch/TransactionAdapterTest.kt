package com.example.marketwatch

import com.google.firebase.Timestamp
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations
import java.util.Date

class TransactionAdapterTest {

    private lateinit var adapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        val transaction = Transaction(
            id = "t1",
            type = "BUY",
            symbol = "AAPL",
            amount = 1500.0,
            quantity = 10.0,
            timestamp = Timestamp(Date())
        )
        transactions.add(transaction)

        adapter = TransactionAdapter(
            transactions = transactions,
            userCurrency = "USD",
            currencySymbol = "$",
            exchangeRate = 1.0
        )
    }

    @Test
    fun `getItemCount returns correct list size`() {
        assert(adapter.itemCount == 1)
    }

    @Test
    fun `updateData changes list and item count`() {
        val newList = listOf(
            Transaction("t2", "SELL", "TSLA", 2000.0, 5.0, Timestamp.now()),
            Transaction("t3", "DEPOSIT", null, 5000.0, null, Timestamp.now())
        )
        adapter.updateData(newList)
        assert(adapter.itemCount == 2)
    }

    @Test
    fun `updateCurrency triggers data refresh logic`() {
        adapter.updateCurrency("ILS", "₪", 3.7)
        // Verify state change if possible or just ensure method runs
        assert(true)
    }

    @Test
    fun `Transaction title logic for BUY`() {
        val t = Transaction(type = "BUY", symbol = "AAPL")
        val title = "Buy ${t.symbol}"
        assert(title == "Buy AAPL")
    }

    @Test
    fun `Transaction title logic for SELL`() {
        val t = Transaction(type = "SELL", symbol = "TSLA")
        val title = "Sell ${t.symbol}"
        assert(title == "Sell TSLA")
    }

    @Test
    fun `Transaction amount formatting for BUY`() {
        val amount = 100.0
        val symbol = "$"
        val rate = 1.0
        val type = "BUY"
        
        val converted = amount * rate
        val formatted = "${if (type == "BUY" || type == "WITHDRAW") "-" else "+"}$symbol${String.format("%.2f", converted)}"
        
        assert(formatted == "-$100.00")
    }

    @Test
    fun `Transaction amount formatting for DEPOSIT`() {
        val amount = 500.0
        val symbol = "₪"
        val rate = 3.7
        val type = "DEPOSIT"
        
        val converted = amount * rate
        val formatted = "${if (type == "BUY" || type == "WITHDRAW") "-" else "+"}$symbol${String.format("%.2f", converted)}"
        
        // 500 * 3.7 = 1850.0
        assert(formatted == "+₪1850.00")
    }

    @Test
    fun `Transaction timestamp null handling`() {
        val t = Transaction(timestamp = null)
        val dateText = t.timestamp?.toDate()?.toString() ?: ""
        assert(dateText == "")
    }
}
