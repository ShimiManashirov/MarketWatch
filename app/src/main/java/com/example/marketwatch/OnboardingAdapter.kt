package com.example.marketwatch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for the onboarding ViewPager2.
 * Renders onboarding screens with custom illustrations, titles, and descriptions.
 *
 * @property items The list of [OnboardingItem] data to display.
 */
class OnboardingAdapter(private val items: List<OnboardingItem>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivOnboardingImage)
        val titleView: TextView = view.findViewById(R.id.tvOnboardingTitle)
        val descriptionView: TextView = view.findViewById(R.id.tvOnboardingDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val item = items[position]
        holder.imageView.setImageResource(item.imageRes)
        holder.titleView.text = item.title
        holder.descriptionView.text = item.description
    }

    override fun getItemCount(): Int = items.size
}
