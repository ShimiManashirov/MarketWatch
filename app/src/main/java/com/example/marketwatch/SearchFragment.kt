package com.example.marketwatch

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : Fragment() {

    private lateinit var adapter: SearchAdapter
    private lateinit var progressBar: ProgressBar
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        progressBar = view.findViewById(R.id.searchProgressBar)
        val searchView = view.findViewById<SearchView>(R.id.stockSearchView)
        val recyclerView = view.findViewById<RecyclerView>(R.id.searchRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = SearchAdapter(emptyList())
        recyclerView.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Debouncing: wait for user to stop typing
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    newText?.let {
                        if (it.length >= 2) performSearch(it)
                    }
                }
                handler.postDelayed(searchRunnable!!, 500)
                return true
            }
        })

        return view
    }

    private fun performSearch(query: String) {
        progressBar.visibility = View.VISIBLE
        FinnhubApiClient.apiService.searchStock(query, FinnhubApiClient.API_KEY)
            .enqueue(object : Callback<StockLookupResponse> {
                override fun onResponse(call: Call<StockLookupResponse>, response: Response<StockLookupResponse>) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val stocks = response.body()?.result ?: emptyList()
                        adapter.updateData(stocks)
                    } else {
                        Toast.makeText(context, "Search failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<StockLookupResponse>, t: Throwable) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
