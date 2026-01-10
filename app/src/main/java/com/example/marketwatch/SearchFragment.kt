package com.example.marketwatch

import android.content.Context
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : Fragment() {

    private lateinit var adapter: SearchAdapter
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var emptySearchContainer: LinearLayout
    private lateinit var searchRecyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var chipGroup: ChipGroup
    
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private val PREFS_NAME = "search_prefs"
    private val KEY_RECENT_SEARCHES = "recent_searches"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        shimmerContainer = view.findViewById(R.id.searchShimmerContainer)
        emptySearchContainer = view.findViewById(R.id.emptySearchContainer)
        searchRecyclerView = view.findViewById(R.id.searchRecyclerView)
        searchView = view.findViewById(R.id.stockSearchView)
        chipGroup = view.findViewById(R.id.suggestionChipGroup)

        searchRecyclerView.layoutManager = LinearLayoutManager(context)
        adapter = SearchAdapter(emptyList())
        searchRecyclerView.adapter = adapter

        updateRecentSearchChips()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { 
                    saveRecentSearch(it)
                    performSearch(it) 
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    showEmptyState()
                    updateRecentSearchChips()
                    handler.removeCallbacksAndMessages(null)
                    return true
                }

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

    private fun saveRecentSearch(query: String) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val recentJson = prefs.getString(KEY_RECENT_SEARCHES, null)
        val gson = Gson()
        
        val type = object : TypeToken<MutableList<String>>() {}.type
        val recentList: MutableList<String> = if (recentJson != null) {
            gson.fromJson(recentJson, type)
        } else {
            mutableListOf()
        }

        // Add to start, remove duplicates, limit to 5
        recentList.remove(query.uppercase())
        recentList.add(0, query.uppercase())
        val limitedList = recentList.take(5)

        prefs.edit().putString(KEY_RECENT_SEARCHES, gson.toJson(limitedList)).apply()
    }

    private fun updateRecentSearchChips() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val recentJson = prefs.getString(KEY_RECENT_SEARCHES, null)
        
        val listToDisplay = if (recentJson != null) {
            Gson().fromJson<List<String>>(recentJson, object : TypeToken<List<String>>() {}.type)
        } else {
            listOf("AAPL", "TSLA", "BTC", "NVDA") // Defaults
        }

        chipGroup.removeAllViews()
        for (symbol in listToDisplay) {
            val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Suggestion)
            chip.text = symbol
            chip.setOnClickListener {
                searchView.setQuery(symbol, true)
            }
            chipGroup.addView(chip)
        }
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
        FinnhubApiClient.apiService.searchStock(query, FinnhubApiClient.API_KEY)
            .enqueue(object : Callback<StockLookupResponse> {
                override fun onResponse(call: Call<StockLookupResponse>, response: Response<StockLookupResponse>) {
                    if (!isAdded) return
                    if (response.isSuccessful) {
                        val stocks = response.body()?.result ?: emptyList()
                        if (stocks.isNotEmpty()) {
                            adapter.updateData(stocks)
                            showResultsState()
                        } else {
                            adapter.updateData(emptyList())
                            showEmptyState()
                        }
                    } else {
                        showEmptyState()
                    }
                }
                override fun onFailure(call: Call<StockLookupResponse>, t: Throwable) {
                    if (!isAdded) return
                    showEmptyState()
                }
            })
    }
}
