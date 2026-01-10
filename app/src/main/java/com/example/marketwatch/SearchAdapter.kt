package com.example.marketwatch

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchAdapter(private var stocks: List<StockSymbol>) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val symbol: TextView = view.findViewById(R.id.stockSymbol)
        val description: TextView = view.findViewById(R.id.stockDescription)
        val price: TextView = view.findViewById(R.id.stockPrice)
        val change: TextView = view.findViewById(R.id.stockChange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stock_search, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val stock = stocks[position]
        holder.symbol.text = stock.displaySymbol
        holder.description.text = stock.description
        
        // נסתיר את המחיר והשינוי ברשימת החיפוש כדי לשמור על מהירות ומגבלות API
        holder.price.visibility = View.GONE
        holder.change.visibility = View.GONE
        
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, StockDetailsActivity::class.java)
            intent.putExtra("symbol", stock.symbol)
            intent.putExtra("description", stock.description)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = stocks.size

    fun updateData(newStocks: List<StockSymbol>) {
        stocks = newStocks
        notifyDataSetChanged()
    }
}
