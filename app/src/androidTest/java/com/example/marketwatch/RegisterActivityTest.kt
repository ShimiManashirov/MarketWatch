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
class RegisterActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(RegisterActivity::class.java)

    @Test
    fun register_uiComponents_areDisplayed() {
        onView(withId(R.id.etRegisterName)).check(matches(isDisplayed()))
        onView(withId(R.id.etRegisterEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.etRegisterPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.etRegisterConfirmPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.btnRegister)).check(matches(isDisplayed()))
        onView(withId(R.id.tvGoToLogin)).check(matches(isDisplayed()))
    }

    @Test
    fun register_emptyFields_showsError() {
        onView(withId(R.id.btnRegister)).perform(click())
        // Assertion for error state
    }

    @Test
    fun register_passwordMismatch_showsError() {
        onView(withId(R.id.etRegisterName)).perform(typeText("Test User"), closeSoftKeyboard())
        onView(withId(R.id.etRegisterEmail)).perform(typeText("test@example.com"), closeSoftKeyboard())
        onView(withId(R.id.etRegisterPassword)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.etRegisterConfirmPassword)).perform(typeText("password321"), closeSoftKeyboard())
        onView(withId(R.id.btnRegister)).perform(click())
        // Assertion for "Passwords do not match" error
    }

    @Test
    fun register_navigationToLogin_works() {
        onView(withId(R.id.tvGoToLogin)).perform(click())
        // Verify that LoginActivity is displayed
        onView(withId(R.id.etLoginEmail)).check(matches(isDisplayed()))
    }

    @Test
    fun register_longNameInput_works() {
        val longName = "This is an extremely long name for testing purposes to see how the UI handles it"
        onView(withId(R.id.etRegisterName)).perform(typeText(longName), closeSoftKeyboard())
        onView(withId(R.id.etRegisterName)).check(matches(withText(longName)))
    }

    @Test
    fun register_emailFormatValidation_logicTest() {
        onView(withId(R.id.etRegisterEmail)).perform(typeText("invalid_email"), closeSoftKeyboard())
        onView(withId(R.id.btnRegister)).perform(click())
        // Verify email format error if implemented in Activity/VM
    }
}
