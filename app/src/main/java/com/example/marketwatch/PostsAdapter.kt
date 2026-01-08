package com.example.marketwatch

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class PostsAdapter(
    private val posts: List<Post>,
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userImage: ImageView = view.findViewById(R.id.postUserProfileImage)
        val userName: TextView = view.findViewById(R.id.postUserName)
        val timestamp: TextView = view.findViewById(R.id.postTimestamp)
        val content: TextView = view.findViewById(R.id.postContent)
        val postImageCard: View = view.findViewById(R.id.postImageCard)
        val postImage: ImageView = view.findViewById(R.id.postImage)
        val menuButton: ImageButton = view.findViewById(R.id.postMenuButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        
        holder.userName.text = if (post.userName.isNotBlank()) post.userName else "Anonymous"
        holder.content.text = if (post.content.isNotBlank()) post.content else "(No content)"
        
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.timestamp.text = post.timestamp?.toDate()?.let { sdf.format(it) } ?: "Just now"

        // Show menu button only for the post owner
        if (post.userId == currentUserId) {
            holder.menuButton.visibility = View.VISIBLE
            holder.menuButton.setOnClickListener { view ->
                showPopupMenu(view, post)
            }
        } else {
            holder.menuButton.visibility = View.GONE
        }

        Glide.with(holder.itemView.context)
            .load(if (post.userProfilePicture.isNotBlank()) post.userProfilePicture else R.drawable.ic_account_circle)
            .circleCrop()
            .placeholder(R.drawable.ic_account_circle)
            .into(holder.userImage)

        if (!post.imageUrl.isNullOrEmpty()) {
            holder.postImageCard.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(post.imageUrl)
                .into(holder.postImage)
        } else {
            holder.postImageCard.visibility = View.GONE
        }
    }

    private fun showPopupMenu(view: View, post: Post) {
        val popup = PopupMenu(view.context, view)
        popup.menu.add("Edit")
        popup.menu.add("Delete")
        
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
