package com.example.marketwatch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.data.PortfolioRepository
import com.example.marketwatch.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PortfolioRepository
    
    private val _portfolioItems = MutableLiveData<List<PortfolioItem>>()
    val portfolioItems: LiveData<List<PortfolioItem>> = _portfolioItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PortfolioRepository(db)
        loadPortfolio()
    }

    private fun loadPortfolio() {
        _isLoading.value = true
        viewModelScope.launch {
            repository.getPortfolioUpdates().collect { items ->
                _portfolioItems.value = items
                _isLoading.value = false
                
                // סנכרון מקומי ברקע
                launch(Dispatchers.IO) {
                    repository.syncLocalDatabase(items)
                }
            }
        }
    }
}
