package com.example.marketwatch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.marketwatch.data.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify as kVerify

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var repository: UserRepository

    private lateinit var viewModel: ProfileViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Mock default user profile
        val mockUser = User("u1", "test@test.com", "John Doe", null, "USD", "UTC")
        `when`(repository.getUserProfile()).thenReturn(flowOf(mockUser))
        
        viewModel = ProfileViewModel(repository)
        // Start collecting LiveData from asLiveData() by registering an observer
        viewModel.userProfile.observeForever { }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `userProfile LiveData updates on init`() = runTest {
        val mockUser = User("u1", "test@test.com", "John Doe", null, "USD", "UTC")
        advanceUntilIdle()
        assertEquals(mockUser, viewModel.userProfile.value)
    }

    @Test
    fun `updateName success updates message`() = runTest {
        val newName = "Jane Doe"
        viewModel.updateName(newName)
        advanceUntilIdle()
        kVerify(repository).updateName(newName)
        assertEquals("Name updated successfully", viewModel.successMessage.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `updateName failure sets error`() = runTest {
        val newName = "Jane Doe"
        `when`(repository.updateName(newName)).thenThrow(RuntimeException("Error"))
        viewModel.updateName(newName)
        advanceUntilIdle()
        assertEquals("Failed to update name", viewModel.errorMessage.value)
    }

    @Test
    fun `updateProfilePictureUrl success updates message`() = runTest {
        val url = "https://example.com/pic.jpg"
        viewModel.updateProfilePictureUrl(url)
        advanceUntilIdle()
        kVerify(repository).updateProfilePictureUrl(url)
        assertEquals("Profile picture updated", viewModel.successMessage.value)
    }

    @Test
    fun `updateProfilePictureUrl failure sets error`() = runTest {
        val url = "invalid"
        `when`(repository.updateProfilePictureUrl(url)).thenThrow(RuntimeException())
        viewModel.updateProfilePictureUrl(url)
        advanceUntilIdle()
        assertEquals("Failed to update profile picture", viewModel.errorMessage.value)
    }

    @Test
    fun `updateCurrency success updates message`() = runTest {
        val currency = "ILS"
        viewModel.updateCurrency(currency)
        advanceUntilIdle()
        kVerify(repository).updateCurrency(currency)
        assertEquals("Currency updated to ILS", viewModel.successMessage.value)
    }

    @Test
    fun `updateCurrency failure sets error`() = runTest {
        `when`(repository.updateCurrency(anyString())).thenThrow(RuntimeException())
        viewModel.updateCurrency("EUR")
        advanceUntilIdle()
        assertEquals("Failed to update currency", viewModel.errorMessage.value)
    }

    @Test
    fun `updateTimezone success updates message`() = runTest {
        val tz = "Asia/Jerusalem"
        viewModel.updateTimezone(tz)
        advanceUntilIdle()
        kVerify(repository).updateTimezone(tz)
        assertEquals("Timezone updated", viewModel.successMessage.value)
    }

    @Test
    fun `updateTimezone failure sets error`() = runTest {
        `when`(repository.updateTimezone(anyString())).thenThrow(RuntimeException())
        viewModel.updateTimezone("GMT")
        advanceUntilIdle()
        assertEquals("Failed to update timezone", viewModel.errorMessage.value)
    }

    @Test
    fun `resetWalletData success updates message`() = runTest {
        viewModel.resetWalletData()
        advanceUntilIdle()
        kVerify(repository).resetWalletData()
        assertEquals("Wallet data reset successfully", viewModel.successMessage.value)
    }

    @Test
    fun `resetWalletData failure sets error`() = runTest {
        `when`(repository.resetWalletData()).thenThrow(RuntimeException())
        viewModel.resetWalletData()
        advanceUntilIdle()
        assertEquals("Failed to reset wallet data", viewModel.errorMessage.value)
    }

    @Test
    fun `deleteAccount success updates message`() = runTest {
        val pass = "password"
        viewModel.deleteAccount(pass)
        advanceUntilIdle()
        kVerify(repository).deleteAccount(pass)
        assertEquals("Account deleted", viewModel.successMessage.value)
    }

    @Test
    fun `deleteAccount failure sets error`() = runTest {
        val pass = "wrong"
        `when`(repository.deleteAccount(pass)).thenThrow(RuntimeException())
        viewModel.deleteAccount(pass)
        advanceUntilIdle()
        assertEquals("Failed to delete account. Check password.", viewModel.errorMessage.value)
    }

    @Test
    fun `updatePassword success updates message`() = runTest {
        val old = "old"
        val new = "new"
        viewModel.updatePassword(old, new)
        advanceUntilIdle()
        kVerify(repository).updatePassword(old, new)
        assertEquals("Password updated successfully", viewModel.successMessage.value)
    }

    @Test
    fun `updatePassword failure sets error`() = runTest {
        `when`(repository.updatePassword(anyString(), anyString())).thenThrow(RuntimeException())
        viewModel.updatePassword("wrong", "new")
        advanceUntilIdle()
        assertEquals("Failed to update password. Check current password.", viewModel.errorMessage.value)
    }

    @Test
    fun `clearError resets errorMessage`() {
        viewModel.updateName("Error Trigger") // This is mocked to error if we want
        viewModel.clearError()
        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun `clearSuccessMessage resets successMessage`() {
        viewModel.clearSuccessMessage()
        assertEquals(null, viewModel.successMessage.value)
    }
}
