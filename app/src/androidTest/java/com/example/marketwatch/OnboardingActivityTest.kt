package com.example.marketwatch

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(OnboardingActivity::class.java)

    @Test
    fun onboarding_nextButton_changesTextOnLastPage() {
        // First page
        onView(withId(R.id.btnOnboardingAction)).check(matches(withText(R.string.btn_next)))
        onView(withId(R.id.btnOnboardingAction)).perform(click())

        // Second page
        onView(withId(R.id.btnOnboardingAction)).check(matches(withText(R.string.btn_next)))
        onView(withId(R.id.btnOnboardingAction)).perform(click())

        // Third (last) page
        onView(withId(R.id.btnOnboardingAction)).check(matches(withText(R.string.btn_get_started)))
    }

    @Test
    fun onboarding_skipButton_isDisplayed() {
        onView(withId(R.id.btnSkip)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSkip)).check(matches(withText(R.string.btn_skip)))
    }
}
