package com.example.marketwatch

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.marketwatch.data.NewsRepository
import com.example.marketwatch.data.local.AppDatabase
import com.facebook.shimmer.ShimmerFrameLayout
import com.squareup.picasso.Picasso
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import org.json.JSONObject
import java.util.*

class StockDetailsFragment : Fragment() {

    private val viewModel: StockDetailsViewModel by viewModels()
    private val portfolioViewModel: PortfolioViewModel by viewModels()
    private lateinit var newsViewModel: NewsViewModel
    private val args: StockDetailsFragmentArgs by navArgs()
    
    private lateinit var symbol: String
    private lateinit var description: String
    
    private lateinit var favoriteStarButton: ImageButton
    private lateinit var priceAlertButton: ImageButton
    private lateinit var tvOwnedShares: TextView
    private lateinit var sellButton: MaterialButton
    private lateinit var stockLogo: ImageView
    private lateinit var newsRecyclerView: RecyclerView
    private lateinit var newsAdapter: NewsAdapter
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
        else Toast.makeText(requireContext(), "Notifications permission is required for price alerts", Toast.LENGTH_LONG).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_stock_details, container, false)

        symbol = args.symbol
        description = args.description

        val newsRepository = NewsRepository(AppDatabase.getDatabase(requireContext()))
        val newsFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return NewsViewModel(newsRepository) as T
            }
        }
        newsViewModel = ViewModelProvider(this, newsFactory).get(NewsViewModel::class.java)

        initViews(view)
        setupObservers()
        
        viewModel.observeStockStatus(symbol)
        viewModel.fetchData(symbol)
        newsViewModel.fetchNewsForSymbol(symbol)
        startWebSocket()

        return view
    }

    private fun initViews(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.stockDetailsToolbar)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        toolbar.title = symbol

        shimmerLayout = view.findViewById(R.id.stockDetailsShimmer)
        contentView = view.findViewById(R.id.stockDetailsContent)
        favoriteStarButton = view.findViewById(R.id.favoriteStarButton)
        priceAlertButton = view.findViewById(R.id.priceAlertButton)
        tvOwnedShares = view.findViewById(R.id.tvOwnedShares)
        sellButton = view.findViewById(R.id.sellStockButton)
        stockLogo = view.findViewById(R.id.ivStockLogo)
        newsRecyclerView = view.findViewById(R.id.rvStockNews)
        lineChart = view.findViewById(R.id.stockChart)
        priceTextView = view.findViewById(R.id.detailsPrice)
        priceIlsTextView = view.findViewById(R.id.detailsPriceIls)
        chipIndustry = view.findViewById(R.id.chipIndustry)

        view.findViewById<TextView>(R.id.detailsSymbol).text = symbol
        view.findViewById<TextView>(R.id.detailsDescription).text = description
        
        newsRecyclerView.layoutManager = LinearLayoutManager(context)
        newsAdapter = NewsAdapter(emptyList()) { news, isBookmarked ->
            newsViewModel.toggleBookmark(news, isBookmarked)
        }
        newsRecyclerView.adapter = newsAdapter
        
        view.findViewById<MaterialButton>(R.id.buyStockButton).setOnClickListener { showTradeDialog(true) }
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
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) hideShimmer()
        }

        viewModel.stockStatus.observe(viewLifecycleOwner) { status ->
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

        viewModel.quote.observe(viewLifecycleOwner) { quote ->
            quote?.let {
                currentPriceUsd = it.currentPrice
                updatePriceUI()
                updateChangeUI(it)
                updateStatsUI(it)
            }
        }

        viewModel.exchangeRate.observe(viewLifecycleOwner) { _ -> updatePriceUI() }

        viewModel.candles.observe(viewLifecycleOwner) { candles ->
            if (candles != null && candles.status == "ok" && !candles.closePrices.isNullOrEmpty()) {
                updateChartData(candles.closePrices)
            } else {
                lineChart.setNoDataText("Historical data unavailable")
                lineChart.invalidate()
            }
        }

        viewModel.companyProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                if (!it.logo.isNullOrEmpty()) Picasso.get().load(it.logo).into(stockLogo)
                if (!it.industry.isNullOrEmpty()) {
                    chipIndustry.text = it.industry
                    chipIndustry.visibility = View.VISIBLE
                }
            }
        }

        newsViewModel.newsList.observe(viewLifecycleOwner) { news ->
            if (news.isNotEmpty()) {
                newsAdapter.updateNews(news.take(10))
            }
        }

        viewModel.tradeStatus.observe(viewLifecycleOwner) { status ->
            if (status.isEmpty()) return@observe
            val message = when (status) {
                "SUCCESS" -> "Trade Successful!"
                "ALERT_SET" -> "Price alert set!"
                "ADDED_TO_FAVORITES" -> "Added to favorites"
                "REMOVED_FROM_FAVORITES" -> "Removed from favorites"
                "ERROR_FETCHING_DATA" -> "Failed to load complete stock data"
                else -> status
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideShimmer() {
        if (shimmerLayout.visibility == View.VISIBLE) {
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            contentView.visibility = View.VISIBLE
        }
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
        view?.findViewById<TextView>(R.id.detailsHigh)?.text = String.format("%.2f", quote.highPrice)
        view?.findViewById<TextView>(R.id.detailsLow)?.text = String.format("%.2f", quote.lowPrice)
        view?.findViewById<TextView>(R.id.detailsPrevClose)?.text = String.format("%.2f", quote.previousClose)
    }

    private fun updatePriceUI() {
        priceTextView.text = "$${String.format("%.2f", currentPriceUsd)}"
        val rate = viewModel.exchangeRate.value ?: 3.7
        val priceIls = currentPriceUsd * rate
        priceIlsTextView.text = "≈ ₪${String.format("%.2f", priceIls)}"
        priceIlsTextView.visibility = View.VISIBLE
    }

    private fun updateChangeUI(quote: StockQuote) {
        val changeView = view?.findViewById<TextView>(R.id.detailsChange)
        changeView?.text = "${String.format("%.2f", quote.percentChange)}%"
        changeView?.setTextColor(if (quote.percentChange >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
    }

    private fun showTradeDialog(isBuy: Boolean) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_buy_stock, null)
        val etQuantity = dialogView.findViewById<TextInputEditText>(R.id.etQuantity)
        val tvDialogSymbol = dialogView.findViewById<TextView>(R.id.dialogBuySymbol)
        val tvCurrentPrice = dialogView.findViewById<TextView>(R.id.dialogCurrentPrice)
        val tvTotalCost = dialogView.findViewById<TextView>(R.id.tvTotalCost)
        val tvWalletBalance = dialogView.findViewById<TextView>(R.id.tvWalletBalance)

        tvDialogSymbol.text = if (isBuy) "Buy $symbol" else "Sell $symbol"
        tvCurrentPrice.text = "Market Price: $${String.format("%.2f", currentPriceUsd)}"
        
        portfolioViewModel.userBalance.observe(viewLifecycleOwner) { balance ->
            tvWalletBalance.text = "Wallet: $${String.format("%.2f", balance)}"
        }

        etQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val qty = s.toString().toDoubleOrNull() ?: 0.0
                tvTotalCost.text = "$${String.format("%.2f", qty * currentPriceUsd)}"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Confirm") { _, _ ->
                val qty = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                if (qty > 0) viewModel.executeTrade(symbol, description, qty, currentPriceUsd, isBuy)
                else Toast.makeText(context, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkNotificationPermissionAndShowDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> showPriceAlertDialog()
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else showPriceAlertDialog()
    }

    private fun showPriceAlertDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_set_price_alert, null)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.etTargetPrice)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvAlertTitle)
        val tvCurrent = dialogView.findViewById<TextView>(R.id.tvCurrentPriceInfo)

        tvTitle.text = "Set Alert for $symbol"
        tvCurrent.text = "Current: $${String.format("%.2f", currentPriceUsd)}"

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Set Alert") { _, _ ->
                val targetPrice = etPrice.text.toString().toDoubleOrNull()
                if (targetPrice != null && targetPrice > 0) viewModel.setPriceAlert(symbol, description, targetPrice)
                else Toast.makeText(context, "Please enter a valid price", Toast.LENGTH_SHORT).show()
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
                        activity?.runOnUiThread { 
                            currentPriceUsd = lastPrice
                            updatePriceUI()
                        }
                    }
                } catch (e: Exception) {}
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webSocket?.close(1000, "Closed")
    }
}
