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

        verify(repository).updateName(newName)
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
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `updatePassword success updates message`() = runTest {
        val old = "old"
        val new = "new"
        
        viewModel.updatePassword(old, new)
        advanceUntilIdle()

        verify(repository).updatePassword(old, new)
        assertEquals("Password updated successfully", viewModel.successMessage.value)
    }

    @Test
    fun `resetWalletData calls repository`() = runTest {
        viewModel.resetWalletData()
        advanceUntilIdle()

        verify(repository).resetWalletData()
        assertEquals("Wallet data reset successfully", viewModel.successMessage.value)
    }

    @Test
    fun `updateCurrency updates message`() = runTest {
        val currency = "ILS"
        viewModel.updateCurrency(currency)
        advanceUntilIdle()

        verify(repository).updateCurrency(currency)
        assertEquals("Currency updated to ILS", viewModel.successMessage.value)
    }

    @Test
    fun `clearError and clearSuccessMessage reset state`() = runTest {
        // Trigger messages manually (since we can't easily mock private setters for simple tests)
        viewModel.updateName("Test")
        advanceUntilIdle()
        
        viewModel.clearSuccessMessage()
        assertEquals(null, viewModel.successMessage.value)
        
        viewModel.clearError()
        assertEquals(null, viewModel.errorMessage.value)
    }
}
