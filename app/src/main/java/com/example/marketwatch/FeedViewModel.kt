package com.example.marketwatch

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.data.PostsRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class FeedViewModel(private val repository: PostsRepository) : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            repository.getRemotePostsFlow()
                .onStart { _isLoading.value = true }
                .catch { e ->
                    _isLoading.value = false
                    _errorMessage.value = e.message ?: "An error occurred"
                }
                .collect { posts ->
                    _isLoading.value = false
                    _posts.value = posts
                }
        }
    }

    fun createPost(content: String, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                repository.createPost(content, imageUri)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create post"
            }
        }
    }

    fun updatePost(postId: String, content: String, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                repository.updatePost(postId, content, imageUri)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update post"
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                repository.deletePost(postId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete post"
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            try {
                repository.toggleLike(post)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to like post"
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
