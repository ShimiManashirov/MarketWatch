package com.example.marketwatch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Activity that handles the initial onboarding experience for new users.
 * Showcases the app's key features using a ViewPager2.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnAction: MaterialButton
    private lateinit var btnSkip: MaterialButton
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen before calling super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        initViews()
        setupViewPager()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.onboardingViewPager)
        btnAction = findViewById(R.id.btnOnboardingAction)
        btnSkip = findViewById(R.id.btnSkip)
        tabLayout = findViewById(R.id.tabLayoutIndicator)

        btnSkip.setOnClickListener { finishOnboarding() }
        
        btnAction.setOnClickListener {
            if (viewPager.currentItem < 2) {
                viewPager.currentItem += 1
            } else {
                finishOnboarding()
            }
        }
    }

    private fun setupViewPager() {
        val items = listOf(
            OnboardingItem(
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_desc_1),
                R.mipmap.ic_launcher
            ),
            OnboardingItem(
                getString(R.string.onboarding_title_2),
                getString(R.string.onboarding_desc_2),
                R.mipmap.ic_launcher
            ),
            OnboardingItem(
                getString(R.string.onboarding_title_3),
                getString(R.string.onboarding_desc_3),
                R.mipmap.ic_launcher
            )
        )

        viewPager.adapter = OnboardingAdapter(items)
        
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 2) {
                    btnAction.text = getString(R.string.btn_get_started)
                } else {
                    btnAction.text = getString(R.string.btn_next)
                }
            }
        })
    }

    private fun finishOnboarding() {
        val sharedPrefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
