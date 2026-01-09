package com.example.marketwatch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        val selectedFragment: Fragment = when (item.itemId) {
            R.id.navigation_feed -> FeedFragment()
            R.id.navigation_search -> SearchFragment()
            R.id.navigation_wallet -> WalletFragment()
            R.id.navigation_portfolio -> PortfolioFragment()
            R.id.navigation_profile -> ProfileFragment()
            else -> return@OnNavigationItemSelectedListener false
        }
        
        loadFragment(selectedFragment)
        true
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
            loadFragment(FeedFragment(), false)
        }
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
                .addOnFailureListener { 
                    updateToolbarUsername("User")
                }
        }

        val logoutButton: Button = findViewById(R.id.toolbar_logout_button)
        val darkModeSwitch: SwitchMaterial = findViewById(R.id.toolbar_dark_mode_switch)

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

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadFragment(fragment: Fragment, animate: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
        
        if (animate) {
            transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
        
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }
}
