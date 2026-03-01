package com.example.marketwatch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
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
        
        holder.price.visibility = View.GONE
        holder.change.visibility = View.GONE
        
        holder.itemView.setOnClickListener {
            val action = SearchFragmentDirections.actionSearchToStockDetails(stock.symbol, stock.description)
            holder.itemView.findNavController().navigate(action)
        }
    }

    override fun getItemCount() = stocks.size

    fun updateData(newStocks: List<StockSymbol>) {
        stocks = newStocks
        notifyDataSetChanged()
    }
}
