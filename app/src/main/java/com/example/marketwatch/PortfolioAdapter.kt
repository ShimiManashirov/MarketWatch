package com.example.marketwatch

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PortfolioAdapter(
    private var items: List<PortfolioItem>,
    private val onLongClick: (PortfolioItem) -> Unit
) : RecyclerView.Adapter<PortfolioAdapter.PortfolioViewHolder>() {

    class PortfolioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val symbol: TextView = view.findViewById(R.id.stockSymbol)
        val description: TextView = view.findViewById(R.id.stockDescription)
        val price: TextView = view.findViewById(R.id.stockPrice)
        val change: TextView = view.findViewById(R.id.stockChange)
        val holdingBadge: TextView = view.findViewById(R.id.holdingBadge)
        val favoriteIndicator: ImageView = view.findViewById(R.id.favoriteIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PortfolioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stock_search, parent, false)
        return PortfolioViewHolder(view)
    }

    override fun onBindViewHolder(holder: PortfolioViewHolder, position: Int) {
        val item = items[position]
        
        holder.symbol.text = item.symbol
        holder.holdingBadge.visibility = if (item.quantity > 0) View.VISIBLE else View.GONE
        holder.favoriteIndicator.visibility = if (item.isFavorite) View.VISIBLE else View.GONE

        holder.description.text = when {
            item.quantity > 0 -> "${String.format("%.2f", item.quantity)} Shares • ${item.description}"
            item.isFavorite -> "In Watchlist • ${item.description}"
            else -> item.description
        }
        
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, StockDetailsActivity::class.java)
            intent.putExtra("symbol", item.symbol)
            intent.putExtra("description", item.description)
            holder.itemView.context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }

        fetchPriceAndCalculatePNL(item, holder)
    }

    private fun fetchPriceAndCalculatePNL(item: PortfolioItem, holder: PortfolioViewHolder) {
        FinnhubApiClient.apiService.getQuote(item.symbol, FinnhubApiClient.API_KEY)
            .enqueue(object : Callback<StockQuote> {
                override fun onResponse(call: Call<StockQuote>, response: Response<StockQuote>) {
                    if (response.isSuccessful) {
                        response.body()?.let { quote ->
                            holder.price.text = "$${String.format("%.2f", quote.currentPrice)}"
                            
                            if (item.quantity > 0 && item.totalCost > 0) {
                                val currentVal = item.quantity * quote.currentPrice
                                val profit = currentVal - item.totalCost
                                val profitPercent = (profit / item.totalCost) * 100
                                
                                val sign = if (profit >= 0) "+" else ""
                                holder.change.text = "$sign$${String.format("%.2f", profit)} (${String.format("%.2f", profitPercent)}%)"
                                holder.change.setTextColor(if (profit >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
                            } else {
                                holder.change.text = "${String.format("%.2f", quote.percentChange)}%"
                                val color = if (quote.percentChange >= 0) "#4CAF50" else "#F44336"
                                holder.change.setTextColor(Color.parseColor(color))
                            }
                        }
                    }
                }
                override fun onFailure(call: Call<StockQuote>, t: Throwable) {
                    holder.price.text = "N/A"
                }
            })
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<PortfolioItem>) {
        val diffCallback = PortfolioDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    class PortfolioDiffCallback(
        private val oldList: List<PortfolioItem>,
        private val newList: List<PortfolioItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int) = oldList[oldPos].symbol == newList[newPos].symbol
        override fun areContentsTheSame(oldPos: Int, newPos: Int) = oldList[oldPos] == newList[newPos]
    }
}
