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
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import org.json.JSONObject
import java.util.*

class StockDetailsActivity : AppCompatActivity() {

    private val viewModel: StockDetailsViewModel by viewModels()
    
    private lateinit var symbol: String
    private lateinit var description: String
    
    private lateinit var favoriteStarButton: ImageButton
    private lateinit var priceAlertButton: ImageButton
    private lateinit var tvOwnedShares: TextView
    private lateinit var sellButton: MaterialButton
    private lateinit var stockLogo: ImageView
    private lateinit var newsRecyclerView: RecyclerView
    private lateinit var lineChart: LineChart
    private lateinit var priceTextView: TextView
    private lateinit var chipIndustry: Chip
    
    private var currentPrice: Double = 0.0
    private var webSocket: WebSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_details)

        symbol = intent.getStringExtra("symbol") ?: ""
        description = intent.getStringExtra("description") ?: ""

        if (symbol.isEmpty()) {
            finish()
            return
        }

        initViews()
        setupObservers()
        
        viewModel.observeStockStatus(symbol)
        viewModel.fetchData(symbol)
        startWebSocket()
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.stockDetailsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = symbol
        toolbar.setNavigationOnClickListener { finish() }

        favoriteStarButton = findViewById(R.id.favoriteStarButton)
        priceAlertButton = findViewById(R.id.priceAlertButton)
        tvOwnedShares = findViewById(R.id.tvOwnedShares)
        sellButton = findViewById(R.id.sellStockButton)
        stockLogo = findViewById(R.id.ivStockLogo)
        newsRecyclerView = findViewById(R.id.rvStockNews)
        lineChart = findViewById(R.id.stockChart)
        priceTextView = findViewById(R.id.detailsPrice)
        chipIndustry = findViewById(R.id.chipIndustry)

        findViewById<TextView>(R.id.detailsSymbol).text = symbol
        findViewById<TextView>(R.id.detailsDescription).text = description
        
        newsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        findViewById<MaterialButton>(R.id.buyStockButton).setOnClickListener { showTradeDialog(true) }
        sellButton.setOnClickListener { showTradeDialog(false) }
        favoriteStarButton.setOnClickListener { viewModel.toggleFavorite(symbol, description) }
    }

    private fun setupObservers() {
        viewModel.stockStatus.observe(this) { status ->
            val isFavorite = status?.isFavorite ?: false
            val ownedQty = status?.quantity ?: 0.0
            
            favoriteStarButton.setImageResource(
                if (isFavorite) android.R.drawable.star_big_on else android.R.drawable.star_big_off
            )
            
            if (ownedQty > 0) {
                tvOwnedShares.visibility = View.VISIBLE
                tvOwnedShares.text = "You own: ${String.format("%.2f", ownedQty)} shares"
                sellButton.visibility = View.VISIBLE
            } else {
                tvOwnedShares.visibility = View.GONE
                sellButton.visibility = View.GONE
            }
        }

        viewModel.quote.observe(this) { quote ->
            quote?.let {
                currentPrice = it.currentPrice
                updatePriceUI(it)
            }
        }

        viewModel.companyProfile.observe(this) { profile ->
            profile?.let {
                if (!it.logo.isNullOrEmpty()) Picasso.get().load(it.logo).into(stockLogo)
                if (!it.industry.isNullOrEmpty()) {
                    chipIndustry.text = it.industry
                    chipIndustry.visibility = View.VISIBLE
                }
            }
        }

        viewModel.news.observe(this) { news ->
            newsRecyclerView.adapter = NewsAdapter(news.take(5))
        }

        viewModel.tradeStatus.observe(this) { status ->
            if (status == "SUCCESS") Toast.makeText(this, "Trade Successful!", Toast.LENGTH_SHORT).show()
            else if (status.isNotEmpty()) Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePriceUI(quote: StockQuote) {
        priceTextView.text = "$${String.format("%.2f", quote.currentPrice)}"
        val changeView = findViewById<TextView>(R.id.detailsChange)
        changeView.text = "${String.format("%.2f", quote.percentChange)}%"
        changeView.setTextColor(if (quote.percentChange >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
    }

    private fun showTradeDialog(isBuy: Boolean) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_buy_stock, null)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvTotalCost)

        AlertDialog.Builder(this)
            .setTitle(if (isBuy) "Buy $symbol" else "Sell $symbol")
            .setView(dialogView)
            .setPositiveButton("Confirm") { _, _ ->
                val qty = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                if (qty > 0) viewModel.executeTrade(symbol, description, qty, currentPrice, isBuy)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startWebSocket() {
        val request = Request.Builder().url("wss://ws.finnhub.io?token=${FinnhubApiClient.API_KEY}").build()
        webSocket = OkHttpClient().newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    if (json.getString("type") == "trade") {
                        val data = json.getJSONArray("data")
                        val lastPrice = data.getJSONObject(data.length() - 1).getDouble("p")
                        runOnUiThread { 
                            currentPrice = lastPrice
                            priceTextView.text = "$${String.format("%.2f", lastPrice)}" 
                        }
                    }
                } catch (e: Exception) {}
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Closed")
    }
}
