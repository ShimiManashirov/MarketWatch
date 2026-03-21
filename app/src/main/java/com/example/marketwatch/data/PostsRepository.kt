package com.example.marketwatch.data

import android.util.Log
import com.example.marketwatch.Post
import com.example.marketwatch.data.local.AppDatabase
import com.example.marketwatch.data.local.PostEntity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class PostsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val localDb: AppDatabase,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
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
                        val commentsCount = (doc.get("commentsCount") as? Number)?.toInt() ?: 0
                        val post = Post(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "Unknown",
                            userProfilePicture = doc.getString("userProfilePicture") ?: "",
                            content = doc.getString("content") ?: "",
                            imageUrl = doc.getString("imageUrl"),
                            timestamp = doc.getTimestamp("timestamp"),
                            likes = likesList,
                            commentsCount = commentsCount
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

    suspend fun getPostById(postId: String): Post? = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection("posts").document(postId).get().await()
            if (doc.exists()) {
                val likesList = doc.get("likes") as? List<String> ?: emptyList()
                val commentsCount = (doc.get("commentsCount") as? Number)?.toInt() ?: 0
                Post(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    userName = doc.getString("userName") ?: "Unknown",
                    userProfilePicture = doc.getString("userProfilePicture") ?: "",
                    content = doc.getString("content") ?: "",
                    imageUrl = doc.getString("imageUrl"),
                    timestamp = doc.getTimestamp("timestamp"),
                    likes = likesList,
                    commentsCount = commentsCount
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates a new post with optional image bytes
     */
    suspend fun createPost(content: String, imageBytes: ByteArray?) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        val userId = user.uid
        
        val userDoc = db.collection("users").document(userId).get().await()
        val userName = userDoc.getString("name") ?: "Unknown User"
        val profilePic = userDoc.getString("profilePictureUrl") ?: ""

        var uploadedImageUrl: String? = null
        if (imageBytes != null) {
            val fileName = "post_images/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(fileName)
            ref.putBytes(imageBytes).await()
            uploadedImageUrl = ref.downloadUrl.await().toString()
        }
        
        val postData = hashMapOf(
            "userId" to userId,
            "userName" to userName,
            "userProfilePicture" to profilePic,
            "content" to content,
            "imageUrl" to uploadedImageUrl,
            "timestamp" to Timestamp.now(),
            "likes" to emptyList<String>(),
            "commentsCount" to 0
        )

        db.collection("posts").add(postData).await()
    }

    suspend fun deletePost(postId: String) = withContext(Dispatchers.IO) {
        // First get the post to see if it has an image to delete from storage
        val doc = db.collection("posts").document(postId).get().await()
        val imageUrl = doc.getString("imageUrl")
        
        if (!imageUrl.isNullOrEmpty() && imageUrl.contains("firebasestorage")) {
            try {
                storage.getReferenceFromUrl(imageUrl).delete().await()
            } catch (e: Exception) {
                Log.e("PostsRepository", "Failed to delete storage image", e)
            }
        }

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
        timestamp = timestamp?.let { Timestamp(it, 0) },
        likes = emptyList()
    )
}
