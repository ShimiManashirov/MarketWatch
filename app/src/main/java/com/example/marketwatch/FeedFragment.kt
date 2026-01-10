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
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.marketwatch.data.local.AppDatabase
import com.example.marketwatch.data.local.PostEntity
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var localDb: AppDatabase
    private lateinit var adapter: PostsAdapter
    private val postsList = mutableListOf<Post>()
    
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var recyclerView: RecyclerView
    
    private var selectedImageUri: Uri? = null
    private var dialogImageView: ImageView? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            dialogImageView?.let {
                it.visibility = View.VISIBLE
                Picasso.get().load(selectedImageUri).into(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        localDb = AppDatabase.getDatabase(requireContext())

        shimmerContainer = view.findViewById(R.id.shimmerViewContainer)
        recyclerView = view.findViewById(R.id.postsRecyclerView)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        adapter = PostsAdapter(postsList, 
            onEditClick = { post -> showEditPostDialog(post) },
            onDeleteClick = { post -> showDeleteConfirmationDialog(post) }
        )
        recyclerView.adapter = adapter

        view.findViewById<ExtendedFloatingActionButton>(R.id.fabAddPost).setOnClickListener {
            showCreatePostDialog()
        }

        observeLocalPosts()
        loadPostsFromFirebase()

        return view
    }

    private fun observeLocalPosts() {
        // Load from Room immediately
        lifecycleScope.launch {
            localDb.postDao().getAllPosts().collect { entities ->
                if (postsList.isEmpty() && entities.isNotEmpty()) {
                    postsList.clear()
                    postsList.addAll(entities.map { it.toPost() })
                    adapter.notifyDataSetChanged()
                    shimmerContainer.stopShimmer()
                    shimmerContainer.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun loadPostsFromFirebase() {
        if (postsList.isEmpty()) {
            shimmerContainer.startShimmer()
            shimmerContainer.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }

        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (!isAdded) return@addSnapshotListener
                
                shimmerContainer.stopShimmer()
                shimmerContainer.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                if (e != null) {
                    Log.e("FeedFragment", "Firebase Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val firebasePosts = mutableListOf<Post>()
                    val localEntities = mutableListOf<PostEntity>()

                    for (doc in snapshots) {
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
                            firebasePosts.add(post)
                            localEntities.add(post.toEntity())
                        } catch (ex: Exception) {
                            Log.e("FeedFragment", "Error parsing post", ex)
                        }
                    }
                    
                    postsList.clear()
                    postsList.addAll(firebasePosts)
                    adapter.notifyDataSetChanged()

                    // Update local cache
                    lifecycleScope.launch(Dispatchers.IO) {
                        localDb.postDao().deleteAll()
                        localDb.postDao().insertPosts(localEntities)
                    }
                }
            }
    }

    // Helper extensions to convert between Post and PostEntity
    private fun Post.toEntity() = PostEntity(
        id = id,
        userId = userId,
        userName = userName,
        userProfilePicture = userProfilePicture,
        content = content,
        imageUrl = imageUrl,
        timestamp = timestamp?.seconds ?: 0L,
        likesCount = likes.size
    )

    private fun PostEntity.toPost() = Post(
        id = id,
        userId = userId,
        userName = userName,
        userProfilePicture = userProfilePicture,
        content = content,
        imageUrl = imageUrl,
        timestamp = Timestamp(timestamp, 0),
        likes = emptyList() // Room doesn't store UIDs list easily, showing count is enough for cache
    )

    private fun showCreatePostDialog() {
        if (!isAdded) return
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_post, null)
        val postEditText = dialogView.findViewById<EditText>(R.id.dialogPostEditText)
        val addImageBtn = dialogView.findViewById<View>(R.id.dialogAddImageBtn)
        dialogImageView = dialogView.findViewById(R.id.dialogPostImageView)
        
        selectedImageUri = null 

        addImageBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Create Post")
            .setView(dialogView)
            .setPositiveButton("Post") { _, _ ->
                val content = postEditText.text.toString().trim()
                if (content.isNotEmpty()) {
                    uploadPost(content, selectedImageUri)
                } else {
                    Toast.makeText(context, "Post content cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditPostDialog(post: Post) {
        if (!isAdded) return
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_post, null)
        val postEditText = dialogView.findViewById<EditText>(R.id.dialogPostEditText)
        val addImageBtn = dialogView.findViewById<View>(R.id.dialogAddImageBtn)
        dialogImageView = dialogView.findViewById(R.id.dialogPostImageView)
        
        postEditText.setText(post.content)
        selectedImageUri = post.imageUrl?.let { Uri.parse(it) }
        if (selectedImageUri != null) {
            dialogImageView?.visibility = View.VISIBLE
            Picasso.get().load(selectedImageUri).into(dialogImageView!!)
        }

        addImageBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Post")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newContent = postEditText.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    updatePost(post.id, newContent, selectedImageUri)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePost(postId: String, content: String, imageUri: Uri?) {
        val updates = hashMapOf<String, Any>(
            "content" to content,
            "imageUrl" to (imageUri?.toString() ?: "")
        )
        
        db.collection("posts").document(postId).update(updates)
            .addOnSuccessListener {
                if (isAdded) Toast.makeText(context, "Post updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
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

    private fun uploadPost(content: String, imageUri: Uri?) {
        val user = auth.currentUser ?: return
        val userId = user.uid
        
        db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            if (!isAdded) return@addOnSuccessListener
            val userName = userDoc.getString("name") ?: "Unknown User"
            val profilePic = userDoc.getString("profilePictureUrl") ?: ""
            
            val postData = hashMapOf(
                "userId" to userId,
                "userName" to userName,
                "userProfilePicture" to profilePic,
                "content" to content,
                "imageUrl" to imageUri?.toString(), 
                "timestamp" to Timestamp.now(),
                "likes" to emptyList<String>()
            )

            db.collection("posts").add(postData)
                .addOnSuccessListener {
                    if (isAdded) Toast.makeText(context, "Post published!", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
