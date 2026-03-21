package com.example.marketwatch.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun signIn(email: String, password: String): Result<FirebaseUser?> = withContext(Dispatchers.IO) {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(name: String, email: String, password: String): Result<FirebaseUser?> = withContext(Dispatchers.IO) {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return@withContext Result.failure(Exception("User ID is null"))

            val randomAvatar = AVATAR_URLS.random()

            val userMap = hashMapOf(
                "name" to name,
                "email" to email,
                "balance" to 10000.0,
                "currency" to "USD",
                "timezone" to "UTC",
                "profilePictureUrl" to randomAvatar
            )

            db.collection("users").document(userId).set(userMap).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private val AVATAR_URLS = listOf(
            "https://api.dicebear.com/7.x/avataaars/png?seed=Felix",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Aneka",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Boo",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Jasper",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Lucky",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Luna",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Max",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Milo",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Oliver",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Jack",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Toby",
            "https://api.dicebear.com/7.x/avataaars/png?seed=Bella"
        )
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}
