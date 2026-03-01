package com.example.marketwatch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.marketwatch.data.PostsRepository

class FeedViewModel(private val repository: PostsRepository) : ViewModel() {
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    // Business logic for the feed screen will go here.
}
