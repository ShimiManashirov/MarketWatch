package com.example.marketwatch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.marketwatch.data.AuthRepository
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val repository = AuthRepository()
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return RegisterViewModel(repository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory).get(RegisterViewModel::class.java)

        val usernameEditText: TextInputEditText = findViewById(R.id.usernameEditText)
        val emailEditText: TextInputEditText = findViewById(R.id.emailEditText)
        val passwordEditText: TextInputEditText = findViewById(R.id.passwordEditText)
        val confirmPasswordEditText: TextInputEditText = findViewById(R.id.confirmPasswordEditText)
        val registerButton: Button = findViewById(R.id.registerButton)
        val loginLinkButton: Button = findViewById(R.id.loginLinkButton)
        val progressBar: ProgressBar = findViewById(R.id.registerProgressBar)

        setupObservers(progressBar)

        registerButton.setOnClickListener {
            val name = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            
            viewModel.register(name, email, password, confirmPassword)
        }

        loginLinkButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupObservers(progressBar: ProgressBar) {
        viewModel.registrationResult.observe(this) { result ->
            if (result.isSuccess) {
                Toast.makeText(this, "Registration successful.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            findViewById<Button>(R.id.registerButton).isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }
}
