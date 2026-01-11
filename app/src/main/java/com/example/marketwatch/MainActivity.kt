package com.example.marketwatch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupNavigation()
        setupToolbar()
        setupPriceAlertWorker()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        
        // This connects the BottomNavigationView with the NavController
        // IDs in bottom_nav_menu.xml must match IDs in nav_graph.xml
        navView.setupWithNavController(navController)
    }

    private fun setupPriceAlertWorker() {
        val alertRequest = PeriodicWorkRequestBuilder<PriceAlertWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PriceAlerts",
            ExistingPeriodicWorkPolicy.KEEP,
            alertRequest
        )
    }

    fun updateToolbarUsername(newName: String) {
        val usernameTextView: TextView = findViewById(R.id.toolbar_username)
        val formattedName = newName.trim().lowercase(Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        usernameTextView.text = "Hello, $formattedName"
    }

    private fun setupToolbar() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        updateToolbarUsername(document.getString("name") ?: "User")
                    } else {
                        updateToolbarUsername("User")
                    }
                }
        }

        val logoutButton: Button = findViewById(R.id.toolbar_logout_button)
        val darkModeSwitch: SwitchMaterial = findViewById(R.id.toolbar_dark_mode_switch)

        val sharedPrefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        darkModeSwitch.isChecked = sharedPrefs.getBoolean("darkMode", false)

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            sharedPrefs.edit().putBoolean("darkMode", isChecked).apply()
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
