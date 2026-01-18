package com.example.marketwatch

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.squareup.picasso.Picasso
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
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
    private lateinit var priceIlsTextView: TextView
    private lateinit var chipIndustry: Chip
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var contentView: NestedScrollView
    
    private var currentPriceUsd: Double = 0.0
    private var webSocket: WebSocket? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) showPriceAlertDialog()
        else Toast.makeText(this, "Notifications permission is required for price alerts", Toast.LENGTH_LONG).show()
    }

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

        shimmerLayout = findViewById(R.id.stockDetailsShimmer)
        contentView = findViewById(R.id.stockDetailsContent)
        favoriteStarButton = findViewById(R.id.favoriteStarButton)
        priceAlertButton = findViewById(R.id.priceAlertButton)
        tvOwnedShares = findViewById(R.id.tvOwnedShares)
        sellButton = findViewById(R.id.sellStockButton)
        stockLogo = findViewById(R.id.ivStockLogo)
        newsRecyclerView = findViewById(R.id.rvStockNews)
        lineChart = findViewById(R.id.stockChart)
        priceTextView = findViewById(R.id.detailsPrice)
        priceIlsTextView = findViewById(R.id.detailsPriceIls)
        chipIndustry = findViewById(R.id.chipIndustry)

        findViewById<TextView>(R.id.detailsSymbol).text = symbol
        findViewById<TextView>(R.id.detailsDescription).text = description
        
        newsRecyclerView.layoutManager = LinearLayoutManager(this)
        newsRecyclerView.adapter = NewsAdapter(emptyList())
        
        findViewById<MaterialButton>(R.id.buyStockButton).setOnClickListener { showTradeDialog(true) }
        sellButton.setOnClickListener { showTradeDialog(false) }
        favoriteStarButton.setOnClickListener { viewModel.toggleFavorite(symbol, description) }
        priceAlertButton.setOnClickListener { checkNotificationPermissionAndShowDialog() }

        setupChartStyle()
        shimmerLayout.startShimmer()
    }

    private fun setupChartStyle() {
        lineChart.apply {
            description.isEnabled = false
            setNoDataText("Loading chart data...")
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            xAxis.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.textColor = Color.GRAY
            legend.isEnabled = false
        }
    }

    private fun setupObservers() {
        viewModel.stockStatus.observe(this) { status ->
            val isFavorite = status?.isFavorite ?: false
            val ownedQty = status?.quantity ?: 0.0
            favoriteStarButton.setImageResource(if (isFavorite) android.R.drawable.star_big_on else android.R.drawable.star_big_off)
            
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
                currentPriceUsd = it.currentPrice
                updatePriceUI()
                updateChangeUI(it)
                updateStatsUI(it)
                hideShimmer()
            }
        }

        viewModel.exchangeRate.observe(this) { _ -> updatePriceUI() }

        viewModel.candles.observe(this) { candles ->
            if (candles != null && candles.status == "ok" && !candles.closePrices.isNullOrEmpty()) {
                updateChartData(candles.closePrices)
            } else {
                lineChart.setNoDataText("No chart data available")
                lineChart.invalidate()
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
            if (news.isNotEmpty()) {
                newsRecyclerView.adapter = NewsAdapter(news.take(5))
            }
        }

        viewModel.tradeStatus.observe(this) { status ->
            when (status) {
                "SUCCESS" -> Toast.makeText(this, "Trade Successful!", Toast.LENGTH_SHORT).show()
                "ALERT_SET" -> Toast.makeText(this, "Price alert set!", Toast.LENGTH_SHORT).show()
                "ADDED_TO_FAVORITES" -> Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
                "REMOVED_FROM_FAVORITES" -> Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                else -> if (status.isNotEmpty()) Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideShimmer() {
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE
        contentView.visibility = View.VISIBLE
    }

    private fun updateChartData(prices: List<Double>) {
        val entries = prices.mapIndexed { index, price -> Entry(index.toFloat(), price.toFloat()) }
        val dataSet = LineDataSet(entries, symbol).apply {
            color = Color.parseColor("#4CAF50")
            setDrawCircles(false)
            lineWidth = 2f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#4CAF50")
            fillAlpha = 30
        }
        lineChart.data = LineData(dataSet)
        lineChart.invalidate()
    }

    private fun updateStatsUI(quote: StockQuote) {
        findViewById<TextView>(R.id.detailsHigh).text = String.format("%.2f", quote.highPrice)
        findViewById<TextView>(R.id.detailsLow).text = String.format("%.2f", quote.lowPrice)
        findViewById<TextView>(R.id.detailsPrevClose).text = String.format("%.2f", quote.previousClose)
    }

    private fun updatePriceUI() {
        priceTextView.text = "$${String.format("%.2f", currentPriceUsd)}"
        val rate = viewModel.exchangeRate.value ?: 3.7
        val priceIls = currentPriceUsd * rate
        priceIlsTextView.text = "≈ ₪${String.format("%.2f", priceIls)}"
        priceIlsTextView.visibility = View.VISIBLE
    }

    private fun updateChangeUI(quote: StockQuote) {
        val changeView = findViewById<TextView>(R.id.detailsChange)
        changeView.text = "${String.format("%.2f", quote.percentChange)}%"
        changeView.setTextColor(if (quote.percentChange >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
    }

    private fun showTradeDialog(isBuy: Boolean) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_buy_stock, null)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)

        AlertDialog.Builder(this)
            .setTitle(if (isBuy) "Buy $symbol" else "Sell $symbol")
            .setView(dialogView)
            .setPositiveButton("Confirm") { _, _ ->
                val qty = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                if (qty > 0) viewModel.executeTrade(symbol, description, qty, currentPriceUsd, isBuy)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkNotificationPermissionAndShowDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> showPriceAlertDialog()
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else showPriceAlertDialog()
    }

    private fun showPriceAlertDialog() {
        val etPrice = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Enter target price in USD"
        }

        AlertDialog.Builder(this)
            .setTitle("Set Price Alert for $symbol")
            .setMessage("Get notified when the price reaches your target.")
            .setView(etPrice)
            .setPositiveButton("Set Alert") { _, _ ->
                val targetPrice = etPrice.text.toString().toDoubleOrNull()
                if (targetPrice != null && targetPrice > 0) viewModel.setPriceAlert(symbol, description, targetPrice)
                else Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show()
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
                            currentPriceUsd = lastPrice
                            updatePriceUI()
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
