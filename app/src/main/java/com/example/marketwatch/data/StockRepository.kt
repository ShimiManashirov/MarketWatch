package com.example.marketwatch.data

import com.example.marketwatch.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class StockRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Firebase: Watchlist and Ownership
    fun getStockStatus(symbol: String): Flow<PortfolioItem?> = callbackFlow {
        val userId = auth.currentUser?.uid ?: return@callbackFlow
        val registration = db.collection("users").document(userId)
            .collection("watchlist").document(symbol)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    trySend(PortfolioItem(
                        symbol = snapshot.getString("symbol") ?: "",
                        description = snapshot.getString("description") ?: "",
                        quantity = snapshot.getDouble("quantity") ?: 0.0,
                        isFavorite = snapshot.getBoolean("isFavorite") ?: false
                    ))
                } else {
                    trySend(null)
                }
            }
        awaitClose { registration.remove() }
    }

    // Firebase: Toggle Favorite
    suspend fun toggleFavorite(symbol: String, description: String, currentState: Boolean, ownedQty: Double) {
        val userId = auth.currentUser?.uid ?: return
        val newState = !currentState
        val docRef = db.collection("users").document(userId).collection("watchlist").document(symbol)

        if (!newState && ownedQty <= 0) {
            docRef.delete().await()
        } else {
            val data = hashMapOf(
                "isFavorite" to newState,
                "symbol" to symbol,
                "description" to description
            )
            docRef.set(data, SetOptions.merge()).await()
        }
    }

    // Firebase: Execute Trade
    suspend fun executeTrade(symbol: String, description: String, quantity: Double, price: Double, isBuy: Boolean, isFavorite: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)
        val totalAmount = quantity * price

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val balance = userSnapshot.getDouble("balance") ?: 0.0
            
            val watchlistRef = userRef.collection("watchlist").document(symbol)
            val stockSnapshot = transaction.get(watchlistRef)
            val currentQty = stockSnapshot.getDouble("quantity") ?: 0.0

            if (isBuy) {
                if (balance < totalAmount) throw Exception("Insufficient funds")
                transaction.update(userRef, "balance", balance - totalAmount)
                transaction.set(watchlistRef, hashMapOf(
                    "symbol" to symbol,
                    "description" to description,
                    "quantity" to currentQty + quantity,
                    "isFavorite" to isFavorite,
                    "lastTradeAt" to Timestamp.now()
                ), SetOptions.merge())
            } else {
                if (currentQty < quantity) throw Exception("Insufficient shares")
                transaction.update(userRef, "balance", balance + totalAmount)
                val newQty = currentQty - quantity
                if (newQty <= 0 && !isFavorite) transaction.delete(watchlistRef)
                else transaction.update(watchlistRef, "quantity", newQty)
            }

            // Log transaction
            val transRef = userRef.collection("transactions").document()
            transaction.set(transRef, hashMapOf(
                "type" to if (isBuy) "BUY" else "SELL",
                "symbol" to symbol,
                "amount" to totalAmount,
                "quantity" to quantity,
                "timestamp" to Timestamp.now()
            ))
        }.await()
    }

    // Finnhub API Calls
    suspend fun getQuote(symbol: String): Response<StockQuote> = 
        FinnhubApiClient.apiService.getQuote(symbol, FinnhubApiClient.API_KEY).execute()

    suspend fun getCompanyProfile(symbol: String): Response<CompanyProfile> =
        FinnhubApiClient.apiService.getCompanyProfile(symbol, FinnhubApiClient.API_KEY).execute()

    suspend fun getNews(symbol: String): Response<List<StockNews>> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val to = sdf.format(Date())
        val from = sdf.format(Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L))
        return FinnhubApiClient.apiService.getStockNews(symbol, from, to, FinnhubApiClient.API_KEY).execute()
    }
}
