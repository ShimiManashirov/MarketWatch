package com.example.marketwatch

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import okhttp3.*
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import java.text.SimpleDateFormat
import java.util.*

class StockDetailsActivity : AppCompatActivity() {

    private lateinit var symbol: String
    private lateinit var description: String
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var favoriteStarButton: ImageButton
    private lateinit var priceAlertButton: ImageButton
    private lateinit var tvOwnedShares: TextView
    private lateinit var sellButton: MaterialButton
    private lateinit var stockLogo: ImageView
    private lateinit var newsRecyclerView: RecyclerView
    private lateinit var lineChart: LineChart
    private lateinit var priceTextView: TextView
    
    // UI components for advanced stats
    private lateinit var chipIndustry: Chip
    private lateinit var tvMarketCap: TextView
    private lateinit var tvExchange: TextView
    private lateinit var tvHigh: TextView
    private lateinit var tvLow: TextView
    private lateinit var tvPrevClose: TextView
    
    private var isFavorite = false
    private var currentPrice: Double = 0.0
    private var ownedQuantity: Double = 0.0

    // WebSocket components
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_details)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        symbol = intent.getStringExtra("symbol") ?: ""
        description = intent.getStringExtra("description") ?: ""

        if (symbol.isEmpty()) {
            Toast.makeText(this, "Error: Symbol not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize Views
        val toolbar = findViewById<Toolbar>(R.id.stockDetailsToolbar)
        favoriteStarButton = findViewById(R.id.favoriteStarButton)
        priceAlertButton = findViewById(R.id.priceAlertButton)
        tvOwnedShares = findViewById(R.id.tvOwnedShares)
        sellButton = findViewById(R.id.sellStockButton)
        stockLogo = findViewById(R.id.ivStockLogo)
        newsRecyclerView = findViewById(R.id.rvStockNews)
        lineChart = findViewById(R.id.stockChart)
        priceTextView = findViewById(R.id.detailsPrice)
        val buyButton = findViewById<MaterialButton>(R.id.buyStockButton)
        
        chipIndustry = findViewById(R.id.chipIndustry)
        tvMarketCap = findViewById(R.id.detailsMarketCap)
        tvExchange = findViewById(R.id.detailsExchange)
        tvHigh = findViewById(R.id.detailsHigh)
        tvLow = findViewById(R.id.detailsLow)
        tvPrevClose = findViewById(R.id.detailsPrevClose)
        
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = symbol
        }

        toolbar.setNavigationOnClickListener { finish() }

        findViewById<TextView>(R.id.detailsSymbol).text = symbol
        findViewById<TextView>(R.id.detailsDescription).text = description

        newsRecyclerView.layoutManager = LinearLayoutManager(this)

        setupChart()
        checkWatchlistAndOwnership()
        fetchStockDetails()
        fetchCompanyProfile()
        fetchStockNews()
        fetchChartDataFromAlphaVantage()
        startWebSocket()

        favoriteStarButton.setOnClickListener { toggleFavorite() }
        priceAlertButton.setOnClickListener { showPriceAlertDialog() }
        buyButton.setOnClickListener { showTradeDialog(true) }
        sellButton.setOnClickListener { showTradeDialog(false) }
    }

    private fun startWebSocket() {
        val request = Request.Builder()
            .url("wss://ws.finnhub.io?token=${FinnhubApiClient.API_KEY}")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                val subscribeMsg = JSONObject().apply {
                    put("type", "subscribe")
                    put("symbol", symbol)
                }
                webSocket.send(subscribeMsg.toString())
                Log.d("WebSocket", "Subscribed to $symbol")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleWebSocketMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e("WebSocket", "Connection failure", t)
            }
        })
    }

    private fun handleWebSocketMessage(text: String) {
        try {
            val json = JSONObject(text)
            if (json.getString("type") == "trade") {
                val data = json.getJSONArray("data")
                val lastTrade = data.getJSONObject(data.length() - 1)
                val newPrice = lastTrade.getDouble("p")
                
                runOnUiThread {
                    updatePriceWithAnimation(newPrice)
                }
            }
        } catch (e: Exception) {
            // Ignore keep-alive or malformed messages
        }
    }

    private fun updatePriceWithAnimation(newPrice: Double) {
        if (newPrice == currentPrice) return

        val oldPrice = currentPrice
        currentPrice = newPrice
        
        priceTextView.text = "$${String.format("%.2f", newPrice)}"
        
        if (newPrice > oldPrice && oldPrice != 0.0) {
            priceTextView.setTextColor(Color.parseColor("#4CAF50"))
        } else if (newPrice < oldPrice && oldPrice != 0.0) {
            priceTextView.setTextColor(Color.parseColor("#F44336"))
        }

        priceTextView.postDelayed({
            if (isFinishing) return@postDelayed
            priceTextView.setTextColor(Color.parseColor("#1A1C1E"))
        }, 600)
    }

    private fun showPriceAlertDialog() {
        val input = EditText(this).apply {
            hint = "Enter target price"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(if (currentPrice > 0) String.format("%.2f", currentPrice) else "")
        }

        AlertDialog.Builder(this)
            .setTitle("Set Price Alert for $symbol")
            .setMessage("We will notify you when $symbol reaches this price.")
            .setView(input)
            .setPositiveButton("Set Alert") { _, _ ->
                val targetPrice = input.text.toString().toDoubleOrNull() ?: 0.0
                if (targetPrice > 0) {
                    savePriceAlert(targetPrice)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun savePriceAlert(targetPrice: Double) {
        val user = auth.currentUser ?: return
        val data = hashMapOf(
            "symbol" to symbol,
            "description" to description,
            "targetPrice" to targetPrice,
            "isFavorite" to isFavorite 
        )
        
        db.collection("users").document(user.uid)
            .collection("watchlist").document(symbol)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Alert set for $$targetPrice", Toast.LENGTH_SHORT).show()
                priceAlertButton.setColorFilter(Color.parseColor("#0D6EFD"))
            }
    }

    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            setNoDataText("Loading chart...")
            setNoDataTextColor(Color.GRAY)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            xAxis.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.apply {
                textColor = Color.GRAY
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A000000")
            }
            legend.isEnabled = false
        }
    }

    private fun fetchChartDataFromAlphaVantage() {
        AlphaVantageApiClient.apiService.getDailySeries(symbol = symbol, apiKey = AlphaVantageApiClient.API_KEY)
            .enqueue(object : Callback<AlphaVantageResponse> {
                override fun onResponse(call: Call<AlphaVantageResponse>, response: retrofit2.Response<AlphaVantageResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val timeSeries = body?.timeSeries
                        
                        if (!timeSeries.isNullOrEmpty()) {
                            val entries = mutableListOf<Entry>()
                            val sortedDates = timeSeries.keys.sorted()
                            
                            sortedDates.takeLast(30).forEachIndexed { index, date ->
                                val closePrice = timeSeries[date]?.close?.toFloat() ?: 0f
                                entries.add(Entry(index.toFloat(), closePrice))
                            }
                            
                            runOnUiThread { displayChart(entries) }
                        } else {
                            runOnUiThread {
                                lineChart.setNoDataText("Historical data currently unavailable")
                                lineChart.invalidate()
                            }
                        }
                    }
                }
                override fun onFailure(call: Call<AlphaVantageResponse>, t: Throwable) {
                    Log.e("StockDetails", "Network Error", t)
                }
            })
    }

    private fun displayChart(entries: List<Entry>) {
        val dataSet = LineDataSet(entries, "Price").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawCircles(false)
            setDrawFilled(true)
            lineWidth = 3f
            color = Color.parseColor("#0D6EFD")
            fillColor = Color.parseColor("#0D6EFD")
            fillAlpha = 40
            setDrawValues(false)
        }

        lineChart.data = LineData(dataSet)
        lineChart.animateX(800)
        lineChart.invalidate()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun checkWatchlistAndOwnership() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .collection("watchlist").document(symbol)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    isFavorite = snapshot.getBoolean("isFavorite") ?: false
                    ownedQuantity = snapshot.getDouble("quantity") ?: 0.0
                    val hasAlert = (snapshot.getDouble("targetPrice") ?: 0.0) > 0
                    priceAlertButton.setColorFilter(if (hasAlert) Color.parseColor("#0D6EFD") else Color.GRAY)
                } else {
                    isFavorite = false
                    ownedQuantity = 0.0
                }
                setFavoriteState(isFavorite)
                updateOwnershipUI()
            }
    }

    private fun updateOwnershipUI() {
        runOnUiThread {
            if (ownedQuantity > 0) {
                tvOwnedShares.visibility = View.VISIBLE
                tvOwnedShares.text = "You own: ${String.format("%.2f", ownedQuantity)} shares"
                sellButton.visibility = View.VISIBLE
            } else {
                tvOwnedShares.visibility = View.GONE
                sellButton.visibility = View.GONE
            }
        }
    }

    private fun setFavoriteState(favorite: Boolean) {
        isFavorite = favorite
        runOnUiThread {
            favoriteStarButton.setImageResource(
                if (favorite) android.R.drawable.star_big_on else android.R.drawable.star_big_off
            )
        }
    }

    private fun toggleFavorite() {
        val user = auth.currentUser ?: return
        val newState = !isFavorite
        
        if (!newState && ownedQuantity <= 0) {
            db.collection("users").document(user.uid)
                .collection("watchlist").document(symbol)
                .delete()
                .addOnSuccessListener {
                    setFavoriteState(false)
                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                }
        } else {
            db.collection("users").document(user.uid)
                .collection("watchlist").document(symbol)
                .set(hashMapOf("isFavorite" to newState, "symbol" to symbol, "description" to description), SetOptions.merge())
                .addOnSuccessListener {
                    setFavoriteState(newState)
                    Toast.makeText(this, if (newState) "Added to favorites" else "Removed from favorites", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun fetchStockDetails() {
        FinnhubApiClient.apiService.getQuote(symbol, FinnhubApiClient.API_KEY)
            .enqueue(object : Callback<StockQuote> {
                override fun onResponse(call: Call<StockQuote>, response: retrofit2.Response<StockQuote>) {
                    if (response.isSuccessful) {
                        response.body()?.let { 
                            currentPrice = it.currentPrice
                            updateUI(it) 
                        }
                    }
                }
                override fun onFailure(call: Call<StockQuote>, t: Throwable) {
                    Log.e("StockDetails", "API Error", t)
                }
            })
    }

    private fun fetchCompanyProfile() {
        FinnhubApiClient.apiService.getCompanyProfile(symbol, FinnhubApiClient.API_KEY)
            .enqueue(object : Callback<CompanyProfile> {
                override fun onResponse(call: Call<CompanyProfile>, response: retrofit2.Response<CompanyProfile>) {
                    if (response.isSuccessful) {
                        response.body()?.let { profile ->
                            runOnUiThread {
                                Glide.with(this@StockDetailsActivity)
                                    .load(profile.logo)
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .into(stockLogo)
                                
                                if (!profile.industry.isNullOrEmpty()) {
                                    chipIndustry.text = profile.industry
                                    chipIndustry.visibility = View.VISIBLE
                                }
                                
                                profile.marketCap?.let {
                                    tvMarketCap.text = formatMarketCap(it)
                                }
                            }
                        }
                    }
                }
                override fun onFailure(call: Call<CompanyProfile>, t: Throwable) {}
            })
    }

    private fun formatMarketCap(cap: Double): String {
        return when {
            cap >= 1000000 -> String.format("%.2fT", cap / 1000000.0)
            cap >= 1000 -> String.format("%.2fB", cap / 1000.0)
            else -> String.format("%.2fM", cap)
        }
    }

    private fun fetchStockNews() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val toDate = sdf.format(Date())
        val fromDate = sdf.format(Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L))

        FinnhubApiClient.apiService.getStockNews(symbol, fromDate, toDate, FinnhubApiClient.API_KEY)
            .enqueue(object : Callback<List<StockNews>> {
                override fun onResponse(call: Call<List<StockNews>>, response: retrofit2.Response<List<StockNews>>) {
                    if (response.isSuccessful) {
                        val news = response.body() ?: emptyList()
                        runOnUiThread {
                            newsRecyclerView.adapter = NewsAdapter(news.take(5))
                        }
                    }
                }
                override fun onFailure(call: Call<List<StockNews>>, t: Throwable) {
                    Log.e("StockDetails", "News fetch failed", t)
                }
            })
    }

    private fun updateUI(quote: StockQuote) {
        runOnUiThread {
            findViewById<TextView>(R.id.detailsPrice).text = "$${String.format("%.2f", quote.currentPrice)}"
            val changeView = findViewById<TextView>(R.id.detailsChange)
            changeView.text = "${String.format("%.2f", quote.change)} (${String.format("%.2f", quote.percentChange)}%)"
            changeView.setTextColor(if (quote.change >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
            
            tvHigh.text = "$${quote.highPrice}"
            tvLow.text = "$${quote.lowPrice}"
            tvPrevClose.text = "$${quote.previousClose}"
        }
    }

    private fun showTradeDialog(isBuy: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_buy_stock, null)
        
        val tvTitle = dialogView.findViewById<TextView>(R.id.dialogBuySymbol)
        val tvPrice = dialogView.findViewById<TextView>(R.id.dialogCurrentPrice)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvTotalCost)
        val tvBalance = dialogView.findViewById<TextView>(R.id.tvWalletBalance)

        tvTitle.text = if (isBuy) "Buy $symbol" else "Sell $symbol"
        tvPrice.text = "Market Price: $${String.format("%.2f", currentPrice)}"
        
        if (isBuy) {
            db.collection("users").document(userId).get().addOnSuccessListener { doc ->
                val balance = doc.getDouble("balance") ?: 0.0
                tvBalance.text = "Wallet: $${String.format("%.2f", balance)}"
            }
        } else {
            tvBalance.text = "Available: ${String.format("%.2f", ownedQuantity)} shares"
        }

        etQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val qty = s.toString().toDoubleOrNull() ?: 0.0
                tvTotal.text = "$${String.format("%.2f", qty * currentPrice)}"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        AlertDialog.Builder(this)
            .setTitle(if (isBuy) "Confirm Trade" else "Confirm Sale")
            .setView(dialogView)
            .setPositiveButton(if (isBuy) "Buy" else "Sell") { _, _ ->
                val qty = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                if (qty > 0) {
                    if (isBuy) executeTrade(qty, true) 
                    else if (qty <= ownedQuantity) executeTrade(qty, false)
                    else Toast.makeText(this, "Not enough shares", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeTrade(quantity: Double, isBuy: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)
        val totalAmount = quantity * currentPrice

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentBalance = snapshot.getDouble("balance") ?: 0.0
            
            val watchlistRef = userRef.collection("watchlist").document(symbol)
            val currentStockDoc = transaction.get(watchlistRef)
            val currentStockQty = currentStockDoc.getDouble("quantity") ?: 0.0

            if (isBuy) {
                if (currentBalance < totalAmount) throw Exception("Insufficient funds")
                transaction.update(userRef, "balance", currentBalance - totalAmount)
                transaction.set(watchlistRef, hashMapOf(
                    "symbol" to symbol,
                    "description" to description,
                    "quantity" to currentStockQty + quantity,
                    "isFavorite" to isFavorite,
                    "lastTradeAt" to Timestamp.now()
                ), SetOptions.merge())
            } else {
                if (currentStockQty < quantity) throw Exception("Insufficient shares")
                transaction.update(userRef, "balance", currentBalance + totalAmount)
                val newQty = currentStockQty - quantity
                
                if (newQty <= 0 && !isFavorite) {
                    transaction.delete(watchlistRef)
                } else {
                    transaction.update(watchlistRef, "quantity", newQty)
                }
            }

            val transactionRef = userRef.collection("transactions").document()
            transaction.set(transactionRef, hashMapOf(
                "type" to if (isBuy) "BUY" else "SELL",
                "symbol" to symbol,
                "amount" to totalAmount,
                "quantity" to quantity,
                "timestamp" to Timestamp.now()
            ))

        }.addOnSuccessListener {
            Toast.makeText(this, "Trade successful!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Activity destroyed")
    }
}
