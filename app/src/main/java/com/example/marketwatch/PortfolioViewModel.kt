package com.example.marketwatch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.data.PortfolioRepository
import com.example.marketwatch.data.local.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PortfolioRepository
    
    private val _portfolioItems = MutableLiveData<List<PortfolioItem>>()
    val portfolioItems: LiveData<List<PortfolioItem>> = _portfolioItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _userBalance = MutableLiveData<Double>(0.0)
    val userBalance: LiveData<Double> = _userBalance

    private val _exchangeRate = MutableLiveData<Double>(3.7)
    val exchangeRate: LiveData<Double> = _exchangeRate

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PortfolioRepository(db)
        loadPortfolio()
        observeUserBalance()
        fetchExchangeRate()
    }

    private fun loadPortfolio() {
        _isLoading.value = true
        viewModelScope.launch {
            repository.getPortfolioUpdates().collect { items ->
                _portfolioItems.value = items
                _isLoading.value = false
                
                launch(Dispatchers.IO) {
                    repository.syncLocalDatabase(items)
                }
            }
        }
    }

    private fun observeUserBalance() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _userBalance.value = snapshot.getDouble("balance") ?: 0.0
                }
            }
    }

    private fun fetchExchangeRate() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = FrankfurterApiClient.apiService.getLatestRates("USD").execute()
                if (response.isSuccessful) {
                    _exchangeRate.postValue(response.body()?.rates?.get("ILS") ?: 3.7)
                }
            } catch (e: Exception) {}
        }
    }
}
