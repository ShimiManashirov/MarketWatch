package com.example.marketwatch

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

/**
 * Fragment responsible for searching and discovering stocks.
 * Uses the Finnhub API to fetch stock symbols and provides a list of recent searches.
 */
class SearchFragment : Fragment() {

    private lateinit var adapter: SearchAdapter
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var emptySearchContainer: View
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

        initViews(view)
        setupSearchLogic()
        updateRecentSearchChips()

        return view
    }

    private fun initViews(view: View) {
        shimmerContainer = view.findViewById(R.id.searchShimmerContainer)
        emptySearchContainer = view.findViewById(R.id.emptySearchContainer)
        searchRecyclerView = view.findViewById(R.id.searchRecyclerView)
        searchView = view.findViewById(R.id.stockSearchView)
        chipGroup = view.findViewById(R.id.suggestionChipGroup)

        searchRecyclerView.layoutManager = LinearLayoutManager(context)
        adapter = SearchAdapter(emptyList())
        searchRecyclerView.adapter = adapter

        view.findViewById<TextView>(R.id.clearHistoryText)?.setOnClickListener {
            clearSearchHistory()
        }
    }

    private fun setupSearchLogic() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { 
                    saveRecentSearch(it)
                    performSearch(it) 
                }
                searchView.clearFocus()
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
                    if (isAdded && newText.length >= 1) performSearch(newText)
                }
                handler.postDelayed(searchRunnable!!, 500)
                return true
            }
        })
    }

    private fun saveRecentSearch(query: String) {
        val context = context ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val recentJson = prefs.getString(KEY_RECENT_SEARCHES, null)
        val gson = Gson()
        
        val type = object : TypeToken<MutableList<String>>() {}.type
        val recentList: MutableList<String> = if (recentJson != null) {
            try {
                gson.fromJson(recentJson, type)
            } catch (e: Exception) {
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        val formattedQuery = query.trim().uppercase()
        if (formattedQuery.isNotBlank()) {
            recentList.remove(formattedQuery)
            recentList.add(0, formattedQuery)
            val limitedList = recentList.take(5)
            prefs.edit().putString(KEY_RECENT_SEARCHES, gson.toJson(limitedList)).apply()
        }
    }

    private fun clearSearchHistory() {
        val context = context ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_RECENT_SEARCHES).apply()
        updateRecentSearchChips()
    }

    private fun updateRecentSearchChips() {
        val context = context ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val recentJson = prefs.getString(KEY_RECENT_SEARCHES, null)
        
        val listToDisplay = if (recentJson != null) {
            try {
                Gson().fromJson<List<String>>(recentJson, object : TypeToken<List<String>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        chipGroup.removeAllViews()
        
        val items = if (listToDisplay.isEmpty()) {
            listOf("AAPL", "TSLA", "BTC", "NVDA", "GOOGL")
        } else {
            listToDisplay
        }

        items.forEach { symbol -> addChip(symbol) }
    }

    private fun addChip(symbol: String) {
        val context = context ?: return
        // Using a simpler Chip construction to avoid potential style-related crashes
        val chip = Chip(context)
        chip.text = symbol
        chip.isCheckable = false
        chip.isClickable = true
        chip.setOnClickListener {
            searchView.setQuery(symbol, true)
        }
        chipGroup.addView(chip)
    }

    private fun showEmptyState() {
        if (!isAdded) return
        emptySearchContainer.visibility = View.VISIBLE
        searchRecyclerView.visibility = View.GONE
        shimmerContainer.stopShimmer()
        shimmerContainer.visibility = View.GONE
    }

    private fun showLoadingState() {
        if (!isAdded) return
        emptySearchContainer.visibility = View.GONE
        searchRecyclerView.visibility = View.GONE
        shimmerContainer.visibility = View.VISIBLE
        shimmerContainer.startShimmer()
    }

    private fun showResultsState() {
        if (!isAdded) return
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

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }
}
