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
import com.squareup.picasso.Picasso
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PostsAdapter(
    private val posts: List<Post>,
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val db = FirebaseFirestore.getInstance()

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userImage: ImageView = view.findViewById(R.id.postUserProfileImage)
        val userName: TextView = view.findViewById(R.id.postUserName)
        val timestamp: TextView = view.findViewById(R.id.postTimestamp)
        val content: TextView = view.findViewById(R.id.postContent)
        val postImageCard: View = view.findViewById(R.id.postImageCard)
        val postImage: ImageView = view.findViewById(R.id.postImage)
        val menuButton: ImageButton = view.findViewById(R.id.postMenuButton)
        val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        val tvLikeCount: TextView = view.findViewById(R.id.tvLikeCount)
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

        if (post.userId == currentUserId) {
            holder.menuButton.visibility = View.VISIBLE
            holder.menuButton.setOnClickListener { view ->
                showPopupMenu(view, post)
            }
        } else {
            holder.menuButton.visibility = View.GONE
        }

        // Use Picasso for user image
        val profilePic = if (post.userProfilePicture.isNotBlank()) post.userProfilePicture else null
        if (profilePic != null) {
            Picasso.get()
                .load(profilePic)
                .placeholder(R.drawable.ic_account_circle)
                .transform(CircleTransform())
                .into(holder.userImage)
        } else {
            holder.userImage.setImageResource(R.drawable.ic_account_circle)
        }

        // Use Picasso for post image
        if (!post.imageUrl.isNullOrEmpty()) {
            holder.postImageCard.visibility = View.VISIBLE
            Picasso.get()
                .load(post.imageUrl)
                .into(holder.postImage)
        } else {
            holder.postImageCard.visibility = View.GONE
        }

        // Like Logic
        val isLiked = currentUserId != null && post.likes.contains(currentUserId)
        holder.btnLike.setImageResource(if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
        holder.btnLike.setColorFilter(if (isLiked) Color.parseColor("#E91E63") else Color.GRAY)
        holder.tvLikeCount.text = "${post.likes.size} likes"

        holder.btnLike.setOnClickListener {
            if (currentUserId == null) return@setOnClickListener
            val postRef = db.collection("posts").document(post.id)
            if (isLiked) {
                postRef.update("likes", FieldValue.arrayRemove(currentUserId))
            } else {
                postRef.update("likes", FieldValue.arrayUnion(currentUserId))
            }
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
