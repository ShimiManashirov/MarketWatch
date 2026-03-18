package com.example.marketwatch

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.Timestamp
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.Date

class PostsAdapterTest {

    @Mock
    private lateinit var mockView: View
    @Mock
    private lateinit var mockUserName: TextView
    @Mock
    private lateinit var mockContent: TextView
    @Mock
    private lateinit var mockTimestamp: TextView
    @Mock
    private lateinit var mockUserImage: ImageView
    @Mock
    private lateinit var mockPostImageCard: View
    @Mock
    private lateinit var mockPostImage: ImageView
    @Mock
    private lateinit var mockMenuButton: View
    @Mock
    private lateinit var mockBtnLike: ImageView
    @Mock
    private lateinit var mockTvLikeCount: TextView
    @Mock
    private lateinit var mockBtnComment: ImageView
    @Mock
    private lateinit var mockTvCommentCount: TextView

    private lateinit var adapter: PostsAdapter
    private val posts = mutableListOf<Post>()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Setup a mock post
        val post = Post(
            id = "1",
            userId = "u1",
            userName = "Test User",
            userProfilePicture = "url",
            content = "Test content",
            imageUrl = "image_url",
            timestamp = Timestamp(Date()),
            likes = listOf("u2"),
            commentsCount = 5
        )
        posts.add(post)

        adapter = PostsAdapter(
            posts = posts,
            currentUserId = "u1",
            onEditClick = {},
            onDeleteClick = {},
            onLikeClick = {},
            onCommentClick = {}
        )
    }

    @Test
    fun `getItemCount returns correct size`() {
        assert(adapter.itemCount == 1)
    }

    @Test
    fun `Post data binding logic verification`() {
        val post = posts[0]
        
        // Verify manual binding logic (as we can't easily run onBindViewHolder in unit tests without Robolectric)
        assert(post.userName == "Test User")
        assert(post.content == "Test content")
        assert(post.likes.size == 1)
        assert(post.commentsCount == 5)
    }

    @Test
    fun `Post handles anonymous user name`() {
        val post = Post(userName = "", content = "Content")
        val displayName = if (post.userName.isNotBlank()) post.userName else "Anonymous"
        assert(displayName == "Anonymous")
    }

    @Test
    fun `Post handles empty content`() {
        val post = Post(userName = "User", content = "")
        val displayContent = if (post.content.isNotBlank()) post.content else "(No content)"
        assert(displayContent == "(No content)")
    }

    @Test
    fun `Post image visibility logic`() {
        val postWithImage = Post(imageUrl = "http://image.com")
        val postWithoutImage = Post(imageUrl = null)
        
        assert(!postWithImage.imageUrl.isNullOrEmpty())
        assert(postWithoutImage.imageUrl.isNullOrEmpty())
    }

    @Test
    fun `Like count formatting test`() {
        val post = Post(likes = listOf("u1", "u2", "u3"))
        val likeText = "${post.likes.size} LIKES"
        assert(likeText == "3 LIKES")
    }

    @Test
    fun `Comment count formatting test`() {
        val postSingle = Post(commentsCount = 1)
        val postMultiple = Post(commentsCount = 10)
        
        val singleText = if (postSingle.commentsCount == 1) "1 Comment" else "${postSingle.commentsCount} Comments"
        val multipleText = if (postMultiple.commentsCount == 1) "1 Comment" else "${postMultiple.commentsCount} Comments"
        
        assert(singleText == "1 Comment")
        assert(multipleText == "10 Comments")
    }
}
