package com.example.marketwatch.data

import android.net.Uri
import com.example.marketwatch.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
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
    }

    suspend fun updateProfilePicture(uri: Uri) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext
        db.collection("users").document(userId).update("profilePictureUrl", uri.toString()).await()
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
        for (doc in watchlist) {
            doc.reference.delete().await()
        }

        val transactions = userRef.collection("transactions").get().await()
        for (doc in transactions) {
            doc.reference.delete().await()
        }
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
