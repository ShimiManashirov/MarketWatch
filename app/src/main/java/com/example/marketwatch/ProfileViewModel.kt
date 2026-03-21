package com.example.marketwatch

import android.content.Context
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

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    fun updateName(newName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateName(newName)
                _successMessage.value = "Name updated successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update name"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Uploads profile picture to Firebase Storage
     */
    fun uploadProfilePicture(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // 1. Compress image
                val imageBytes = ImageManager.uriToCompressedBytes(context, uri)
                
                if (imageBytes != null) {
                    // 2. Upload to Firebase Storage
                    repository.uploadProfilePicture(imageBytes)
                    _successMessage.value = "Profile picture updated successfully!"
                } else {
                    _errorMessage.value = "Failed to process image"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfilePictureUrl(url: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateProfilePictureUrl(url)
                _successMessage.value = "Profile picture updated"
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
                _successMessage.value = "Currency updated to $currencyCode"
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
                _successMessage.value = "Timezone updated"
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
                _successMessage.value = "Wallet data reset successfully"
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
                _successMessage.value = "Account deleted"
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
                _successMessage.value = "Password updated successfully"
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

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
