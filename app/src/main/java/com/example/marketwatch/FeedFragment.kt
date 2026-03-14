package com.example.marketwatch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Fragment displaying the community feed.
 * Allows users to view, like, and navigate to post details for commenting.
 */
class FeedFragment : Fragment() {

    private lateinit var viewModel: FeedViewModel
    private lateinit var adapter: PostsAdapter
    private val postsList = mutableListOf<Post>()

    private var shimmerContainer: ShimmerFrameLayout? = null
    private var recyclerView: RecyclerView? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        
        recyclerView?.layoutManager = LinearLayoutManager(context)

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
        recyclerView?.adapter = adapter

        view.findViewById<ExtendedFloatingActionButton>(R.id.fabAddPost)?.setOnClickListener {
            showCreatePostDialog()
        }

        swipeRefreshLayout?.setOnRefreshListener {
            viewModel.loadPosts()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postsList.clear()
            postsList.addAll(posts)
            adapter.notifyDataSetChanged()
            swipeRefreshLayout?.isRefreshing = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                if (swipeRefreshLayout?.isRefreshing == false) {
                    shimmerContainer?.startShimmer()
                    shimmerContainer?.visibility = View.VISIBLE
                    recyclerView?.visibility = View.GONE
                }
            } else {
                shimmerContainer?.stopShimmer()
                shimmerContainer?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE
                swipeRefreshLayout?.isRefreshing = false
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                if (isAdded) Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
                swipeRefreshLayout?.isRefreshing = false
            }
        }
    }

    private fun showCreatePostDialog() {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_post, null)
        val postEditText = dialogView.findViewById<EditText>(R.id.dialogPostEditText)

        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Post") { _, _ ->
                val content = postEditText.text.toString().trim()
                if (content.isNotEmpty()) {
                    viewModel.createPost(content, null)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditPostDialog(post: Post) {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_post, null)
        val postEditText = dialogView.findViewById<EditText>(R.id.dialogPostEditText)
        
        postEditText.setText(post.content)

        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newContent = postEditText.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    viewModel.updatePost(post.id, newContent, null)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(post: Post) {
        val context = context ?: return
        AlertDialog.Builder(context)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePost(post.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
