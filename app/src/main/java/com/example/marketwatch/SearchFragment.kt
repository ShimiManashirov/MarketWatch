package com.example.marketwatch

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : Fragment() {

    private lateinit var adapter: SearchAdapter
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var emptySearchContainer: LinearLayout
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var searchView: SearchView
    
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        shimmerContainer = view.findViewById(R.id.searchShimmerContainer)
        emptySearchContainer = view.findViewById(R.id.emptySearchContainer)
        searchRecyclerView = view.findViewById(R.id.searchRecyclerView)
        searchView = view.findViewById(R.id.stockSearchView)
        val chipGroup = view.findViewById<ChipGroup>(R.id.suggestionChipGroup)

        searchRecyclerView.layoutManager = LinearLayoutManager(context)
        adapter = SearchAdapter(emptyList())
        searchRecyclerView.adapter = adapter

        // Set up suggestion chips
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.setOnClickListener {
                val symbol = chip.text.toString()
                searchView.setQuery(symbol, true)
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { 
                    handler.removeCallbacksAndMessages(null)
                    performSearch(it) 
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    showEmptyState()
                    handler.removeCallbacksAndMessages(null)
                    return true
                }

                // Debouncing: wait for user to stop typing
                handler.removeCallbacksAndMessages(null)
                searchRunnable = Runnable {
                    if (newText.length >= 1) performSearch(newText)
                }
                handler.postDelayed(searchRunnable!!, 400)
                return true
            }
        })

        return view
    }

    private fun showEmptyState() {
        emptySearchContainer.visibility = View.VISIBLE
        searchRecyclerView.visibility = View.GONE
        shimmerContainer.stopShimmer()
        shimmerContainer.visibility = View.GONE
    }

    private fun showLoadingState() {
        emptySearchContainer.visibility = View.GONE
        searchRecyclerView.visibility = View.GONE
        shimmerContainer.visibility = View.VISIBLE
        shimmerContainer.startShimmer()
    }

    private fun showResultsState() {
        emptySearchContainer.visibility = View.GONE
        shimmerContainer.stopShimmer()
        shimmerContainer.visibility = View.GONE
        searchRecyclerView.visibility = View.VISIBLE
    }

    private fun performSearch(query: String) {
        showLoadingState()
        
        // Log query for debugging
        Log.d("SearchFragment", "Searching for: $query")

        FinnhubApiClient.apiService.searchStock(query, FinnhubApiClient.API_KEY)
            .enqueue(object : Callback<StockLookupResponse> {
                override fun onResponse(call: Call<StockLookupResponse>, response: Response<StockLookupResponse>) {
                    if (!isAdded) return
                    
                    if (response.isSuccessful) {
                        val stocks = response.body()?.result ?: emptyList()
                        Log.d("SearchFragment", "Results found: ${stocks.size}")
                        
                        if (stocks.isNotEmpty()) {
                            adapter.updateData(stocks)
                            showResultsState()
                        } else {
                            adapter.updateData(emptyList())
                            showEmptyState()
                        }
                    } else {
                        Log.e("SearchFragment", "Search failed: ${response.code()}")
                        showEmptyState()
                        Toast.makeText(context, "Search failed. Check your API limits.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<StockLookupResponse>, t: Throwable) {
                    if (!isAdded) return
                    Log.e("SearchFragment", "Network error", t)
                    showEmptyState()
                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
