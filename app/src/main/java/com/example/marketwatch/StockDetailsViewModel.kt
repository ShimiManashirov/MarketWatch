package com.example.marketwatch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.data.StockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StockDetailsViewModel : ViewModel() {
    private val repository = StockRepository()

    private val _stockStatus = MutableLiveData<PortfolioItem?>()
    val stockStatus: LiveData<PortfolioItem?> = _stockStatus

    private val _quote = MutableLiveData<StockQuote?>()
    val quote: LiveData<StockQuote?> = _quote

    private val _companyProfile = MutableLiveData<CompanyProfile?>()
    val companyProfile: LiveData<CompanyProfile?> = _companyProfile

    private val _news = MutableLiveData<List<StockNews>>()
    val news: LiveData<List<StockNews>> = _news

    private val _tradeStatus = MutableLiveData<String>()
    val tradeStatus: LiveData<String> = _tradeStatus

    fun observeStockStatus(symbol: String) {
        viewModelScope.launch {
            repository.getStockStatus(symbol).collect { status ->
                _stockStatus.value = status
            }
        }
    }

    fun fetchData(symbol: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val quoteResponse = repository.getQuote(symbol)
                if (quoteResponse.isSuccessful) _quote.postValue(quoteResponse.body())

                val profileResponse = repository.getCompanyProfile(symbol)
                if (profileResponse.isSuccessful) _companyProfile.postValue(profileResponse.body())

                val newsResponse = repository.getNews(symbol)
                if (newsResponse.isSuccessful) _news.postValue(newsResponse.body())
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    fun toggleFavorite(symbol: String, description: String) {
        val current = _stockStatus.value
        viewModelScope.launch {
            repository.toggleFavorite(
                symbol, 
                description, 
                current?.isFavorite ?: false, 
                current?.quantity ?: 0.0
            )
        }
    }

    fun executeTrade(symbol: String, description: String, quantity: Double, price: Double, isBuy: Boolean) {
        viewModelScope.launch {
            try {
                repository.executeTrade(
                    symbol, description, quantity, price, isBuy, 
                    _stockStatus.value?.isFavorite ?: false
                )
                _tradeStatus.value = "SUCCESS"
            } catch (e: Exception) {
                _tradeStatus.value = e.message ?: "Trade failed"
            }
        }
    }
}
