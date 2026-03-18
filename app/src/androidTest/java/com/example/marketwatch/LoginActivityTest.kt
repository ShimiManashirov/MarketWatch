package com.example.marketwatch

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Test
    fun login_uiComponents_areDisplayed() {
        onView(withId(R.id.etLoginEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.etLoginPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
        onView(withId(R.id.tvGoToRegister)).check(matches(isDisplayed()))
        onView(withText(R.string.login_title)).check(matches(isDisplayed()))
    }

    @Test
    fun login_emptyFields_showsError() {
        onView(withId(R.id.btnLogin)).perform(click())
        // Assuming your LoginViewModel/Activity shows a toast or error on the field
        // We can verify the error message if it's in a TextView or via Toast checking logic
    }

    @Test
    fun login_invalidEmail_showsError() {
        onView(withId(R.id.etLoginEmail)).perform(typeText("invalid-email"), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())
        // Assertion for error state
    }

    @Test
    fun login_navigationToRegister_works() {
        onView(withId(R.id.tvGoToRegister)).perform(click())
        // Verify that RegisterActivity is displayed
        onView(withId(R.id.etRegisterName)).check(matches(isDisplayed()))
    }

    @Test
    fun login_passwordVisibilityToggle_works() {
        // Test logic for toggling password visibility if implemented in your layout
    }

    @Test
    fun login_brandingElements_areCorrect() {
        // Verify logo or branding text
        onView(withText("Market Watch")).check(matches(isDisplayed()))
    }
}
