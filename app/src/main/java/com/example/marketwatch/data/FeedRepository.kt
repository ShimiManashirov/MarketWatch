package com.example.marketwatch.data

import android.net.Uri
import android.util.Log
import com.example.marketwatch.Post
import com.example.marketwatch.data.local.AppDatabase
import com.example.marketwatch.data.local.PostEntity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FeedRepository(
    private val db: FirebaseFirestore,
    private val localDb: AppDatabase,
    private val auth: FirebaseAuth
) {

    fun getPosts(): Flow<List<Post>> {
        val localPosts = localDb.postDao().getAllPosts().map { entities ->
            entities.map { it.toPost() }
        }

        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FeedRepository", "Listen failed.", e)
                    return@addSnapshotListener
                }

                GlobalScope.launch(Dispatchers.IO) {
                    val firebasePosts = mutableListOf<Post>()
                    val localEntities = mutableListOf<PostEntity>()
                    for (doc in snapshots!!) {
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
                            Log.e("FeedRepository", "Error parsing post", ex)
                        }
                    }
                    localDb.postDao().deleteAll()
                    localDb.postDao().insertPosts(localEntities)
                }
            }
        return localPosts
    }

    suspend fun createPost(content: String, imageUri: Uri?) {
        val user = auth.currentUser ?: return
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

    suspend fun updatePost(postId: String, content: String, imageUri: Uri?) {
        val updates = hashMapOf<String, Any>(
            "content" to content,
            "imageUrl" to (imageUri?.toString() ?: "")
        )
        db.collection("posts").document(postId).update(updates).await()
    }

    suspend fun deletePost(postId: String) {
        db.collection("posts").document(postId).delete().await()
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