package com.example.marketwatch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PostsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: PostsAdapter
    private val postsList = mutableListOf<Post>()
    
    private var selectedImageUri: Uri? = null
    private var dialogImageView: ImageView? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            dialogImageView?.let {
                it.visibility = View.VISIBLE
                it.setImageURI(selectedImageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_posts)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupBottomNavigation()
        
        findViewById<FloatingActionButton>(R.id.fabAddPost).setOnClickListener {
            showCreatePostDialog()
        }

        loadPosts()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.postsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PostsAdapter(postsList, 
            onEditClick = { post -> showEditPostDialog(post) },
            onDeleteClick = { post -> showDeleteConfirmationDialog(post) }
        )
        recyclerView.adapter = adapter
    }

    private fun loadPosts() {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading posts", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                postsList.clear()
                snapshots?.forEach { doc ->
                    val post = doc.toObject(Post::class.java).copy(id = doc.id)
                    postsList.add(post)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showCreatePostDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_post, null)
        val postEditText = dialogView.findViewById<EditText>(R.id.dialogPostEditText)
        val addImageBtn = dialogView.findViewById<View>(R.id.dialogAddImageBtn)
        dialogImageView = dialogView.findViewById(R.id.dialogPostImageView)
        
        selectedImageUri = null // Reset

        addImageBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        AlertDialog.Builder(this)
            .setTitle("Create Post")
            .setView(dialogView)
            .setPositiveButton("Post") { _, _ ->
                val content = postEditText.text.toString().trim()
                if (content.isNotEmpty()) {
                    uploadPost(content, selectedImageUri)
                } else {
                    Toast.makeText(this, "Post content cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditPostDialog(post: Post) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_post, null)
        val postEditText = dialogView.findViewById<EditText>(R.id.dialogPostEditText)
        val addImageBtn = dialogView.findViewById<View>(R.id.dialogAddImageBtn)
        dialogImageView = dialogView.findViewById(R.id.dialogPostImageView)
        
        postEditText.setText(post.content)
        selectedImageUri = post.imageUrl?.let { Uri.parse(it) }
        
        if (selectedImageUri != null) {
            dialogImageView?.visibility = View.VISIBLE
            Glide.with(this).load(selectedImageUri).into(dialogImageView!!)
        }

        addImageBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        AlertDialog.Builder(this)
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
                Toast.makeText(this, "Post updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
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

    private fun uploadPost(content: String, imageUri: Uri?) {
        val user = auth.currentUser ?: return
        val userId = user.uid
        
        db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            val userName = userDoc.getString("name") ?: "Unknown User"
            val profilePic = userDoc.getString("profilePictureUrl") ?: ""
            
            val postData = hashMapOf(
                "userId" to userId,
                "userName" to userName,
                "userProfilePicture" to profilePic,
                "content" to content,
                "imageUrl" to imageUri?.toString(), 
                "timestamp" to Timestamp.now()
            )

            db.collection("posts").add(postData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Post published!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to publish post", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupBottomNavigation() {
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.selectedItemId = R.id.navigation_feed
        navView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_feed -> true
                R.id.navigation_search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_portfolio -> {
                    startActivity(Intent(this, PortfolioActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
