package com.example.marketwatch

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.marketwatch.data.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: UserRepository) : ViewModel() {

    val userProfile: LiveData<User?> = repository.getUserProfile().asLiveData()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _operationSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> = _operationSuccess

    fun updateName(newName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateName(newName)
                _operationSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update name"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfilePicture(uri: Uri) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateProfilePicture(uri)
                _operationSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update profile picture"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateCurrency(currencyCode: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateCurrency(currencyCode)
                _operationSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update currency"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTimezone(timezoneId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateTimezone(timezoneId)
                _operationSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update timezone"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetWalletData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.resetWalletData()
                _operationSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to reset wallet data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAccount(password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteAccount(password)
                _operationSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete account. Check password."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePassword(currentPwd: String, newPwd: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updatePassword(currentPwd, newPwd)
                _operationSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update password. Check current password."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun resetOperationSuccess() {
        _operationSuccess.value = false
    }
}
