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
                        // שיפור הקריאה: תמיכה גם ב-Long וגם ב-Double מה-Firestore
                        val symbol = doc.getString("symbol") ?: ""
                        val description = doc.getString("description") ?: ""
                        val isFavorite = doc.getBoolean("isFavorite") ?: false
                        
                        // קבלת הכמות בצורה בטוחה (מספרים ב-Firestore יכולים להיות מסוגים שונים)
                        val quantityRaw = doc.get("quantity")
                        val quantity = when (quantityRaw) {
                            is Number -> quantityRaw.toDouble()
                            else -> 0.0
                        }

                        if (symbol.isNotBlank()) {
                            PortfolioItem(symbol, description, quantity, isFavorite)
                        } else null
                    } catch (ex: Exception) {
                        Log.e("PortfolioRepo", "Error parsing doc ${doc.id}", ex)
                        null
                    }
                } ?: emptyList()
                
                trySend(items)
            }
        
        awaitClose { registration.remove() }
    }

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
