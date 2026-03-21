package com.example.marketwatch

import android.content.Context
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

    private val _postCreated = MutableLiveData<Boolean>()
    val postCreated: LiveData<Boolean> = _postCreated

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

    /**
     * Creates a post and uploads image to Firebase Storage if provided
     */
    fun createPost(context: Context, content: String, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                var imageBytes: ByteArray? = null
                if (imageUri != null) {
                    imageBytes = ImageManager.uriToCompressedBytes(context, imageUri)
                }
                
                repository.createPost(content, imageBytes)
                _postCreated.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create post: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates an existing post
     */
    fun updatePost(context: Context, postId: String, content: String, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                var imageBytes: ByteArray? = null
                var existingImageUrl: String? = null

                if (imageUri != null) {
                    if (imageUri.toString().startsWith("http")) {
                        existingImageUrl = imageUri.toString()
                    } else {
                        imageBytes = ImageManager.uriToCompressedBytes(context, imageUri)
                    }
                }

                repository.updatePost(postId, content, imageBytes, existingImageUrl)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update post"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deletePost(postId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete post"
            } finally {
                _isLoading.value = false
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
    
    fun resetPostCreated() {
        _postCreated.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
