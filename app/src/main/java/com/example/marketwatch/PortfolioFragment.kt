package com.example.marketwatch

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
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

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        progressBar = view.findViewById(R.id.portfolioProgressBar)
        emptyTextContainer = view.findViewById(R.id.emptyPortfolioText)
        pieChart = view.findViewById(R.id.portfolioPieChart)
        chartCard = view.findViewById(R.id.portfolioChartCard)
        
        val rvHoldings = view.findViewById<RecyclerView>(R.id.portfolioRecyclerView)
        val rvWatchlist = view.findViewById<RecyclerView>(R.id.watchlistRecyclerView)

        // Setup Holdings List
        rvHoldings.layoutManager = LinearLayoutManager(context)
        holdingsAdapter = PortfolioAdapter(ownedList) { item -> showRemoveDialog(item, true) }
        rvHoldings.adapter = holdingsAdapter

        // Setup Watchlist List
        rvWatchlist.layoutManager = LinearLayoutManager(context)
        watchlistAdapter = PortfolioAdapter(watchlistItems) { item -> showRemoveDialog(item, false) }
        rvWatchlist.adapter = watchlistAdapter

        setupPieChart()
        loadPortfolioData()

        return view
    }

    private fun setupPieChart() {
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            centerText = "Holdings"
            setCenterTextSize(16f)
            legend.isEnabled = false
        }
    }

    private fun loadPortfolioData() {
        val userId = auth.currentUser?.uid ?: return
        progressBar.visibility = View.VISIBLE

        db.collection("users").document(userId)
            .collection("watchlist")
            .addSnapshotListener { snapshots, e ->
                if (!isAdded) return@addSnapshotListener
                progressBar.visibility = View.GONE
                
                if (e != null) {
                    Log.e("PortfolioFragment", "Error loading data", e)
                    return@addSnapshotListener
                }

                ownedList.clear()
                watchlistItems.clear()
                val pieEntries = mutableListOf<PieEntry>()

                snapshots?.forEach { doc ->
                    try {
                        val item = doc.toObject(PortfolioItem::class.java)
                        if (item.symbol.isNotBlank()) {
                            // Split into two lists
                            if (item.quantity > 0) {
                                ownedList.add(item)
                                pieEntries.add(PieEntry(item.quantity.toFloat(), item.symbol))
                            } else if (item.isFavorite) {
                                watchlistItems.add(item)
                            }
                        }
                    } catch (ex: Exception) {
                        Log.e("PortfolioFragment", "Error parsing item", ex)
                    }
                }

                holdingsAdapter.updateData(ownedList)
                watchlistAdapter.updateData(watchlistItems)
                updatePieChart(pieEntries)

                // Manage Visibility
                val totalEmpty = ownedList.isEmpty() && watchlistItems.isEmpty()
                emptyTextContainer.visibility = if (totalEmpty) View.VISIBLE else View.GONE
                chartCard.visibility = if (ownedList.isEmpty()) View.GONE else View.VISIBLE
                
                view?.findViewById<TextView>(R.id.titleHoldings)?.visibility = if (ownedList.isEmpty()) View.GONE else View.VISIBLE
                view?.findViewById<TextView>(R.id.titleWatchlist)?.visibility = if (watchlistItems.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    private fun updatePieChart(entries: List<PieEntry>) {
        if (entries.isEmpty()) return
        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            sliceSpace = 2f
            valueTextColor = Color.WHITE
        }
        pieChart.data = PieData(dataSet)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun showRemoveDialog(item: PortfolioItem, isOwned: Boolean) {
        val title = if (isOwned) "Owned Stock" else "Remove from Watchlist"
        val message = if (isOwned) "You own shares of ${item.symbol}. Go to details to sell them." else "Remove ${item.symbol} from your favorites?"
        
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            
        if (!isOwned) {
            builder.setPositiveButton("Remove") { _, _ ->
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                db.collection("users").document(userId)
                    .collection("watchlist").document(item.symbol)
                    .update("isFavorite", false)
            }
        }
        
        builder.setNegativeButton("Close", null).show()
    }
}
