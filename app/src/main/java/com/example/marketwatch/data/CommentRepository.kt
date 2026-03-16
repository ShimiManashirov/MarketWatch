package com.example.marketwatch.data

import com.example.marketwatch.Comment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository for handling comments on community posts.
 * Interacts with Firebase Firestore to store and retrieve comments in real-time.
 */
class CommentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * Retrieves a real-time flow of comments for a specific post.
     * @param postId The ID of the post to fetch comments for.
     */
    fun getCommentsForPost(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = db.collection("posts").document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }

                val comments = snapshots?.map { doc ->
                    Comment(
                        id = doc.id,
                        postId = postId,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "Unknown",
                        userProfilePicture = doc.getString("userProfilePicture") ?: "",
                        content = doc.getString("content") ?: "",
                        timestamp = doc.getTimestamp("timestamp")
                    )
                } ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Adds a new comment to a post.
     * Updates the post's comment count in a transaction.
     */
    suspend fun addComment(postId: String, content: String) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        val userId = user.uid
        
        // Fetch current user details
        val userDoc = db.collection("users").document(userId).get().await()
        val userName = userDoc.getString("name") ?: "Unknown"
        val profilePic = userDoc.getString("profilePictureUrl") ?: ""

        val commentData = hashMapOf(
            "userId" to userId,
            "userName" to userName,
            "userProfilePicture" to profilePic,
            "content" to content,
            "timestamp" to Timestamp.now()
        )

        val postRef = db.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentCount = snapshot.getLong("commentsCount") ?: 0L
            
            transaction.set(commentRef, commentData)
            transaction.update(postRef, "commentsCount", currentCount + 1)
        }.await()
    }

    /**
     * Deletes a comment from a post.
     * Decrements the post's comment count.
     */
    suspend fun deleteComment(postId: String, commentId: String) = withContext(Dispatchers.IO) {
        val postRef = db.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document(commentId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentCount = snapshot.getLong("commentsCount") ?: 0L
            
            transaction.delete(commentRef)
            if (currentCount > 0) {
                transaction.update(postRef, "commentsCount", currentCount - 1)
            }
        }.await()
    }
}
