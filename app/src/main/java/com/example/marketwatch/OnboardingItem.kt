package com.example.marketwatch

/**
 * Data model for an onboarding screen item.
 *
 * @property title The main title displayed on the screen.
 * @property description The descriptive text explaining the feature.
 * @property imageRes The resource ID of the illustration/icon to display.
 */
data class OnboardingItem(
    val title: String,
    val description: String,
    val imageRes: Int
)
