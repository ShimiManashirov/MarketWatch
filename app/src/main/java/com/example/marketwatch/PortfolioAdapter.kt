package com.example.marketwatch

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class PortfolioItem(
    val symbol: String = "",
    val description: String = "",
    val quantity: Double = 0.0,
    val isFavorite: Boolean = false
)

class PortfolioAdapter(
    private var items: List<PortfolioItem>,
    private val onLongClick: (PortfolioItem) -> Unit
) : RecyclerView.Adapter<PortfolioAdapter.PortfolioViewHolder>() {

    class PortfolioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val symbol: TextView = view.findViewById(R.id.stockSymbol)
        val description: TextView = view.findViewById(R.id.stockDescription)
        val price: TextView = view.findViewById(R.id.stockPrice)
        val change: TextView = view.findViewById(R.id.stockChange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PortfolioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stock_search, parent, false)
        return PortfolioViewHolder(view)
    }

    override fun onBindViewHolder(holder: PortfolioViewHolder, position: Int) {
        val item = items[position]
        
        holder.symbol.text = item.symbol
        // Show quantity in description if owned
        holder.description.text = if (item.quantity > 0) {
            "${String.format("%.2f", item.quantity)} Shares • ${item.description}"
        } else {
            "Watching • ${item.description}"
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

        FinnhubApiClient.apiService.getQuote(item.symbol, FinnhubApiClient.API_KEY)
            .enqueue(object : Callback<StockQuote> {
                override fun onResponse(call: Call<StockQuote>, response: Response<StockQuote>) {
                    if (response.isSuccessful) {
                        val quote = response.body()
                        if (quote != null) {
                            holder.price.text = "$${String.format("%.2f", quote.currentPrice)}"
                            holder.change.text = "${String.format("%.2f", quote.percentChange)}%"
                            holder.change.setTextColor(if (quote.percentChange >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
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
        items = newItems
        notifyDataSetChanged()
    }
}
