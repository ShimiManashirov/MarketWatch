package com.example.marketwatch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_feed -> {
                loadFragment(FeedFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_search -> {
                loadFragment(SearchFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_portfolio -> {
                loadFragment(PortfolioFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_profile -> {
                loadFragment(ProfileFragment())
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupToolbar()

        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        if (savedInstanceState == null) {
            loadFragment(FeedFragment())
        }
    }

    fun updateToolbarUsername(newName: String) {
        val usernameTextView: TextView = findViewById(R.id.toolbar_username)
        usernameTextView.text = newName
    }

    private fun setupToolbar() {
        val usernameTextView: TextView = findViewById(R.id.toolbar_username)
        val logoutButton: Button = findViewById(R.id.toolbar_logout_button)
        val darkModeSwitch: SwitchMaterial = findViewById(R.id.toolbar_dark_mode_switch)

        // Fetch and display username
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        updateToolbarUsername(document.getString("name") ?: "User")
                    } else {
                        updateToolbarUsername("User")
                    }
                }
                .addOnFailureListener { 
                    updateToolbarUsername("User")
                    Toast.makeText(this, "Failed to load username", Toast.LENGTH_SHORT).show()
                }
        }

        // Dark Mode Switch Logic
        val sharedPrefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        darkModeSwitch.isChecked = sharedPrefs.getBoolean("darkMode", false)

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            sharedPrefs.edit().putBoolean("darkMode", isChecked).apply()
        }

        // Logout Button Logic
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
