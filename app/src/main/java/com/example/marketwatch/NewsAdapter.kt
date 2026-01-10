package com.example.marketwatch

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class NewsAdapter(private val newsList: List<StockNews>) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivNewsImage)
        val source: TextView = view.findViewById(R.id.tvNewsSource)
        val headline: TextView = view.findViewById(R.id.tvNewsHeadline)
        val date: TextView = view.findViewById(R.id.tvNewsDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stock_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = newsList[position]
        holder.source.text = news.source
        holder.headline.text = news.headline
        
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.date.text = sdf.format(Date(news.datetime * 1000))

        if (!news.image.isNullOrEmpty()) {
            Picasso.get()
                .load(news.image)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .into(holder.image)
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.url))
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = newsList.size
}
