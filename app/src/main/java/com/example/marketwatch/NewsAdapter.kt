package com.example.marketwatch

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying stock market news.
 * Includes support for bookmarking articles and opening them in a browser.
 *
 * @property newsList The list of news articles to display.
 * @property bookmarkedIds A set of IDs representing articles currently bookmarked by the user.
 * @property onBookmarkClick Callback triggered when the bookmark button is clicked.
 */
class NewsAdapter(
    private var newsList: List<StockNews>,
    private var bookmarkedIds: Set<Long> = emptySet(),
    private val onBookmarkClick: (StockNews, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivNewsImage)
        val source: TextView = view.findViewById(R.id.tvNewsSource)
        val headline: TextView = view.findViewById(R.id.tvNewsHeadline)
        val date: TextView = view.findViewById(R.id.tvNewsDate)
        val btnBookmark: ImageButton = view.findViewById(R.id.btnBookmark)
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
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.image)
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        // Bookmark Logic
        val isBookmarked = bookmarkedIds.contains(news.id)
        holder.btnBookmark.setImageResource(
            if (isBookmarked) android.R.drawable.btn_star_big_on 
            else android.R.drawable.btn_star_big_off
        )
        
        // Material 3 tinting
        holder.btnBookmark.setColorFilter(
            if (isBookmarked) Color.parseColor("#FFB100") // Gold for active
            else Color.GRAY
        )

        holder.btnBookmark.setOnClickListener {
            onBookmarkClick(news, isBookmarked)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.url))
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = newsList.size

    /**
     * Updates the news list and refreshes the adapter.
     */
    fun updateNews(newList: List<StockNews>) {
        this.newsList = newList
        notifyDataSetChanged()
    }

    /**
     * Updates the set of bookmarked IDs and refreshes the adapter.
     */
    fun updateBookmarks(newBookmarkedIds: Set<Long>) {
        this.bookmarkedIds = newBookmarkedIds
        notifyDataSetChanged()
    }
}
