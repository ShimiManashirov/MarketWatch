package com.example.marketwatch.data

import android.util.Log
import com.example.marketwatch.PortfolioItem
import com.example.marketwatch.data.local.AppDatabase
import com.example.marketwatch.data.local.StockEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class PortfolioRepository(private val localDb: AppDatabase) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // קבלת נתונים מ-Firebase בזמן אמת
    fun getPortfolioUpdates(): Flow<List<PortfolioItem>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = db.collection("users").document(userId)
            .collection("watchlist")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }

                val items = snapshots?.mapNotNull { doc ->
                    try {
                        PortfolioItem(
                            symbol = doc.getString("symbol") ?: "",
                            description = doc.getString("description") ?: "",
                            quantity = doc.getDouble("quantity") ?: 0.0,
                            isFavorite = doc.getBoolean("isFavorite") ?: false
                        )
                    } catch (ex: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(items)
            }
        
        awaitClose { registration.remove() }
    }

    // סנכרון ל-Room
    suspend fun syncLocalDatabase(items: List<PortfolioItem>) {
        val entities = items.map {
            StockEntity(
                symbol = it.symbol,
                description = it.description,
                quantity = it.quantity,
                isFavorite = it.isFavorite
            )
        }
        localDb.stockDao().deleteAll()
        localDb.stockDao().insertStocks(entities)
    }
}
