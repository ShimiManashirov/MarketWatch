package com.example.marketwatch

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PortfolioFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: PortfolioAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private val portfolioList = mutableListOf<PortfolioItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_portfolio, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        progressBar = view.findViewById(R.id.portfolioProgressBar)
        emptyText = view.findViewById(R.id.emptyPortfolioText)
        val recyclerView = view.findViewById<RecyclerView>(R.id.portfolioRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = PortfolioAdapter(portfolioList) { item ->
            showDeleteDialog(item)
        }
        recyclerView.adapter = adapter

        loadWatchlist()

        return view
    }

    private fun loadWatchlist() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        progressBar.visibility = View.VISIBLE

        db.collection("users").document(userId)
            .collection("watchlist")
            .whereEqualTo("isFavorite", true)
            .addSnapshotListener { snapshots, e ->
                if (!isAdded) return@addSnapshotListener
                progressBar.visibility = View.GONE
                
                if (e != null) {
                    Log.e("PortfolioFragment", "Error loading watchlist", e)
                    return@addSnapshotListener
                }

                portfolioList.clear()
                snapshots?.forEach { doc ->
                    try {
                        val item = doc.toObject(PortfolioItem::class.java)
                        if (item.symbol.isNotBlank()) {
                            portfolioList.add(item)
                        }
                    } catch (ex: Exception) {
                        Log.e("PortfolioFragment", "Error parsing stock item", ex)
                    }
                }

                adapter.updateData(portfolioList)
                emptyText.text = "Your favorites list is empty"
                emptyText.visibility = if (portfolioList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun showDeleteDialog(item: PortfolioItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove from Favorites")
            .setMessage("Are you sure you want to remove ${item.symbol} from your favorites list?")
            .setPositiveButton("Remove") { _, _ ->
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                
                // Instead of deleting the doc, just set isFavorite to false
                // This keeps the quantity data if they own shares
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
