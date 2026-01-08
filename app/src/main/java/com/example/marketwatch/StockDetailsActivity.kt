package com.example.marketwatch

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StockDetailsActivity : AppCompatActivity() {

    private lateinit var symbol: String
    private lateinit var description: String
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var favoriteStarButton: ImageButton
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_details)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        symbol = intent.getStringExtra("symbol") ?: ""
        description = intent.getStringExtra("description") ?: ""

        favoriteStarButton = findViewById(R.id.favoriteStarButton)
        val toolbar = findViewById<Toolbar>(R.id.stockDetailsToolbar)
        
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = symbol
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.detailsSymbol).text = symbol
        findViewById<TextView>(R.id.detailsDescription).text = description

        checkIfInWatchlist()
        fetchStockDetails()

        favoriteStarButton.setOnClickListener {
            if (!isFavorite) {
                addToWatchlist()
            } else {
                removeFromWatchlist()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun checkIfInWatchlist() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        
        db.collection("users").document(userId)
            .collection("watchlist").document(symbol)
            .get()
            .addOnSuccessListener { document ->
                setFavoriteState(document != null && document.exists())
            }
            .addOnFailureListener { e ->
                Log.e("StockDetails", "Error checking watchlist", e)
            }
    }

    private fun setFavoriteState(favorite: Boolean) {
        isFavorite = favorite
        runOnUiThread {
            if (favorite) {
                favoriteStarButton.setImageResource(android.R.drawable.star_big_on)
            } else {
                favoriteStarButton.setImageResource(android.R.drawable.star_big_off)
            }
        }
    }

    private fun fetchStockDetails() {
        FinnhubApiClient.apiService.getQuote(symbol, FinnhubApiClient.API_KEY)
            .enqueue(object : Callback<StockQuote> {
                override fun onResponse(call: Call<StockQuote>, response: Response<StockQuote>) {
                    if (response.isSuccessful) {
                        val quote = response.body()
                        if (quote != null) {
                            updateUI(quote)
                        }
                    }
                }

                override fun onFailure(call: Call<StockQuote>, t: Throwable) {
                    Toast.makeText(this@StockDetailsActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUI(quote: StockQuote) {
        runOnUiThread {
            findViewById<TextView>(R.id.detailsPrice).text = "$${String.format("%.2f", quote.currentPrice)}"
            
            val changeText = "${String.format("%.2f", quote.change)} (${String.format("%.2f", quote.percentChange)}%)"
            val changeView = findViewById<TextView>(R.id.detailsChange)
            changeView.text = changeText
            
            if (quote.change >= 0) {
                changeView.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                changeView.setTextColor(Color.parseColor("#F44336"))
            }

            findViewById<TextView>(R.id.detailsHigh).text = "$${quote.highPrice}"
            findViewById<TextView>(R.id.detailsLow).text = "$${quote.lowPrice}"
            findViewById<TextView>(R.id.detailsOpen).text = "$${quote.openPrice}"
            findViewById<TextView>(R.id.detailsPrevClose).text = "$${quote.previousClose}"
        }
    }

    private fun addToWatchlist() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        
        val stockData = hashMapOf(
            "symbol" to symbol,
            "description" to description,
            "addedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(userId)
            .collection("watchlist").document(symbol)
            .set(stockData)
            .addOnSuccessListener {
                setFavoriteState(true)
                Toast.makeText(this, "$symbol added to favorites", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeFromWatchlist() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        
        db.collection("users").document(userId)
            .collection("watchlist").document(symbol)
            .delete()
            .addOnSuccessListener {
                setFavoriteState(false)
                Toast.makeText(this, "$symbol removed from favorites", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
