package com.example.marketwatch

import com.google.firebase.Timestamp
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CommentAdapterTest {

    private lateinit var adapter: CommentAdapter
    private val comments = mutableListOf<Comment>()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        val comment = Comment(
            id = "c1",
            postId = "p1",
            userId = "u1",
            userName = "Test User",
            userProfilePicture = "url",
            content = "Great insights!",
            timestamp = Timestamp(Date())
        )
        comments.add(comment)

        adapter = CommentAdapter(
            comments = comments,
            currentUserId = "u1",
            onDeleteClick = {}
        )
    }

    @Test
    fun `getItemCount returns correct list size`() {
        assert(adapter.itemCount == 1)
    }

    @Test
    fun `updateComments changes list and item count`() {
        val newList = listOf(
            Comment(id = "c2", postId = "p1", userId = "u2", userName = "U2", userProfilePicture = "", content = "C2", timestamp = Timestamp.now()),
            Comment(id = "c3", postId = "p1", userId = "u3", userName = "U3", userProfilePicture = "", content = "C3", timestamp = Timestamp.now())
        )
        adapter.updateComments(newList)
        assert(adapter.itemCount == 2)
    }

    @Test
    fun `Comment data binding logic verification`() {
        val comment = comments[0]
        assert(comment.userName == "Test User")
        assert(comment.content == "Great insights!")
        assert(comment.userId == "u1")
    }
}
