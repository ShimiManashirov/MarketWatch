package com.example.marketwatch

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.PropertyName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class PortfolioItem(
    @get:PropertyName("symbol") @set:PropertyName("symbol") var symbol: String = "",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("quantity") @set:PropertyName("quantity") var quantity: Double = 0.0,
    @get:PropertyName("isFavorite") @set:PropertyName("isFavorite") var isFavorite: Boolean = false
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
        
        // הצגת תג "OWNED" אם המניה בבעלות
        holder.holdingBadge.visibility = if (item.quantity > 0) View.VISIBLE else View.GONE
        
        // הצגת כוכב אם המניה במועדפים
        holder.favoriteIndicator.visibility = if (item.isFavorite) View.VISIBLE else View.GONE

        // עדכון התיאור
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

        // טעינת מחיר עדכני
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
        items = ArrayList(newItems) // יצירת עותק חדש כדי להבטיח עדכון
        notifyDataSetChanged()
    }
}
