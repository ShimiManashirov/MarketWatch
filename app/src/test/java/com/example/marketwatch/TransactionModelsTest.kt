package com.example.marketwatch

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class TransactionModelsTest {

    @Test
    fun `Transaction correctly holds values`() {
        val now = Timestamp(Date())
        val transaction = Transaction(
            id = "t1",
            type = "BUY",
            symbol = "AAPL",
            amount = 1500.0,
            quantity = 10.0,
            timestamp = now
        )
        
        assertEquals("t1", transaction.id)
        assertEquals("BUY", transaction.type)
        assertEquals("AAPL", transaction.symbol)
        assertEquals(1500.0, transaction.amount, 0.0)
        assertEquals(10.0, transaction.quantity!!, 0.0)
        assertEquals(now, transaction.timestamp)
    }

    @Test
    fun `Transaction handles default values`() {
        val transaction = Transaction()
        assertEquals("", transaction.id)
        assertEquals("", transaction.type)
        assertEquals(null, transaction.symbol)
        assertEquals(0.0, transaction.amount, 0.0)
        assertEquals(null, transaction.quantity)
        assertEquals(null, transaction.timestamp)
    }
}
