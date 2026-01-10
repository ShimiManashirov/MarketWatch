package com.example.marketwatch

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
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
    private lateinit var adapter: PortfolioAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyTextContainer: LinearLayout
    private lateinit var pieChart: PieChart
    private lateinit var chartCard: MaterialCardView
    private val portfolioList = mutableListOf<PortfolioItem>()

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
        val recyclerView = view.findViewById<RecyclerView>(R.id.portfolioRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = PortfolioAdapter(portfolioList) { item ->
            showDeleteDialog(item)
        }
        recyclerView.adapter = adapter

        setupPieChart()
        loadWatchlist()

        return view
    }

    private fun setupPieChart() {
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Portfolio"
            setCenterTextSize(18f)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            legend.isEnabled = false
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
        }
    }

    private fun loadWatchlist() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        progressBar.visibility = View.VISIBLE

        db.collection("users").document(userId)
            .collection("watchlist")
            .addSnapshotListener { snapshots, e ->
                if (!isAdded) return@addSnapshotListener
                progressBar.visibility = View.GONE
                
                if (e != null) {
                    Log.e("PortfolioFragment", "Error loading watchlist", e)
                    return@addSnapshotListener
                }

                portfolioList.clear()
                val pieEntries = mutableListOf<PieEntry>()
                var totalQuantity = 0.0

                snapshots?.forEach { doc ->
                    try {
                        val item = doc.toObject(PortfolioItem::class.java)
                        if (item.symbol.isNotBlank()) {
                            // Only show in "Active Holdings" list if it's a favorite or owned
                            if (item.isFavorite || item.quantity > 0) {
                                portfolioList.add(item)
                            }
                            // Only add to Pie Chart if owned
                            if (item.quantity > 0) {
                                pieEntries.add(PieEntry(item.quantity.toFloat(), item.symbol))
                                totalQuantity += item.quantity
                            }
                        }
                    } catch (ex: Exception) {
                        Log.e("PortfolioFragment", "Error parsing stock item", ex)
                    }
                }

                adapter.updateData(portfolioList)
                updatePieChart(pieEntries)

                emptyTextContainer.visibility = if (portfolioList.isEmpty()) View.VISIBLE else View.GONE
                chartCard.visibility = if (pieEntries.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    private fun updatePieChart(entries: List<PieEntry>) {
        if (entries.isEmpty()) return

        val dataSet = PieDataSet(entries, "Assets Allocation").apply {
            sliceSpace = 3f
            selectionShift = 5f
            colors = ColorTemplate.COLORFUL_COLORS.toList()
        }

        val data = PieData(dataSet).apply {
            setValueTextSize(10f)
            setValueTextColor(Color.WHITE)
        }

        pieChart.data = data
        pieChart.highlightValues(null)
        pieChart.animateY(1400)
        pieChart.invalidate()
    }

    private fun showDeleteDialog(item: PortfolioItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove from Favorites")
            .setMessage("Are you sure you want to remove ${item.symbol} from your favorites list?")
            .setPositiveButton("Remove") { _, _ ->
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                db.collection("users").document(userId)
                    .collection("watchlist").document(item.symbol)
                    .update("isFavorite", false)
                    .addOnSuccessListener {
                        if (isAdded) Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
