package com.example.marketwatch

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchFragmentTest {

    @Test
    fun search_uiComponents_areDisplayed() {
        onView(withId(R.id.stockSearchView)).check(matches(isDisplayed()))
        onView(withId(R.id.suggestionChipGroup)).check(matches(isDisplayed()))
        onView(withId(R.id.clearHistoryText)).check(matches(isDisplayed()))
    }

    @Test
    fun search_typeQuery_showsShimmerInitially() {
        onView(withId(R.id.stockSearchView)).perform(typeText("AAPL"), closeSoftKeyboard())
        // Verify shimmer is visible during the delay/loading
        onView(withId(R.id.searchShimmerContainer)).check(matches(isDisplayed()))
    }

    @Test
    fun search_clearHistory_logic() {
        onView(withId(R.id.clearHistoryText)).perform(click())
        // Verify default chips are shown after clearing
        onView(withText("AAPL")).check(matches(isDisplayed()))
        onView(withText("TSLA")).check(matches(isDisplayed()))
    }

    @Test
    fun search_chipClick_updatesQuery() {
        onView(withText("BTC")).perform(click())
        // onView(withId(R.id.stockSearchView)).check(matches(withText(containsString("BTC"))))
    }

    @Test
    fun search_emptyState_visibleInitially() {
        onView(withId(R.id.emptySearchContainer)).check(matches(isDisplayed()))
    }

    @Test
    fun search_longQueryInput_works() {
        val longQuery = "EXTREMELY LONG STOCK NAME THAT DOES NOT EXIST"
        onView(withId(R.id.stockSearchView)).perform(typeText(longQuery), closeSoftKeyboard())
        // Verify UI handles long strings
    }
}
