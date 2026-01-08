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
        
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, StockDetailsActivity::class.java)
            intent.putExtra("symbol", stock.symbol)
            intent.putExtra("description", stock.description)
            holder.itemView.context.startActivity(intent)
        }

        // Reset price and change while loading
        holder.price.text = "Loading..."
        holder.change.text = ""

        // Fetch real-time price for this symbol
        FinnhubApiClient.apiService.getQuote(stock.symbol, FinnhubApiClient.API_KEY)
            .enqueue(object : Callback<StockQuote> {
                override fun onResponse(call: Call<StockQuote>, response: Response<StockQuote>) {
                    if (response.isSuccessful) {
                        val quote = response.body()
                        if (quote != null) {
                            holder.price.text = "$${String.format("%.2f", quote.currentPrice)}"
                            val changeText = "${String.format("%.2f", quote.percentChange)}%"
                            holder.change.text = changeText
                            
                            if (quote.percentChange >= 0) {
                                holder.change.setTextColor(Color.parseColor("#4CAF50")) // Green
                            } else {
                                holder.change.setTextColor(Color.parseColor("#F44336")) // Red
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<StockQuote>, t: Throwable) {
                    holder.price.text = "N/A"
                }
            })
    }

    override fun getItemCount() = stocks.size

    fun updateData(newStocks: List<StockSymbol>) {
        stocks = newStocks
        notifyDataSetChanged()
    }
}
