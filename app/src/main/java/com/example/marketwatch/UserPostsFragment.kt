package com.example.marketwatch

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserPostsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: PostsAdapter
    private val userPostsList = mutableListOf<Post>()
    private lateinit var emptyLayout: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_user_posts, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val toolbar = view.findViewById<Toolbar>(R.id.userPostsToolbar)
        emptyLayout = view.findViewById(R.id.emptyUserPostsLayout)
        val recyclerView = view.findViewById<RecyclerView>(R.id.userPostsRecyclerView)

        toolbar.setNavigationOnClickListener { 
            findNavController().navigateUp()
        }
        toolbar.title = "My Activity"

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = PostsAdapter(userPostsList,
            auth.currentUser?.uid,
            onEditClick = { post -> showEditPostDialog(post) },
            onDeleteClick = { post -> showDeleteConfirmationDialog(post) },
            onLikeClick = { post -> toggleLike(post) },
            onCommentClick = { post ->
                val action = UserPostsFragmentDirections.actionUserPostsToPostDetails(post.id)
                findNavController().navigate(action)
            }
        )
        recyclerView.adapter = adapter

        loadUserPosts()
        
        return view
    }

    private fun toggleLike(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = db.collection("posts").document(post.id)
        val isLiked = post.likes.contains(userId)
        
        if (isLiked) {
            postRef.update("likes", FieldValue.arrayRemove(userId))
        } else {
            postRef.update("likes", FieldValue.arrayUnion(userId))
        }
    }

    private fun loadUserPosts() {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("posts")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (!isAdded) return@addSnapshotListener
                if (e != null) {
                    Log.e("UserPosts", "Listen failed.", e)
                    return@addSnapshotListener
                }

                userPostsList.clear()
                snapshots?.forEach { doc ->
                    try {
                        val likesList = doc.get("likes") as? List<String> ?: emptyList()
                        val post = Post(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "Unknown",
                            userProfilePicture = doc.getString("userProfilePicture") ?: "",
                            content = doc.getString("content") ?: "",
                            imageUrl = doc.getString("imageUrl"),
                            timestamp = doc.getTimestamp("timestamp"),
                            likes = likesList
                        )
                        userPostsList.add(post)
                    } catch (ex: Exception) {
                        Log.e("UserPosts", "Error parsing post", ex)
                    }
                }
                adapter.notifyDataSetChanged()
                emptyLayout.visibility = if (userPostsList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun showEditPostDialog(post: Post) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_post, null)
        val postEditText = dialogView.findViewById<EditText>(R.id.dialogPostEditText)

        postEditText.setText(post.content)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Post")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newContent = postEditText.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    updatePost(post.id, newContent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePost(postId: String, content: String) {
        db.collection("posts").document(postId)
            .update("content", content)
            .addOnSuccessListener {
                if (isAdded) Toast.makeText(context, "Post updated", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog(post: Post) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("posts").document(post.id).delete()
                    .addOnSuccessListener {
                        if (isAdded) Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
