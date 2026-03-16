package com.example.marketwatch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.data.NewsRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the News feature.
 * Manages fetching news articles and handling bookmarking logic.
 */
class NewsViewModel(private val repository: NewsRepository) : ViewModel() {

    private val _newsList = MutableLiveData<List<StockNews>>()
    val newsList: LiveData<List<StockNews>> = _newsList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Retrieves all bookmarked news articles from the local database.
     */
    val bookmarkedNews: LiveData<List<StockNews>> = repository.getAllBookmarks().asLiveData()

    /**
     * Fetches news for a specific stock symbol.
     */
    fun fetchNewsForSymbol(symbol: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = repository.getStockNews(symbol)
                _newsList.value = results
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load news: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Toggles the bookmark status of a news article.
     */
    fun toggleBookmark(news: StockNews, isBookmarked: Boolean) {
        viewModelScope.launch {
            if (isBookmarked) {
                repository.removeBookmark(news.id)
            } else {
                repository.addBookmark(news)
            }
        }
    }

    /**
     * Checks if a specific news article is bookmarked.
     */
    fun isBookmarked(newsId: Long): LiveData<Boolean> {
        return repository.isBookmarked(newsId).asLiveData()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
