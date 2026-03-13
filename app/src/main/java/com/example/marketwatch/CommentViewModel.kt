package com.example.marketwatch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.data.CommentRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing comments on a specific post.
 */
class CommentViewModel(private val repository: CommentRepository) : ViewModel() {

    private val _postId = MutableLiveData<String>()
    
    // SwitchMap-like behavior: whenever the postId changes, fetch new comments
    val comments: LiveData<List<Comment>> = _postId.switchMap { id ->
        repository.getCommentsForPost(id).asLiveData()
    }

    private val _isSending = MutableLiveData<Boolean>()
    val isSending: LiveData<Boolean> = _isSending

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * Sets the active post ID to load comments for.
     */
    fun setPostId(postId: String) {
        _postId.value = postId
    }

    /**
     * Adds a new comment to the current post.
     */
    fun addComment(content: String) {
        val postId = _postId.value ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            _isSending.value = true
            try {
                repository.addComment(postId, content)
            } catch (e: Exception) {
                _error.value = "Failed to post comment: ${e.message}"
            } finally {
                _isSending.value = false
            }
        }
    }

    /**
     * Deletes a comment.
     */
    fun deleteComment(comment: Comment) {
        viewModelScope.launch {
            try {
                repository.deleteComment(comment.postId, comment.id)
            } catch (e: Exception) {
                _error.value = "Failed to delete comment"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
