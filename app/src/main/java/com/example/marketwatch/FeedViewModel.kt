package com.example.marketwatch

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.data.PostsRepository
import kotlinx.coroutines.launch

class FeedViewModel(private val repository: PostsRepository) : ViewModel() {

    val posts: LiveData<List<Post>> = repository.getRemotePostsFlow().asLiveData()

    fun createPost(content: String, imageUri: Uri?) {
        viewModelScope.launch {
            repository.createPost(content, imageUri)
        }
    }

    fun updatePost(postId: String, content: String, imageUri: Uri?) {
        viewModelScope.launch {
            repository.updatePost(postId, content, imageUri)
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            repository.toggleLike(post)
        }
    }
}
