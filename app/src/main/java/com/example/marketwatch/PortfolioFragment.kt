package com.example.marketwatch

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PortfolioFragment : Fragment() {

    private val viewModel: PortfolioViewModel by viewModels()
    
    private lateinit var holdingsAdapter: PortfolioAdapter
    private lateinit var watchlistAdapter: PortfolioAdapter
    
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyTextContainer: LinearLayout
    private lateinit var pieChart: PieChart
    private lateinit var chartCard: MaterialCardView
    private lateinit var holdingsSection: View
    private lateinit var watchlistSection: View
    private lateinit var sectionDivider: View
    
    private lateinit var tvTotalBalance: TextView
    private lateinit var tvTotalBalanceIls: TextView
    private lateinit var tvCashBalance: TextView
    private lateinit var tvTotalProfit: TextView
    
    private val ownedList = mutableListOf<PortfolioItem>()
    private val watchlistItems = mutableListOf<PortfolioItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_portfolio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerViews(view)
        setupPieChart()
        observeViewModel()
    }

    private fun initViews(view: View) {
        progressBar = view.findViewById(R.id.portfolioProgressBar)
        emptyTextContainer = view.findViewById(R.id.emptyPortfolioText)
        pieChart = view.findViewById(R.id.portfolioPieChart)
        chartCard = view.findViewById(R.id.portfolioChartCard)
        holdingsSection = view.findViewById(R.id.holdingsSection)
        watchlistSection = view.findViewById(R.id.watchlistSection)
        sectionDivider = view.findViewById(R.id.sectionDivider)
        
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance)
        tvTotalBalanceIls = view.findViewById(R.id.tvTotalBalanceIls)
        tvCashBalance = view.findViewById(R.id.tvCashBalance)
        tvTotalProfit = view.findViewById(R.id.tvTotalProfit)
    }

    private fun setupRecyclerViews(view: View) {
        val rvHoldings = view.findViewById<RecyclerView>(R.id.portfolioRecyclerView)
        val rvWatchlist = view.findViewById<RecyclerView>(R.id.watchlistRecyclerView)

        holdingsAdapter = PortfolioAdapter(ownedList) { item -> showRemoveDialog(item, true) }
        rvHoldings.layoutManager = LinearLayoutManager(context)
        rvHoldings.adapter = holdingsAdapter

        watchlistAdapter = PortfolioAdapter(watchlistItems) { item -> showRemoveDialog(item, false) }
        rvWatchlist.layoutManager = LinearLayoutManager(context)
        rvWatchlist.adapter = watchlistAdapter
    }

    private fun setupPieChart() {
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            centerText = "Assets"
            setCenterTextSize(16f)
            legend.isEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.portfolioItems.observe(viewLifecycleOwner) { items ->
            if (items != null) {
                updateUI(items)
            }
        }

        viewModel.userBalance.observe(viewLifecycleOwner) { balance ->
            updateSummary()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun updateUI(items: List<PortfolioItem>) {
        ownedList.clear()
        watchlistItems.clear()
        val pieEntries = mutableListOf<PieEntry>()

        items.forEach { item ->
            if (item.quantity > 0) {
                ownedList.add(item)
                pieEntries.add(PieEntry(item.quantity.toFloat(), item.symbol))
            }
            if (item.isFavorite) {
                watchlistItems.add(item)
            }
        }

        holdingsAdapter.updateData(ownedList)
        watchlistAdapter.updateData(watchlistItems)
        updatePieChart(pieEntries)
        updateVisibility()
        updateSummary()
    }

    private fun updateSummary() {
        val cash = viewModel.userBalance.value ?: 0.0
        tvCashBalance.text = "$${String.format("%.2f", cash)}"
        
        var totalStockValue = 0.0
        var totalCost = 0.0
        var loadedCount = 0

        if (ownedList.isEmpty()) {
            displaySummary(cash, 0.0, 0.0)
            return
        }

        ownedList.forEach { item ->
            FinnhubApiClient.apiService.getQuote(item.symbol, FinnhubApiClient.API_KEY)
                .enqueue(object : Callback<StockQuote> {
                    override fun onResponse(call: Call<StockQuote>, response: Response<StockQuote>) {
                        if (response.isSuccessful) {
                            response.body()?.let { quote ->
                                totalStockValue += (item.quantity * quote.currentPrice)
                                totalCost += item.totalCost
                            }
                        }
                        loadedCount++
                        if (loadedCount == ownedList.size) {
                            displaySummary(cash, totalStockValue, totalCost)
                        }
                    }
                    override fun onFailure(call: Call<StockQuote>, t: Throwable) {
                        loadedCount++
                        if (loadedCount == ownedList.size) displaySummary(cash, totalStockValue, totalCost)
                    }
                })
        }
    }

    private fun displaySummary(cash: Double, stockValue: Double, cost: Double) {
        val totalBalance = cash + stockValue
        val totalProfit = stockValue - cost
        val rate = viewModel.exchangeRate.value ?: 3.7
        
        tvTotalBalance.text = "$${String.format("%.2f", totalBalance)}"
        tvTotalBalanceIls.text = "≈ ₪${String.format("%.2f", totalBalance * rate)}"
        
        val profitSign = if (totalProfit >= 0) "+" else ""
        tvTotalProfit.text = "$profitSign$${String.format("%.2f", totalProfit)}"
        tvTotalProfit.setTextColor(if (totalProfit >= 0) Color.GREEN else Color.RED)
    }

    private fun updateVisibility() {
        val hasHoldings = ownedList.isNotEmpty()
        val hasWatchlist = watchlistItems.isNotEmpty()
        
        emptyTextContainer.visibility = if (!hasHoldings && !hasWatchlist) View.VISIBLE else View.GONE
        chartCard.visibility = if (hasHoldings) View.VISIBLE else View.GONE
        holdingsSection.visibility = if (hasHoldings) View.VISIBLE else View.GONE
        watchlistSection.visibility = if (hasWatchlist) View.VISIBLE else View.GONE
        sectionDivider.visibility = if (hasHoldings && hasWatchlist) View.VISIBLE else View.GONE
    }

    private fun updatePieChart(entries: List<PieEntry>) {
        if (entries.isEmpty()) {
            pieChart.data = null
            pieChart.invalidate()
            return
        }
        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            sliceSpace = 2f
            valueTextColor = Color.WHITE
            valueTextSize = 12f
        }
        pieChart.data = PieData(dataSet)
        pieChart.invalidate()
    }

    private fun showRemoveDialog(item: PortfolioItem, fromHoldings: Boolean) {
        val title = if (fromHoldings) "Owned Stock" else "Remove from Watchlist"
        val message = if (fromHoldings) "You own ${item.quantity} shares. Go to details to sell." else "Remove ${item.symbol} from favorites?"
        
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            
        if (!fromHoldings) {
            builder.setPositiveButton("Remove") { _, _ ->
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton
                FirebaseFirestore.getInstance().collection("users").document(userId)
                    .collection("watchlist").document(item.symbol)
                    .update("isFavorite", false)
            }
        }
        builder.setNegativeButton("Close", null).show()
    }
}
