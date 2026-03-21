package com.example.marketwatch

import android.app.Activity
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.marketwatch.data.PostsRepository
import com.example.marketwatch.data.local.AppDatabase
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.io.File

class FeedFragment : Fragment() {

    private lateinit var viewModel: FeedViewModel
    private lateinit var adapter: PostsAdapter
    private val postsList = mutableListOf<Post>()

    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var selectedImageUri: Uri? = null
    private var dialogImageView: ImageView? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            dialogImageView?.let { imageView ->
                try {
                    if (selectedImageUri == null) return@registerForActivityResult
                    
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, selectedImageUri!!))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(requireContext().contentResolver, selectedImageUri)
                    }
                    
                    val savedPath = ImageManager.saveBitmapLocally(requireContext(), bitmap)
                    selectedImageUri = Uri.fromFile(File(savedPath))
                    
                    imageView.visibility = View.VISIBLE
                    ImageManager.loadImage(imageView, savedPath, isCircle = false)
                    
                } catch (e: Exception) {
                    Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)

        val repository = PostsRepository(
            FirebaseFirestore.getInstance(),
            AppDatabase.getDatabase(requireContext()),
            FirebaseAuth.getInstance()
        )
        val factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return FeedViewModel(repository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory).get(FeedViewModel::class.java)

        shimmerContainer = view.findViewById(R.id.shimmerViewContainer)
        recyclerView = view.findViewById(R.id.postsRecyclerView)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = PostsAdapter(
            postsList,
            FirebaseAuth.getInstance().currentUser?.uid,
            onEditClick = { post -> showEditPostDialog(post) },
            onDeleteClick = { post -> showDeleteConfirmationDialog(post) },
            onLikeClick = { post -> viewModel.toggleLike(post) },
            onCommentClick = { post -> 
                val action = FeedFragmentDirections.actionFeedToPostDetails(post.id)
                findNavController().navigate(action)
            }
        )
        recyclerView.adapter = adapter

        view.findViewById<ExtendedFloatingActionButton>(R.id.fabAddPost).setOnClickListener {
            showCreatePostDialog()
        }

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadPosts()
        }

        observeViewModel()

        return view
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postsList.clear()
            postsList.addAll(posts)
            adapter.notifyDataSetChanged()
            swipeRefreshLayout.isRefreshing = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                if (!swipeRefreshLayout.isRefreshing) {
                    shimmerContainer.startShimmer()
                    shimmerContainer.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
            } else {
                shimmerContainer.stopShimmer()
                shimmerContainer.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                swipeRefreshLayout.isRefreshing = false
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun showCreatePostDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_post, null)
        val postEditText = dialogView.findViewById<EditText>(R.id.dialogPostEditText)
        val addImageBtn = dialogView.findViewById<ImageButton>(R.id.dialogAddImageBtn)
        val showUrlBtn = dialogView.findViewById<ImageButton>(R.id.dialogShowUrlBtn)
        val urlInputLayout = dialogView.findViewById<TextInputLayout>(R.id.dialogUrlInputLayout)
        val imageUrlEditText = dialogView.findViewById<EditText>(R.id.dialogImageUrlEditText)
        dialogImageView = dialogView.findViewById(R.id.dialogPostImageView)

        selectedImageUri = null 

        addImageBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        showUrlBtn.setOnClickListener {
            urlInputLayout.visibility = if (urlInputLayout.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Post") { _, _ ->
                val content = postEditText.text.toString().trim()
                val manualUrl = imageUrlEditText.text.toString().trim()
                
                if (content.isNotEmpty()) {
                    if (manualUrl.isNotEmpty()) {
                        viewModel.createPost(requireContext(), content, Uri.parse(manualUrl))
                    } else {
                        viewModel.createPost(requireContext(), content, selectedImageUri)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
                val manualUrl = imageUrlEditText.text.toString().trim()
                
                if (newContent.isNotEmpty()) {
                    val finalUri = if (manualUrl.isNotEmpty()) Uri.parse(manualUrl) else selectedImageUri
                    viewModel.updatePost(requireContext(), post.id, newContent, finalUri)
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
                viewModel.deletePost(post.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
