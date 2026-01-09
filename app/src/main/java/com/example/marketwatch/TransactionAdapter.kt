package com.example.marketwatch

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private var userCurrency: String = "USD",
    private var currencySymbol: String = "$",
    private var exchangeRate: Double = 1.0
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivTransactionIcon)
        val title: TextView = view.findViewById(R.id.tvTransactionTitle)
        val date: TextView = view.findViewById(R.id.tvTransactionDate)
        val amount: TextView = view.findViewById(R.id.tvTransactionAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.date.text = transaction.timestamp?.toDate()?.let { sdf.format(it) } ?: ""

        val convertedAmount = transaction.amount * exchangeRate
        val formattedAmount = "${if (transaction.type == "BUY" || transaction.type == "WITHDRAW") "-" else "+"}$currencySymbol${String.format("%.2f", convertedAmount)}"

        when (transaction.type) {
            "BUY" -> {
                holder.title.text = "Buy ${transaction.symbol}"
                holder.amount.text = formattedAmount
                holder.amount.setTextColor(Color.parseColor("#F44336"))
                holder.icon.setImageResource(android.R.drawable.ic_menu_save)
            }
            "SELL" -> {
                holder.title.text = "Sell ${transaction.symbol}"
                holder.amount.text = formattedAmount
                holder.amount.setTextColor(Color.parseColor("#4CAF50"))
                holder.icon.setImageResource(android.R.drawable.ic_menu_upload)
            }
            "DEPOSIT" -> {
                holder.title.text = "Deposit Funds"
                holder.amount.text = formattedAmount
                holder.amount.setTextColor(Color.parseColor("#4CAF50"))
                holder.icon.setImageResource(android.R.drawable.ic_input_add)
            }
            "WITHDRAW" -> {
                holder.title.text = "Withdraw Funds"
                holder.amount.text = formattedAmount
                holder.amount.setTextColor(Color.parseColor("#F44336"))
                holder.icon.setImageResource(android.R.drawable.ic_menu_upload)
            }
        }
    }

    override fun getItemCount() = transactions.size

    fun updateData(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    fun updateCurrency(currency: String, symbol: String, rate: Double) {
        this.userCurrency = currency
        this.currencySymbol = symbol
        this.exchangeRate = rate
        notifyDataSetChanged()
    }
}
