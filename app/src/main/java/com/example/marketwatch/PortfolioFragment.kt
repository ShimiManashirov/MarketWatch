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

class PortfolioFragment : Fragment() {

    private val viewModel: PortfolioViewModel by viewModels()
    
    private lateinit var holdingsAdapter: PortfolioAdapter
    private lateinit var watchlistAdapter: PortfolioAdapter
    
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyTextContainer: LinearLayout
    private lateinit var pieChart: PieChart
    private lateinit var chartCard: MaterialCardView
    
    private val ownedList = mutableListOf<PortfolioItem>()
    private val watchlistItems = mutableListOf<PortfolioItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_portfolio, container, false)

        initViews(view)
        setupRecyclerViews()
        setupPieChart()
        observeViewModel()

        return view
    }

    private fun initViews(view: View) {
        progressBar = view.findViewById(R.id.portfolioProgressBar)
        emptyTextContainer = view.findViewById(R.id.emptyPortfolioText)
        pieChart = view.findViewById(R.id.portfolioPieChart)
        chartCard = view.findViewById(R.id.portfolioChartCard)
    }

    private fun setupRecyclerViews() {
        val rvHoldings = view?.findViewById<RecyclerView>(R.id.portfolioRecyclerView)
        val rvWatchlist = view?.findViewById<RecyclerView>(R.id.watchlistRecyclerView)

        holdingsAdapter = PortfolioAdapter(ownedList) { item -> showRemoveDialog(item, true) }
        rvHoldings?.layoutManager = LinearLayoutManager(context)
        rvHoldings?.adapter = holdingsAdapter

        watchlistAdapter = PortfolioAdapter(watchlistItems) { item -> showRemoveDialog(item, false) }
        rvWatchlist?.layoutManager = LinearLayoutManager(context)
        rvWatchlist?.adapter = watchlistAdapter
    }

    private fun setupPieChart() {
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            centerText = "Holdings"
            setCenterTextSize(16f)
            legend.isEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.portfolioItems.observe(viewLifecycleOwner) { items ->
            updateUI(items)
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
    }

    private fun updateVisibility() {
        val totalEmpty = ownedList.isEmpty() && watchlistItems.isEmpty()
        emptyTextContainer.visibility = if (totalEmpty) View.VISIBLE else View.GONE
        chartCard.visibility = if (ownedList.isEmpty()) View.GONE else View.VISIBLE
        
        view?.findViewById<View>(R.id.holdingsSection)?.visibility = if (ownedList.isEmpty()) View.GONE else View.VISIBLE
        view?.findViewById<View>(R.id.watchlistSection)?.visibility = if (watchlistItems.isEmpty()) View.GONE else View.VISIBLE
        view?.findViewById<View>(R.id.sectionDivider)?.visibility = if (ownedList.isNotEmpty() && watchlistItems.isNotEmpty()) View.VISIBLE else View.GONE
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
