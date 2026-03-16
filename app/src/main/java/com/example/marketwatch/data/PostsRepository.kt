package com.example.marketwatch.data

import android.net.Uri
import android.util.Log
import com.example.marketwatch.Post
import com.example.marketwatch.data.local.AppDatabase
import com.example.marketwatch.data.local.PostEntity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PostsRepository(
    private val db: FirebaseFirestore,
    private val localDb: AppDatabase,
    private val auth: FirebaseAuth
) {

    fun getLocalPosts(): Flow<List<Post>> {
        return localDb.postDao().getAllPosts().map { entities ->
            entities.map { it.toPost() }
        }
    }

    fun getRemotePostsFlow(): Flow<List<Post>> = callbackFlow {
        val listener = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }

                val posts = mutableListOf<Post>()
                val entities = mutableListOf<PostEntity>()
                
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
                        posts.add(post)
                        entities.add(post.toEntity())
                    } catch (ex: Exception) {
                        Log.e("PostsRepository", "Error parsing post", ex)
                    }
                }
                
                launch(Dispatchers.IO) {
                    localDb.postDao().deleteAll()
                    localDb.postDao().insertPosts(entities)
                }
                
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Fetches a single post by its ID from Firestore.
     */
    suspend fun getPostById(postId: String): Post? = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection("posts").document(postId).get().await()
            if (doc.exists()) {
                val likesList = doc.get("likes") as? List<String> ?: emptyList()
                Post(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    userName = doc.getString("userName") ?: "Unknown",
                    userProfilePicture = doc.getString("userProfilePicture") ?: "",
                    content = doc.getString("content") ?: "",
                    imageUrl = doc.getString("imageUrl"),
                    timestamp = doc.getTimestamp("timestamp"),
                    likes = likesList
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createPost(content: String, imageUri: Uri?) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        val userId = user.uid
        
        val userDoc = db.collection("users").document(userId).get().await()
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

        db.collection("posts").add(postData).await()
    }

    suspend fun updatePost(postId: String, content: String, imageUri: Uri?) = withContext(Dispatchers.IO) {
        val updates = hashMapOf<String, Any>(
            "content" to content,
            "imageUrl" to (imageUri?.toString() ?: "")
        )
        db.collection("posts").document(postId).update(updates).await()
    }

    suspend fun deletePost(postId: String) = withContext(Dispatchers.IO) {
        db.collection("posts").document(postId).delete().await()
    }

    suspend fun toggleLike(post: Post) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        val postRef = db.collection("posts").document(post.id)
        val isLiked = post.likes.contains(userId)
        
        if (isLiked) {
            postRef.update("likes", FieldValue.arrayRemove(userId)).await()
        } else {
            postRef.update("likes", FieldValue.arrayUnion(userId)).await()
        }
    }

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
        likes = emptyList()
    )
}
