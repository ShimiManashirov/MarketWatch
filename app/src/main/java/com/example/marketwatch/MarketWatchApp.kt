package com.example.marketwatch

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class MarketWatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val sharedPrefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("darkMode", false)
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
