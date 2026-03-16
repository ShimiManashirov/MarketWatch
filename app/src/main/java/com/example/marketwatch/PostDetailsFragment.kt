package com.example.marketwatch

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

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

        // Fix: Use generic EditText to prevent ClassCastException
        val etComment = view.findViewById<EditText>(R.id.etComment)
        val btnSend = view.findViewById<View>(R.id.btnSendComment)

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
                    Toast.makeText(context, "Error: Check your connection or permissions", Toast.LENGTH_SHORT).show()
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
                    val userName = view.findViewById<TextView>(R.id.postUserName)
                    val content = view.findViewById<TextView>(R.id.postContent)
                    val tvLikeCount = view.findViewById<TextView>(R.id.tvLikeCount)
                    val userImage = view.findViewById<ImageView>(R.id.postUserProfileImage)
                    val postImage = view.findViewById<ImageView>(R.id.postImage)
                    val btnLike = view.findViewById<View>(R.id.btnLike)

                    userName?.text = post.userName
                    content?.text = post.content
                    tvLikeCount?.text = "${post.likes.size} LIKES"

                    if (post.userProfilePicture.isNotBlank() && userImage != null) {
                        Picasso.get().load(post.userProfilePicture).transform(CircleTransform()).into(userImage)
                    }

                    if (!post.imageUrl.isNullOrEmpty() && postImage != null) {
                        view.findViewById<View>(R.id.postImageCard)?.visibility = View.VISIBLE
                        Picasso.get().load(post.imageUrl).into(postImage)
                    }
                    
                    if (btnLike is ImageView) {
                        val isLiked = post.likes.contains(FirebaseAuth.getInstance().currentUser?.uid)
                        btnLike.setImageResource(if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
                    }
                }
            } catch (e: Exception) {
                Log.e("PostDetails", "Error loading details", e)
            }
        }
    }
}
