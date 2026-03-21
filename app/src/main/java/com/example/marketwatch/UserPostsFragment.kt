package com.example.marketwatch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.marketwatch.data.PostsRepository
import com.example.marketwatch.data.local.AppDatabase
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class UserPostsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var repository: PostsRepository
    private lateinit var adapter: PostsAdapter
    private val userPostsList = mutableListOf<Post>()
    
    private lateinit var emptyLayout: View
    private lateinit var swipeRefresh: SwipeRefreshLayout
    
    private var selectedImageUri: Uri? = null
    private var dialogImageView: ImageView? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            dialogImageView?.let { imageView ->
                imageView.visibility = View.VISIBLE
                Picasso.get().load(selectedImageUri).into(imageView)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_user_posts, container, false)

        auth = FirebaseAuth.getInstance()
        repository = PostsRepository(
            FirebaseFirestore.getInstance(),
            AppDatabase.getDatabase(requireContext()),
            auth
        )

        val toolbar = view.findViewById<Toolbar>(R.id.userPostsToolbar)
        emptyLayout = view.findViewById(R.id.emptyUserPostsLayout)
        swipeRefresh = view.findViewById(R.id.userPostsSwipeRefresh)
        val recyclerView = view.findViewById<RecyclerView>(R.id.userPostsRecyclerView)

        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

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

        swipeRefresh.setOnRefreshListener { loadUserPosts() }

        loadUserPosts()
        
        return view
    }

    private fun toggleLike(post: Post) {
        lifecycleScope.launch {
            try {
                repository.toggleLike(post)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to like post", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserPosts() {
        val userId = auth.currentUser?.uid ?: return
        swipeRefresh.isRefreshing = true
        
        // Use a simpler query first to check if it's an Index issue
        FirebaseFirestore.getInstance().collection("posts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshots ->
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
                
                // Sort manually in Kotlin to avoid Index requirement for now
                userPostsList.sortByDescending { it.timestamp }
                
                adapter.notifyDataSetChanged()
                emptyLayout.visibility = if (userPostsList.isEmpty()) View.VISIBLE else View.GONE
                swipeRefresh.isRefreshing = false
            }
            .addOnFailureListener { e ->
                Log.e("UserPosts", "Load failed: ${e.message}", e)
                Toast.makeText(context, "Failed to load posts", Toast.LENGTH_SHORT).show()
                swipeRefresh.isRefreshing = false
            }
    }

    private fun showEditPostDialog(post: Post) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_post, null)
        val postEditText = dialogView.findViewById<EditText>(R.id.dialogPostEditText)
        val addImageBtn = dialogView.findViewById<ImageButton>(R.id.dialogAddImageBtn)
        val showUrlBtn = dialogView.findViewById<ImageButton>(R.id.dialogShowUrlBtn)
        val urlInputLayout = dialogView.findViewById<TextInputLayout>(R.id.dialogUrlInputLayout)
        val imageUrlEditText = dialogView.findViewById<EditText>(R.id.dialogImageUrlEditText)
        dialogImageView = dialogView.findViewById(R.id.dialogPostImageView)
        
        postEditText.setText(post.content)
        if (!post.imageUrl.isNullOrEmpty()) {
            dialogImageView?.visibility = View.VISIBLE
            Picasso.get().load(post.imageUrl).into(dialogImageView!!)
            if (post.imageUrl.startsWith("http")) {
                urlInputLayout.visibility = View.VISIBLE
                imageUrlEditText.setText(post.imageUrl)
            }
        }

        addImageBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        showUrlBtn.setOnClickListener {
            urlInputLayout.visibility = if (urlInputLayout.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Post")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newContent = postEditText.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    FirebaseFirestore.getInstance().collection("posts").document(post.id)
                        .update("content", newContent)
                        .addOnSuccessListener { loadUserPosts() }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(post: Post) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deletePost(post.id)
                        loadUserPosts()
                        Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to delete post", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
