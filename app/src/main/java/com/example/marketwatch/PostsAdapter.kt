package com.example.marketwatch

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

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
        val btnComment: ImageView = view.findViewById(R.id.btnComment)
        val tvCommentCount: TextView = view.findViewById(R.id.tvCommentCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        
        holder.userName.text = if (post.userName.isNotBlank()) post.userName else "Anonymous"
        holder.content.text = post.content
        
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.timestamp.text = post.timestamp?.toDate()?.let { sdf.format(it).uppercase() } ?: "JUST NOW"

        ImageManager.loadProfileImage(holder.userImage, post.userProfilePicture)

        if (!post.imageUrl.isNullOrEmpty()) {
            holder.postImageCard.visibility = View.VISIBLE
            ImageManager.loadImage(holder.postImage, post.imageUrl, isCircle = false)
        } else {
            holder.postImageCard.visibility = View.GONE
        }

        val isLiked = currentUserId != null && post.likes.contains(currentUserId)
        holder.btnLike.setImageResource(if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
        holder.btnLike.setColorFilter(if (isLiked) Color.parseColor("#E91E63") else Color.parseColor("#8E8E93"))
        
        holder.tvLikeCount.text = post.likes.size.toString()
        
        holder.btnLike.setOnClickListener {
            // Animation for Like
            it.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction {
                it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }.start()
            onLikeClick(post)
        }

        holder.tvCommentCount.text = post.commentsCount.toString()
        holder.btnComment.setOnClickListener { onCommentClick(post) }
        
        holder.menuButton.setOnClickListener { showPopupMenu(it, post) }
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
