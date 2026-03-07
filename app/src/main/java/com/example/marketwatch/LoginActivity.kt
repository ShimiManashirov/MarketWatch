package com.example.marketwatch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.marketwatch.data.AuthRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        val repository = AuthRepository()
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(repository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory).get(LoginViewModel::class.java)

        val emailEditText: TextInputEditText = findViewById(R.id.emailEditText)
        val passwordEditText: TextInputEditText = findViewById(R.id.passwordEditText)
        val loginButton: Button = findViewById(R.id.loginButton)
        val registerButton: Button = findViewById(R.id.registerButton)
        val progressBar: ProgressBar? = findViewById(R.id.loginProgressBar) // Check if exists in XML

        setupObservers(progressBar)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            viewModel.login(email, password)
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupObservers(progressBar: ProgressBar?) {
        viewModel.loginResult.observe(this) { result ->
            if (result.isSuccess) {
                Toast.makeText(this, "Login successful.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
            findViewById<Button>(R.id.loginButton).isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
