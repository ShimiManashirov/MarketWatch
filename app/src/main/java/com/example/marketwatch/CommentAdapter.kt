package com.example.marketwatch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.util.*

/**
 * Adapter for displaying a list of comments in a discussion thread.
 * Handles rendering comment content, user metadata, and deletion logic.
 *
 * @property comments The list of [Comment] objects to display.
 * @property currentUserId The ID of the currently logged-in user (to enable deletion of own comments).
 * @property onDeleteClick Callback triggered when the delete button is pressed.
 */
class CommentAdapter(
    private var comments: List<Comment>,
    private val currentUserId: String?,
    private val onDeleteClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userImage: ImageView = view.findViewById(R.id.ivCommentUserImage)
        val userName: TextView = view.findViewById(R.id.tvCommentUserName)
        val timestamp: TextView = view.findViewById(R.id.tvCommentTimestamp)
        val content: TextView = view.findViewById(R.id.tvCommentContent)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        
        holder.userName.text = if (comment.userName.isNotBlank()) comment.userName else "Anonymous"
        holder.content.text = comment.content
        
        // Format timestamp using centralized utility
        holder.timestamp.text = FormatUtils.formatDateTime(comment.timestamp?.toDate())

        // Profile Picture handling
        if (comment.userProfilePicture.isNotBlank()) {
            Picasso.get()
                .load(comment.userProfilePicture)
                .placeholder(R.drawable.ic_account_circle)
                .transform(CircleTransform())
                .into(holder.userImage)
        } else {
            holder.userImage.setImageResource(R.drawable.ic_account_circle)
        }

        // Show delete button only for user's own comments
        if (comment.userId == currentUserId) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDeleteClick(comment) }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    override fun getItemCount() = comments.size

    /**
     * Updates the data set and refreshes the view.
     */
    fun updateComments(newComments: List<Comment>) {
        this.comments = newComments
        notifyDataSetChanged()
    }
}
