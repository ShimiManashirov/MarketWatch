package com.example.marketwatch

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class DataModelsTest {

    @Test
    fun `Post model properties verification`() {
        val now = Timestamp.now()
        val post = Post(
            id = "p1",
            userId = "u1",
            userName = "User 1",
            userProfilePicture = "pic_url",
            content = "Hello World",
            imageUrl = "image_url",
            timestamp = now,
            likes = listOf("u2", "u3"),
            commentsCount = 10
        )
        
        assertEquals("p1", post.id)
        assertEquals("u1", post.userId)
        assertEquals("User 1", post.userName)
        assertEquals("pic_url", post.userProfilePicture)
        assertEquals("Hello World", post.content)
        assertEquals("image_url", post.imageUrl)
        assertEquals(now, post.timestamp)
        assertEquals(2, post.likes.size)
        assertEquals(10, post.commentsCount)
    }

    @Test
    fun `User model properties verification`() {
        val user = User(
            uid = "u1",
            email = "test@example.com",
            name = "John Doe",
            profilePictureUrl = "profile_url",
            currency = "ILS",
            timezone = "Asia/Jerusalem"
        )
        
        assertEquals("u1", user.uid)
        assertEquals("test@example.com", user.email)
        assertEquals("John Doe", user.name)
        assertEquals("profile_url", user.profilePictureUrl)
        assertEquals("ILS", user.currency)
        assertEquals("Asia/Jerusalem", user.timezone)
    }

    @Test
    fun `Comment model properties verification`() {
        val now = Timestamp.now()
        val comment = Comment(
            id = "c1",
            postId = "p1",
            userId = "u1",
            userName = "User 1",
            userProfilePicture = "url",
            content = "Nice post!",
            timestamp = now
        )
        
        assertEquals("c1", comment.id)
        assertEquals("p1", comment.postId)
        assertEquals("u1", comment.userId)
        assertEquals("User 1", comment.userName)
        assertEquals("url", comment.userProfilePicture)
        assertEquals("Nice post!", comment.content)
        assertEquals(now, comment.timestamp)
    }

    @Test
    fun `StockQuote properties verification`() {
        val quote = StockQuote(
            currentPrice = 150.5,
            change = 2.5,
            percentChange = 1.6,
            highPrice = 155.0,
            lowPrice = 148.0,
            openPrice = 149.0,
            previousClose = 148.0
        )
        
        assertEquals(150.5, quote.currentPrice, 0.0)
        assertEquals(2.5, quote.change, 0.0)
        assertEquals(1.6, quote.percentChange, 0.0)
        assertEquals(155.0, quote.highPrice, 0.0)
        assertEquals(148.0, quote.lowPrice, 0.0)
        assertEquals(149.0, quote.openPrice, 0.0)
        assertEquals(148.0, quote.previousClose, 0.0)
    }

    @Test
    fun `OnboardingItem properties verification`() {
        val item = OnboardingItem(
            title = "Welcome",
            description = "Start your journey",
            imageRes = 1234
        )
        
        assertEquals("Welcome", item.title)
        assertEquals("Start your journey", item.description)
        assertEquals(1234, item.imageRes)
    }

    @Test
    fun `PortfolioItem properties verification`() {
        val item = PortfolioItem(
            symbol = "AAPL",
            description = "Apple Inc.",
            quantity = 5.0,
            isFavorite = true,
            totalCost = 750.0
        )
        
        assertEquals("AAPL", item.symbol)
        assertEquals("Apple Inc.", item.description)
        assertEquals(5.0, item.quantity, 0.0)
        assertEquals(true, item.isFavorite)
        assertEquals(750.0, item.totalCost, 0.0)
    }
}
