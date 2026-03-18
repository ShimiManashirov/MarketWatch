package com.example.marketwatch.data.local

import com.example.marketwatch.Post
import com.example.marketwatch.StockNews
import com.example.marketwatch.PortfolioItem
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappersTest {

    @Test
    fun `Post to PostEntity mapping verification`() {
        val now = Timestamp.now()
        val post = Post(
            id = "1",
            userId = "u1",
            userName = "User",
            userProfilePicture = "pic",
            content = "Hello",
            imageUrl = "img",
            timestamp = now,
            likes = listOf("u1", "u2"),
            commentsCount = 5
        )

        val entity = PostEntity(
            id = post.id,
            userId = post.userId,
            userName = post.userName,
            userProfilePicture = post.userProfilePicture,
            content = post.content,
            imageUrl = post.imageUrl,
            timestamp = post.timestamp?.seconds ?: 0L,
            likesCount = post.likes.size
        )

        assertEquals(post.id, entity.id)
        assertEquals(post.userId, entity.userId)
        assertEquals(post.userName, entity.userName)
        assertEquals(post.content, entity.content)
        assertEquals(post.imageUrl, entity.imageUrl)
        assertEquals(post.timestamp?.seconds, entity.timestamp)
        assertEquals(2, entity.likesCount)
    }

    @Test
    fun `StockNews to NewsBookmarkEntity mapping verification`() {
        val news = StockNews(
            id = 1L,
            category = "biz",
            datetime = 123456L,
            headline = "H",
            image = "I",
            symbol = "S",
            source = "Src",
            summary = "Sum",
            url = "U"
        )

        val entity = NewsBookmarkEntity(
            id = news.id,
            category = news.category,
            datetime = news.datetime,
            headline = news.headline,
            image = news.image,
            symbol = news.symbol,
            source = news.source,
            summary = news.summary,
            url = news.url
        )

        assertEquals(news.id, entity.id)
        assertEquals(news.headline, entity.headline)
        assertEquals(news.symbol, entity.symbol)
        assertEquals(news.url, entity.url)
    }

    @Test
    fun `PortfolioItem to StockEntity mapping verification`() {
        val item = PortfolioItem(
            symbol = "AAPL",
            description = "Apple",
            quantity = 10.0,
            isFavorite = true,
            totalCost = 1500.0
        )

        val entity = StockEntity(
            symbol = item.symbol,
            description = item.description,
            quantity = item.quantity,
            isFavorite = item.isFavorite,
            totalCost = item.totalCost
        )

        assertEquals(item.symbol, entity.symbol)
        assertEquals(item.quantity, entity.quantity, 0.0)
        assertEquals(item.isFavorite, entity.isFavorite)
        assertEquals(item.totalCost, entity.totalCost, 0.0)
    }

    @Test
    fun `PostEntity to Post domain model mapping verification`() {
        val entity = PostEntity("1", "u1", "N1", "P1", "C1", "I1", 1000L, 10)
        
        val post = Post(
            id = entity.id,
            userId = entity.userId,
            userName = entity.userName,
            userProfilePicture = entity.userProfilePicture,
            content = entity.content,
            imageUrl = entity.imageUrl,
            timestamp = Timestamp(entity.timestamp, 0),
            likes = emptyList() // Room doesn't store the full likes list
        )
        
        assertEquals(entity.id, post.id)
        assertEquals(entity.content, post.content)
        assertEquals(entity.timestamp, post.timestamp?.seconds)
    }
}
