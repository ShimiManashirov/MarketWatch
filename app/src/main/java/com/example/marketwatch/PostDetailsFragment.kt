package com.example.marketwatch

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.marketwatch.data.CommentRepository
import com.example.marketwatch.data.PostsRepository
import com.example.marketwatch.data.local.AppDatabase
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fragment that displays the details of a single post and its comment thread.
 */
class PostDetailsFragment : Fragment() {

    private val args: PostDetailsFragmentArgs by navArgs()
    private lateinit var commentViewModel: CommentViewModel
    private lateinit var postsRepository: PostsRepository
    private lateinit var adapter: CommentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val localDb = AppDatabase.getDatabase(requireContext())
        
        postsRepository = PostsRepository(db, localDb, auth)
        val commentRepository = CommentRepository(db, auth)
        
        val factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CommentViewModel(commentRepository) as T
            }
        }
        commentViewModel = ViewModelProvider(this, factory).get(CommentViewModel::class.java)

        initViews(view)
        setupObservers()

        commentViewModel.setPostId(args.postId)
        loadPostDetails(view)
    }

    private fun initViews(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.postDetailsToolbar)
        toolbar?.setNavigationOnClickListener { findNavController().navigateUp() }

        val rvComments = view.findViewById<RecyclerView>(R.id.rvComments)
        rvComments?.layoutManager = LinearLayoutManager(context)
        
        adapter = CommentAdapter(
            emptyList(),
            FirebaseAuth.getInstance().currentUser?.uid
        ) { comment ->
            commentViewModel.deleteComment(comment)
        }
        rvComments?.adapter = adapter

        val etComment = view.findViewById<TextInputEditText>(R.id.etComment)
        val btnSend = view.findViewById<ImageButton>(R.id.btnSendComment)

        btnSend?.setOnClickListener {
            val content = etComment?.text?.toString()?.trim() ?: ""
            if (content.isNotEmpty()) {
                commentViewModel.addComment(content)
                etComment?.text?.clear()
            }
        }
    }

    private fun setupObservers() {
        commentViewModel.comments.observe(viewLifecycleOwner) { comments ->
            adapter.updateComments(comments)
        }

        commentViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                if (isAdded) {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    commentViewModel.clearError()
                }
            }
        }
    }

    private fun loadPostDetails(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val post = withContext(Dispatchers.IO) {
                    postsRepository.getPostById(args.postId)
                }
                
                if (post != null && isAdded) {
                    val userImage = view.findViewById<ImageView>(R.id.postUserProfileImage)
                    val userName = view.findViewById<TextView>(R.id.postUserName)
                    val timestamp = view.findViewById<TextView>(R.id.postTimestamp)
                    val content = view.findViewById<TextView>(R.id.postContent)
                    val postImageCard = view.findViewById<View>(R.id.postImageCard)
                    val postImage = view.findViewById<ImageView>(R.id.postImage)
                    val tvLikeCount = view.findViewById<TextView>(R.id.tvLikeCount)
                    val btnLike = view.findViewById<ImageButton>(R.id.btnLike)

                    userName?.text = post.userName
                    content?.text = post.content
                    
                    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    timestamp?.text = post.timestamp?.toDate()?.let { sdf.format(it) } ?: "Just now"
                    tvLikeCount?.text = "${post.likes.size} likes"

                    if (post.userProfilePicture.isNotBlank() && userImage != null) {
                        Picasso.get()
                            .load(post.userProfilePicture)
                            .placeholder(R.drawable.ic_account_circle)
                            .transform(CircleTransform())
                            .into(userImage)
                    }

                    if (!post.imageUrl.isNullOrEmpty() && postImage != null) {
                        postImageCard?.visibility = View.VISIBLE
                        Picasso.get().load(post.imageUrl).into(postImage)
                    } else {
                        postImageCard?.visibility = View.GONE
                    }
                    
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    val isLiked = post.likes.contains(currentUserId)
                    btnLike?.setImageResource(if (isLiked) android.R.drawable.star_big_on else android.R.drawable.star_big_off)
                }
            } catch (e: Exception) {
                Log.e("PostDetails", "Error loading post details", e)
            }
        }
    }
}
