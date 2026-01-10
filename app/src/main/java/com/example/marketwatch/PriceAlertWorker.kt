package com.example.marketwatch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PriceAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.success()

        try {
            // 1. שליפת רשימת המעקב של המשתמש
            val watchlist = db.collection("users").document(userId)
                .collection("watchlist")
                .get().await()

            for (doc in watchlist) {
                val symbol = doc.getString("symbol") ?: continue
                val targetPrice = doc.getDouble("targetPrice") ?: 0.0
                
                if (targetPrice > 0) {
                    // 2. בדיקת מחיר נוכחי מה-API (קריאה סינכרונית בתוך קורוטינה)
                    val response = FinnhubApiClient.apiService.getQuote(symbol, FinnhubApiClient.API_KEY).execute()
                    if (response.isSuccessful) {
                        val currentPrice = response.body()?.currentPrice ?: 0.0
                        
                        // 3. אם המחיר עבר את היעד - שלח התראה
                        if (currentPrice >= targetPrice) {
                            sendNotification(symbol, currentPrice)
                            // איפוס מחיר יעד כדי לא להספים התראות
                            db.collection("users").document(userId)
                                .collection("watchlist").document(symbol)
                                .update("targetPrice", 0.0)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PriceAlertWorker", "Error checking prices", e)
            return Result.retry()
        }

        return Result.success()
    }

    private fun sendNotification(symbol: String, price: Double) {
        val channelId = "price_alerts"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Price Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Price Alert: $symbol")
            .setContentText("$symbol has reached your target price of $$price!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(symbol.hashCode(), notification)
    }
}
