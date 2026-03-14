package com.example.marketwatch

import android.os.Bundle
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
import kotlinx.coroutines.CoroutineScope
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
        val view = inflater.inflate(R.layout.fragment_post_details, container, false)

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
        setupObservers(view)

        commentViewModel.setPostId(args.postId)
        loadPostDetails(view)

        return view
    }

    private fun initViews(view: View) {
        val toolbar = view.findViewById<Toolbar>(R.id.postDetailsToolbar)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val rvComments = view.findViewById<RecyclerView>(R.id.rvComments)
        rvComments.layoutManager = LinearLayoutManager(context)
        
        adapter = CommentAdapter(
            emptyList(),
            FirebaseAuth.getInstance().currentUser?.uid
        ) { comment ->
            commentViewModel.deleteComment(comment)
        }
        rvComments.adapter = adapter

        val etComment = view.findViewById<TextInputEditText>(R.id.etComment)
        val btnSend = view.findViewById<ImageButton>(R.id.btnSendComment)

        btnSend.setOnClickListener {
            val content = etComment.text.toString().trim()
            if (content.isNotEmpty()) {
                commentViewModel.addComment(content)
                etComment.text?.clear()
            }
        }
    }

    private fun setupObservers(view: View) {
        commentViewModel.comments.observe(viewLifecycleOwner) { comments ->
            adapter.updateComments(comments)
        }

        commentViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                commentViewModel.clearError()
            }
        }
    }

    private fun loadPostDetails(view: View) {
        CoroutineScope(Dispatchers.IO).launch {
            val post = postsRepository.getPostById(args.postId)
            withContext(Dispatchers.Main) {
                if (post != null && isAdded) {
                    val userImage = view.findViewById<ImageView>(R.id.postUserProfileImage)
                    val userName = view.findViewById<TextView>(R.id.postUserName)
                    val timestamp = view.findViewById<TextView>(R.id.postTimestamp)
                    val content = view.findViewById<TextView>(R.id.postContent)
                    val postImageCard = view.findViewById<View>(R.id.postImageCard)
                    val postImage = view.findViewById<ImageView>(R.id.postImage)
                    val tvLikeCount = view.findViewById<TextView>(R.id.tvLikeCount)
                    val btnLike = view.findViewById<ImageButton>(R.id.btnLike)

                    userName.text = post.userName
                    content.text = post.content
                    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    timestamp.text = post.timestamp?.toDate()?.let { sdf.format(it) } ?: "Just now"
                    tvLikeCount.text = "${post.likes.size} likes"

                    if (post.userProfilePicture.isNotBlank()) {
                        Picasso.get().load(post.userProfilePicture).transform(CircleTransform()).into(userImage)
                    }

                    if (!post.imageUrl.isNullOrEmpty()) {
                        postImageCard.visibility = View.VISIBLE
                        Picasso.get().load(post.imageUrl).into(postImage)
                    } else {
                        postImageCard.visibility = View.GONE
                    }
                    
                    val isLiked = post.likes.contains(FirebaseAuth.getInstance().currentUser?.uid)
                    btnLike.setImageResource(if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
                }
            }
        }
    }
}
