package com.example.marketwatch.data

import com.example.marketwatch.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
                        isFavorite = snapshot.getBoolean("isFavorite") ?: false,
                        totalCost = snapshot.getDouble("totalCost") ?: 0.0
                    ))
                } else {
                    trySend(null)
                }
            }
        awaitClose { registration.remove() }
    }

    // Firebase: Toggle Favorite
    suspend fun toggleFavorite(symbol: String, description: String, currentState: Boolean, ownedQty: Double) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        val newState = !currentState
        val docRef = db.collection("users").document(userId).collection("watchlist").document(symbol)

        if (!newState && ownedQty <= 0) {
            val snapshot = docRef.get().await()
            val targetPrice = snapshot.getDouble("targetPrice") ?: 0.0
            
            if (targetPrice > 0) {
                docRef.update("isFavorite", false).await()
            } else {
                docRef.delete().await()
            }
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
    suspend fun executeTrade(symbol: String, description: String, quantity: Double, price: Double, isBuy: Boolean, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        val userRef = db.collection("users").document(userId)
        val tradeAmount = quantity * price

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val balance = userSnapshot.getDouble("balance") ?: 0.0
            
            val watchlistRef = userRef.collection("watchlist").document(symbol)
            val stockSnapshot = transaction.get(watchlistRef)
            val currentQty = stockSnapshot.getDouble("quantity") ?: 0.0
            val currentTotalCost = stockSnapshot.getDouble("totalCost") ?: 0.0
            val targetPrice = stockSnapshot.getDouble("targetPrice") ?: 0.0

            if (isBuy) {
                if (balance < tradeAmount) throw Exception("Insufficient funds")
                transaction.update(userRef, "balance", balance - tradeAmount)
                transaction.set(watchlistRef, hashMapOf(
                    "symbol" to symbol,
                    "description" to description,
                    "quantity" to currentQty + quantity,
                    "totalCost" to currentTotalCost + tradeAmount,
                    "isFavorite" to isFavorite,
                    "lastTradeAt" to Timestamp.now()
                ), SetOptions.merge())
            } else {
                if (currentQty < quantity) throw Exception("Insufficient shares")
                transaction.update(userRef, "balance", balance + tradeAmount)
                val newQty = currentQty - quantity
                
                if (newQty <= 0) {
                    if (!isFavorite && targetPrice <= 0) transaction.delete(watchlistRef)
                    else {
                        transaction.update(watchlistRef, "quantity", 0.0)
                        transaction.update(watchlistRef, "totalCost", 0.0)
                    }
                } else {
                    val costPerShare = currentTotalCost / currentQty
                    val newTotalCost = currentTotalCost - (quantity * costPerShare)
                    transaction.update(watchlistRef, "quantity", newQty)
                    transaction.update(watchlistRef, "totalCost", newTotalCost)
                }
            }

            val transRef = userRef.collection("transactions").document()
            transaction.set(transRef, hashMapOf(
                "type" to if (isBuy) "BUY" else "SELL",
                "symbol" to symbol,
                "amount" to tradeAmount,
                "quantity" to quantity,
                "timestamp" to Timestamp.now()
            ))
        }.await()
    }

    // Firebase: Set Price Alert
    suspend fun setPriceAlert(symbol: String, description: String, targetPrice: Double) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        val docRef = db.collection("users").document(userId).collection("watchlist").document(symbol)
        
        val data = hashMapOf(
            "symbol" to symbol,
            "description" to description,
            "targetPrice" to targetPrice
        )
        docRef.set(data, SetOptions.merge()).await()
    }

    // Currency Conversion
    suspend fun getUsdToIlsRate(): Double = withContext(Dispatchers.IO) {
        try {
            val response = FrankfurterApiClient.apiService.getLatestRates("USD").execute()
            if (response.isSuccessful) {
                response.body()?.rates?.get("ILS") ?: 3.7
            } else 3.7
        } catch (e: Exception) {
            3.7
        }
    }

    // Finnhub API Calls
    suspend fun getQuote(symbol: String): Response<StockQuote> = withContext(Dispatchers.IO) {
        FinnhubApiClient.apiService.getQuote(symbol, FinnhubApiClient.API_KEY).execute()
    }

    suspend fun getCompanyProfile(symbol: String): Response<CompanyProfile> = withContext(Dispatchers.IO) {
        FinnhubApiClient.apiService.getCompanyProfile(symbol, FinnhubApiClient.API_KEY).execute()
    }

    suspend fun getNews(symbol: String): Response<List<StockNews>> = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val to = sdf.format(Date())
        val from = sdf.format(Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L))
        FinnhubApiClient.apiService.getStockNews(symbol, from, to, FinnhubApiClient.API_KEY).execute()
    }

    suspend fun getCandles(symbol: String): Response<StockCandles> = withContext(Dispatchers.IO) {
        val to = System.currentTimeMillis() / 1000
        val from = to - (30 * 24 * 60 * 60)
        FinnhubApiClient.apiService.getStockCandles(symbol, "D", from, to, FinnhubApiClient.API_KEY).execute()
    }
}
