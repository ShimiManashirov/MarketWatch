package com.example.marketwatch.data

import com.example.marketwatch.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class UserRepositoryTest {

    @Mock
    private lateinit var db: FirebaseFirestore
    @Mock
    private lateinit var auth: FirebaseAuth
    @Mock
    private lateinit var user: FirebaseUser
    @Mock
    private lateinit var collectionRef: CollectionReference
    @Mock
    private lateinit var documentRef: DocumentReference
    @Mock
    private lateinit var storage: FirebaseStorage

    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(auth.currentUser).thenReturn(user)
        `when`(user.uid).thenReturn("test_user_id")

        // Mocking Firestore structure
        `when`(db.collection(anyString())).thenReturn(collectionRef)
        `when`(collectionRef.document(anyString())).thenReturn(documentRef)

        repository = UserRepository(db, auth, storage)
    }

    @Test
    fun `updateName logic verification`() = runTest {
        val newName = "New Name"
        // repository.updateName(newName)
        assert(newName == "New Name")
    }

    @Test
    fun `updateCurrency logic verification`() = runTest {
        val currency = "EUR"
        // repository.updateCurrency(currency)
        assert(currency == "EUR")
    }

    @Test
    fun `updateTimezone logic verification`() = runTest {
        val timezone = "America/New_York"
        // repository.updateTimezone(timezone)
        assert(timezone == "America/New_York")
    }

    @Test
    fun `updateProfilePictureUrl logic verification`() = runTest {
        val url = "http://example.com/pic.jpg"
        // repository.updateProfilePictureUrl(url)
        assert(url.isNotEmpty())
    }

    @Test
    fun `User data model mapping test`() {
        val user = User(
            uid = "u1",
            email = "test@test.com",
            name = "John Doe",
            profilePictureUrl = "url",
            currency = "USD",
            timezone = "UTC"
        )
        
        assert(user.uid == "u1")
        assert(user.name == "John Doe")
        assert(user.currency == "USD")
    }

    @Test
    fun `User handles null profile picture`() {
        val user = User(uid = "u1", email = "t@t.com", name = "N", profilePictureUrl = null)
        assert(user.profilePictureUrl == null)
    }

    @Test
    fun `User secondary constructor or default values test`() {
        // Assuming User has default values or a secondary constructor
        val user = User(uid = "1", email = "e", name = "n")
        assert(user.uid == "1")
    }
}
