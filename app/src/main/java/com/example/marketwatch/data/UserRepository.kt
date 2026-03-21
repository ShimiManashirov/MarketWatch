package com.example.marketwatch.data

import android.net.Uri
import com.example.marketwatch.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class UserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    fun getUserProfile(): Flow<User?> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val user = User(
                        uid = snapshot.id,
                        email = auth.currentUser?.email ?: "",
                        name = snapshot.getString("name") ?: "",
                        profilePictureUrl = snapshot.getString("profilePictureUrl"),
                        currency = snapshot.getString("currency") ?: "USD",
                        timezone = snapshot.getString("timezone") ?: "UTC"
                    )
                    trySend(user)
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateName(newName: String) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        db.collection("users").document(userId).update("name", newName).await()
        updateUserInfoInPosts(userId, newName, null)
    }

    suspend fun uploadProfilePicture(imageBytes: ByteArray) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        val fileName = "profile_pics/$userId/profile.jpg"
        val ref = storage.reference.child(fileName)
        
        ref.putBytes(imageBytes).await()
        
        // Add timestamp to URL to bust cache everywhere (Feed, Profile, etc.)
        val downloadUrl = ref.downloadUrl.await().toString() + "?v=" + System.currentTimeMillis()
        
        db.collection("users").document(userId).update("profilePictureUrl", downloadUrl).await()
        updateUserInfoInPosts(userId, null, downloadUrl)
    }

    suspend fun updateProfilePictureUrl(url: String) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        db.collection("users").document(userId).update("profilePictureUrl", url).await()
        updateUserInfoInPosts(userId, null, url)
    }

    private suspend fun updateUserInfoInPosts(userId: String, newName: String?, newProfilePic: String?) {
        val postsQuery = db.collection("posts").whereEqualTo("userId", userId).get().await()
        if (postsQuery.isEmpty) return

        val batch = db.batch()
        for (doc in postsQuery.documents) {
            val updates = mutableMapOf<String, Any>()
            if (newName != null) updates["userName"] = newName
            if (newProfilePic != null) updates["userProfilePicture"] = newProfilePic
            batch.update(doc.reference, updates)
        }
        batch.commit().await()
    }

    suspend fun updateCurrency(currencyCode: String) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        db.collection("users").document(userId).update("currency", currencyCode).await()
    }

    suspend fun updateTimezone(timezoneId: String) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        db.collection("users").document(userId).update("timezone", timezoneId).await()
    }

    suspend fun resetWalletData() = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        val userRef = db.collection("users").document(userId)
        userRef.update("balance", 0.0).await()

        val watchlist = userRef.collection("watchlist").get().await()
        for (doc in watchlist) doc.reference.delete().await()

        val transactions = userRef.collection("transactions").get().await()
        for (doc in transactions) doc.reference.delete().await()
    }

    suspend fun deleteAccount(password: String) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        val credential = EmailAuthProvider.getCredential(user.email!!, password)
        user.reauthenticate(credential).await()
        val userId = user.uid
        db.collection("users").document(userId).delete().await()
        user.delete().await()
    }

    suspend fun updatePassword(currentPwd: String, newPwd: String) = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPwd)
        user.reauthenticate(credential).await()
        user.updatePassword(newPwd).await()
    }
}
