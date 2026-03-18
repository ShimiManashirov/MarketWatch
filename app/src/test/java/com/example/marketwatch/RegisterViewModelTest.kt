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
class RegisterViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var repository: AuthRepository

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var viewModel: RegisterViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = RegisterViewModel(repository)
    }

    @Test
    fun `register with empty fields sets error`() = runTest {
        viewModel.register("", "", "", "")
        assertEquals("Please fill all fields", viewModel.errorMessage.value)
    }

    @Test
    fun `register with mismatching passwords sets error`() = runTest {
        viewModel.register("Name", "test@test.com", "pass1", "pass2")
        assertEquals("Passwords do not match", viewModel.errorMessage.value)
    }

    @Test
    fun `register success updates registrationResult`() = runTest {
        val name = "John"
        val email = "john@example.com"
        val pass = "123456"
        `when`(repository.signUp(name, email, pass)).thenReturn(Result.success(firebaseUser))

        viewModel.register(name, email, pass, pass)
        advanceUntilIdle()

        assertEquals(Result.success(firebaseUser), viewModel.registrationResult.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `register failure updates error message`() = runTest {
        val name = "John"
        val email = "john@example.com"
        val pass = "123456"
        val exception = Exception("Email already exists")
        `when`(repository.signUp(name, email, pass)).thenReturn(Result.failure(exception))

        viewModel.register(name, email, pass, pass)
        advanceUntilIdle()

        assertEquals("Email already exists", viewModel.errorMessage.value)
    }
}
