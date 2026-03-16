package com.example.marketwatch

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for the community feed.
 * Handles display of posts including user info, content, images, and interactions.
 */
class PostsAdapter(
    private val posts: List<Post>,
    private val currentUserId: String?,
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userImage: ImageView = view.findViewById(R.id.postUserProfileImage)
        val userName: TextView = view.findViewById(R.id.postUserName)
        val timestamp: TextView = view.findViewById(R.id.postTimestamp)
        val content: TextView = view.findViewById(R.id.postContent)
        val postImageCard: View = view.findViewById(R.id.postImageCard)
        val postImage: ImageView = view.findViewById(R.id.postImage)
        val menuButton: View = view.findViewById(R.id.postMenuButton)
        val btnLike: ImageView = view.findViewById(R.id.btnLike)
        val tvLikeCount: TextView = view.findViewById(R.id.tvLikeCount)
        val btnComment: MaterialButton = view.findViewById(R.id.btnComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        
        holder.userName.text = if (post.userName.isNotBlank()) post.userName else "Anonymous"
        holder.content.text = if (post.content.isNotBlank()) post.content else "(No content)"
        
        // Format date to match partner's screen: MAR 14, 20:00
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
        holder.timestamp.text = post.timestamp?.toDate()?.let { sdf.format(it).uppercase() } ?: "JUST NOW"

        // Setup menu for the dropdown icon
        holder.menuButton.setOnClickListener { view ->
            showPopupMenu(view, post)
        }

        // Load profile picture
        val profilePic = if (post.userProfilePicture.isNotBlank()) post.userProfilePicture else null
        Picasso.get()
            .load(profilePic)
            .placeholder(R.drawable.ic_account_circle)
            .error(R.drawable.ic_account_circle)
            .transform(CircleTransform())
            .into(holder.userImage)

        // Post Image visibility
        if (!post.imageUrl.isNullOrEmpty()) {
            holder.postImageCard.visibility = View.VISIBLE
            Picasso.get().load(post.imageUrl).into(holder.postImage)
        } else {
            holder.postImageCard.visibility = View.GONE
        }

        // Like Logic - match "0 LIKES" style
        val isLiked = currentUserId != null && post.likes.contains(currentUserId)
        
        // Change icon based on status
        holder.btnLike.setImageResource(if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
        
        // Change color to DARK RED when liked, else GRAY
        if (isLiked) {
            holder.btnLike.setColorFilter(Color.parseColor("#B71C1C")) // Dark Red
        } else {
            holder.btnLike.setColorFilter(Color.parseColor("#8E8E93")) // Standard Gray
        }

        holder.tvLikeCount.text = "${post.likes.size} LIKES"

        holder.btnLike.setOnClickListener {
            onLikeClick(post)
        }

        holder.tvLikeCount.text = "${post.likes.size} LIKES"

        holder.btnLike.setOnClickListener { onLikeClick(post) }
        
        // Comment logic
        holder.btnComment.setOnClickListener { onCommentClick(post) }
    }

    private fun showPopupMenu(view: View, post: Post) {
        val popup = PopupMenu(view.context, view)
        if (post.userId == currentUserId) {
            popup.menu.add("Edit")
            popup.menu.add("Delete")
        } else {
            popup.menu.add("Report")
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Edit" -> onEditClick(post)
                "Delete" -> onDeleteClick(post)
            }
            true
        }
        popup.show()
    }

    override fun getItemCount() = posts.size
}
