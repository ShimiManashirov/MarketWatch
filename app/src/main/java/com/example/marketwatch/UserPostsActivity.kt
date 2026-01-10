package com.example.marketwatch

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserPostsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: PostsAdapter
    private val userPostsList = mutableListOf<Post>()
    private lateinit var emptyLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_posts)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val toolbar = findViewById<Toolbar>(R.id.userPostsToolbar)
        emptyLayout = findViewById(R.id.emptyUserPostsLayout)
        val recyclerView = findViewById<RecyclerView>(R.id.userPostsRecyclerView)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PostsAdapter(userPostsList,
            onEditClick = { post -> showEditPostDialog(post) },
            onDeleteClick = { post -> showDeleteConfirmationDialog(post) }
        )
        recyclerView.adapter = adapter

        loadUserPosts()
    }

    private fun loadUserPosts() {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("posts")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_post, null)
        val postEditText = dialogView.findViewById<EditText>(R.id.dialogPostEditText)
        val dialogImageView = dialogView.findViewById<ImageView>(R.id.dialogPostImageView)
        dialogView.findViewById<View>(R.id.dialogAddImageBtn).visibility = View.GONE 

        postEditText.setText(post.content)
        if (!post.imageUrl.isNullOrEmpty()) {
            dialogImageView.visibility = View.VISIBLE
            // Fixed: Replaced Glide with Picasso
            Picasso.get().load(post.imageUrl).into(dialogImageView)
        }

        AlertDialog.Builder(this)
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
                Toast.makeText(this, "Post updated", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog(post: Post) {
        AlertDialog.Builder(this)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("posts").document(post.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Post deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
