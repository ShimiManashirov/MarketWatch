package com.example.marketwatch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.marketwatch.data.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var repository: AuthRepository

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var viewModel: LoginViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(repository)
    }

    @Test
    fun `login with empty fields sets error message`() = runTest {
        // Act
        viewModel.login("", "")

        // Assert
        assertEquals("Please fill all fields", viewModel.errorMessage.value)
    }

    @Test
    fun `login success updates loginResult`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password"
        `when`(repository.signIn(email, password)).thenReturn(Result.success(firebaseUser))

        // Act
        viewModel.login(email, password)
        advanceUntilIdle()

        // Assert
        assertEquals(Result.success(firebaseUser), viewModel.loginResult.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `login failure updates error message`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "wrong"
        val exception = Exception("Invalid credentials")
        `when`(repository.signIn(email, password)).thenReturn(Result.failure(exception))

        // Act
        viewModel.login(email, password)
        advanceUntilIdle()

        // Assert
        assertEquals("Invalid credentials", viewModel.errorMessage.value)
        assertEquals(false, viewModel.isLoading.value)
    }
}
