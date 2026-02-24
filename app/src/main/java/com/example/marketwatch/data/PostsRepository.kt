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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PostsRepository(
    private val db: FirebaseFirestore,
    private val localDb: AppDatabase,
    private val auth: FirebaseAuth
) {

    private val postDao = localDb.postDao()

    val allPosts: Flow<List<Post>> = postDao.getAllPosts().map { entities ->
        entities.map { it.toPost() }
    }

    suspend fun refreshPosts() {
        try {
            val snapshots = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val firebasePosts = snapshots.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("PostsRepository", "Error parsing post", e)
                    null
                }
            }

            withContext(Dispatchers.IO) {
                postDao.deleteAll()
                postDao.insertPosts(firebasePosts.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Log.e("PostsRepository", "Error fetching posts from Firebase", e)
        }
    }

    suspend fun createPost(content: String, imageUri: Uri?) {
        val user = auth.currentUser ?: return
        val userDoc = db.collection("users").document(user.uid).get().await()
        val userName = userDoc.getString("name") ?: "Unknown User"
        val profilePic = userDoc.getString("profilePictureUrl") ?: ""

        val postData = hashMapOf(
            "userId" to user.uid,
            "userName" to userName,
            "userProfilePicture" to profilePic,
            "content" to content,
            "imageUrl" to imageUri?.toString(),
            "timestamp" to Timestamp.now(),
            "likes" to emptyList<String>()
        )

        db.collection("posts").add(postData).await()
        refreshPosts()
    }
    
    suspend fun updatePost(postId: String, content: String, imageUri: Uri?) {
        val updates = hashMapOf<String, Any>(
            "content" to content,
            "imageUrl" to (imageUri?.toString() ?: "")
        )
        db.collection("posts").document(postId).update(updates).await()
        refreshPosts()
    }

    suspend fun deletePost(postId: String) {
        db.collection("posts").document(postId).delete().await()
        refreshPosts()
    }

    suspend fun toggleLike(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = db.collection("posts").document(postId)

        db.runTransaction {
            transaction ->
            val snapshot = transaction.get(postRef)
            val likes = snapshot.get("likes") as? List<String> ?: emptyList()
            if (likes.contains(userId)) {
                transaction.update(postRef, "likes", FieldValue.arrayRemove(userId))
            } else {
                transaction.update(postRef, "likes", FieldValue.arrayUnion(userId))
            }
            null
        }.await()
        refreshPosts()
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
